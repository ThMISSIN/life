# -*- coding: utf-8 -*-
"""Agent API：供 Java 主服务调用的编排对话、流式对话与知识库接口。

与既有 /chat（直连 LLM 骨架演示）并存；/agent/* 走完整编排链
（意图→ReAct/Plan→记忆→工具→反思），全部接口受内部令牌保护。
"""

from __future__ import annotations

import asyncio
import json
from typing import Any, AsyncIterator

from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile
from fastapi.responses import StreamingResponse
from loguru import logger
from pydantic import BaseModel, Field

from app.api.deps import require_internal_token, resolve_user_id

router = APIRouter(tags=["agent"], dependencies=[Depends(require_internal_token)])


class AgentChatRequest(BaseModel):
    """编排对话请求。身份由 Java 侧经 Header 注入，body 只带业务参数。"""

    question: str = Field(min_length=1, description="用户问题")
    session_id: str | None = Field(default=None, description="会话 ID，缺省按用户隔离")
    mode: str = Field(default="react", description="react | plan_execute")
    force_answer: bool = Field(default=False, description="低置信度时跳过澄清直接作答")


def _service(request: Request) -> Any:
    svc = getattr(request.app.state, "agent", None)
    if svc is None:
        raise HTTPException(status_code=503, detail="Agent 服务未就绪")
    return svc


def _session_id(req: AgentChatRequest, user_id: str) -> str:
    """会话键：显式传入优先，否则用户级隔离（user:{id}）。"""
    sid = (req.session_id or "").strip()
    return sid or f"user:{user_id}"


def _sse(obj: dict[str, Any]) -> str:
    return "data: " + json.dumps(obj, ensure_ascii=False, default=str) + "\n\n"


@router.post("/agent/chat")
async def agent_chat(
    req: AgentChatRequest,
    request: Request,
    user_id: str = Depends(resolve_user_id),
) -> dict[str, Any]:
    """非流式编排对话：返回答案 + 意图/模式/轨迹/反思元信息。"""
    svc = _service(request)
    try:
        return await svc.chat(
            req.question,
            _session_id(req, user_id),
            mode=req.mode,
            force_answer=req.force_answer,
            user_id=user_id,
        )
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception as e:  # noqa: BLE001
        logger.exception("agent chat 失败: {}", e)
        raise HTTPException(status_code=500, detail=f"Agent 对话失败: {e!s}") from e


@router.post("/agent/chat/stream")
async def agent_chat_stream(
    req: AgentChatRequest,
    request: Request,
    user_id: str = Depends(resolve_user_id),
) -> StreamingResponse:
    """SSE 流式编排对话：step 事件（过程透明）+ answer 事件（最终结果）。

    实现：orchestrator 的 on_event 回调写入 asyncio.Queue，生成器边跑边取；
    任务异常通过 error 事件下发（流式场景错误必须进流）。
    """
    svc = _service(request)
    session_id = _session_id(req, user_id)

    async def gen() -> AsyncIterator[bytes]:
        queue: asyncio.Queue = asyncio.Queue()

        async def on_event(payload: dict[str, Any]) -> None:
            compact: dict[str, Any] = {"phase": payload.get("phase") or payload.get("subtask_id")}
            if payload.get("action"):
                compact["action"] = payload["action"]
            if payload.get("observation"):
                compact["observation"] = str(payload["observation"])[:400]
            if payload.get("status"):
                compact["status"] = payload["status"]
            if payload.get("error"):
                compact["error"] = str(payload["error"])[:200]
            await queue.put(compact)

        yield _sse({"type": "accepted", "session_id": session_id}).encode("utf-8")
        task = asyncio.create_task(
            svc.chat(
                req.question,
                session_id,
                mode=req.mode,
                force_answer=req.force_answer,
                on_event=on_event,
                user_id=user_id,
            )
        )
        try:
            while True:
                try:
                    event = await asyncio.wait_for(queue.get(), timeout=0.5)
                    yield _sse({"type": "step", "data": event}).encode("utf-8")
                    continue
                except asyncio.TimeoutError:
                    pass
                if task.done():
                    # 排空残留事件后结束
                    while not queue.empty():
                        yield _sse({"type": "step", "data": queue.get_nowait()}).encode("utf-8")
                    break
            result = task.result()
            yield _sse({"type": "answer", "data": result}).encode("utf-8")
        except Exception as e:  # noqa: BLE001
            logger.exception("agent stream 失败: {}", e)
            if not task.cancelled():
                yield _sse({"type": "error", "message": str(e)}).encode("utf-8")
        yield _sse({"type": "done"}).encode("utf-8")

    return StreamingResponse(
        gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no"},
    )


@router.post("/agent/knowledge/documents")
async def upload_knowledge_document(
    request: Request,
    file: UploadFile = File(..., description="知识文档（txt/pdf）"),
) -> dict[str, Any]:
    """知识入库：解析→分块→嵌入→Milvus + BM25 注册（RAG 语料的唯一入口）。"""
    svc = _service(request)
    if not svc.knowledge_enabled:
        raise HTTPException(status_code=503, detail="知识库未配置（需 Milvus 与嵌入模型）")
    data = await file.read()
    if not data:
        raise HTTPException(status_code=422, detail="空文件")
    try:
        return await svc.ingest(file.filename or "unnamed", data, file.content_type)
    except Exception as e:  # noqa: BLE001
        logger.exception("知识入库失败: {}", e)
        raise HTTPException(status_code=500, detail=f"知识入库失败: {e!s}") from e


@router.get("/agent/knowledge/search")
async def knowledge_search(
    request: Request,
    query: str,
    top_k: int = 5,
) -> dict[str, Any]:
    """知识库检索（调试/前端搜索组件可直接调用，绕过 Agent 循环）。"""
    svc = _service(request)
    if not svc.knowledge_enabled:
        raise HTTPException(status_code=503, detail="知识库未配置")
    return {"query": query, "results": await svc.search(query, top_k=max(1, min(top_k, 20)))}


@router.get("/agent/status")
async def agent_status(request: Request) -> dict[str, Any]:
    """能力矩阵探活：Java /ai/status 透传，前端可据此显隐知识库入口。"""
    return _service(request).status()
