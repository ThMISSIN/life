# -*- coding: utf-8 -*-
"""组合根（Composition Root）：把 core 层的 Protocol 端口接到 infrastructure 实现。

本模块一次性接完此前链路分析（docs/模块链路分析.md §7）识别出的断点：
1. chat → AgentOrchestrator 装配（RouterGateway / TracerGateway / ToolsGateway 适配）
2. MemoryManager 门面：bridge react 期待的 get_relevant/append_turn ↔ STM/LTM 实际接口
3. 知识入库：ETL → embedding → Milvus(id+embedding+text) → BM25 内存索引注册
4. 长期记忆：agent_memory 集合（pk+embedding+content+session_id+meta）

装配策略是"尽可能富安装"：Redis/Milvus/嵌入模型/LLM Key 任何一项缺失都不会导致
启动失败，能力逐项降级（无 LLM 则拒绝对话；无 Redis 则无记忆；无 Milvus 则无知识库），
status() 暴露能力矩阵供 Java 侧探活。
"""

from __future__ import annotations

import uuid
from typing import Any, Optional

from loguru import logger

from app.config import Settings, get_settings
from app.core.agent.orchestrator import AgentOrchestrator, AgentResponse, IntentContext
from app.core.intent.recognizer import IntentRecognizer
from app.core.memory.long_term import LongTermMemory
from app.core.memory.short_term import ShortTermMemory
from app.core.tools.base import BaseTool


class _LLMGateway:
    """把 infra ModelRouter.chat 适配为 core 期待的对象。

    同时满足两个协议面：
    - core 编排器的 llm：acomplete(messages, **kwargs) -> str
    - 短期记忆压缩器的：ainvoke(prompt) -> 带 .content 对象
    """

    def __init__(self, router: Any) -> None:
        self._router = router

    async def acomplete(self, messages: list[dict[str, str]], **kwargs: Any) -> str:
        resp = await self._router.chat(messages, **kwargs)
        return resp.content or ""

    async def ainvoke(self, input: Any, **kwargs: Any) -> Any:  # noqa: A003
        if isinstance(input, str):
            input = [{"role": "user", "content": input}]
        return await self._router.chat(list(input), **kwargs)


class RouterGateway:
    """infra.ModelRouter → orchestrator.ModelRouter Protocol（get_llm 按场景返回网关）。

    当前单模型部署，各 purpose 返回同一网关；多模型时在此按 purpose→配置 映射。
    """

    def __init__(self, router: Any) -> None:
        self._gateway = _LLMGateway(router)

    def get_llm(self, purpose: str) -> _LLMGateway:  # noqa: ARG002
        return self._gateway


class TracerGateway:
    """infra.Tracer（start_trace/start_child_span）→ orchestrator.Tracer Protocol。"""

    def __init__(self, tracer: Any) -> None:
        self._tracer = tracer

    def new_trace_id(self) -> str:
        return str(uuid.uuid4())

    def start_span(self, name: str, trace_id: str, attributes: Optional[dict] = None) -> Any:
        if attributes:
            logger.debug("span={} attrs={}", name, attributes)
        return self._tracer.start_child_span(trace_id, name)

    def end_span(self, span: Any, error: Optional[BaseException] = None) -> None:
        self._tracer.end_span(span, error=str(error) if error else None)

    def log_event(self, trace_id: str, name: str, payload: dict) -> None:
        span = self._tracer.start_child_span(trace_id, name)
        self._tracer.end_span(span, result=payload)


class ToolsGateway:
    """core ToolRegistry（描述式）→ orchestrator ToolRegistry Protocol（调用式）。"""

    def __init__(self, registry: Any) -> None:
        self._registry = registry

    def list_tool_names(self) -> list[str]:
        return [t.name for t in self._registry.get_all_tools()]

    async def invoke(self, name: str, arguments: dict[str, Any]) -> str:
        tool = self._registry.get_tool(name)
        result = await tool.execute(**(arguments or {}))
        if isinstance(result, str):
            return result
        import json

        try:
            return json.dumps(result, ensure_ascii=False, default=str)
        except Exception:  # noqa: BLE001
            return str(result)


class AgentMemory:
    """记忆门面适配：get_relevant/append_turn（core Protocol）→ STM + LTM。

    读降级、写强一致的容错等级保持与 MemoryManager 一致：
    get_relevant 任何一路失败都跳过该路；append_turn 向上抛由调用方记日志。
    """

    def __init__(
        self,
        stm: Optional[ShortTermMemory],
        ltm: Optional[LongTermMemory],
    ) -> None:
        self._stm = stm
        self._ltm = ltm

    async def get_relevant(self, session_id: str, query: str, limit: int = 8) -> list[str]:
        out: list[str] = []
        if self._stm is not None:
            try:
                recent = await self._stm.get_history(session_id)
                out.extend(f"[近期] {m.role.value}: {m.content[:200]}" for m in recent[-limit:])
            except Exception as e:  # noqa: BLE001
                logger.warning("短期记忆读取失败，跳过: {}", e)
        if self._ltm is not None:
            try:
                items = await self._ltm.recall(query, session_id, top_k=3)
                out.extend(f"[长期] {i.content[:300]}" for i in items)
            except Exception as e:  # noqa: BLE001
                logger.warning("长期记忆召回失败，跳过: {}", e)
        return out[: limit * 2]

    async def append_turn(
        self,
        session_id: str,
        role: str,
        content: str,
        metadata: Optional[dict[str, Any]] = None,
    ) -> None:
        from app.models.enums import MessageRole
        from app.models.schemas import Message

        if self._stm is not None:
            try:
                msg_role = MessageRole(role) if role in ("user", "assistant", "system", "tool") else MessageRole.USER
            except ValueError:
                msg_role = MessageRole.USER
            await self._stm.add_message(
                session_id,
                Message(role=msg_role, content=content, metadata=metadata or {}),
            )
        # 沉淀规则：用户陈述进长期记忆（语义记忆），助手答案只留短期窗口
        if self._ltm is not None and role == "user" and len(content) >= 8:
            try:
                await self._ltm.store(session_id, content, metadata or {})
            except Exception as e:  # noqa: BLE001
                logger.warning("长期记忆写入失败（不影响对话）: {}", e)


class KnowledgeSearchTool(BaseTool):
    """把 RAG 三段中的"检索+重排"注册为 Agent 可自主调用的工具。

    service 引用在组合根装配阶段回填，未装配时执行返回明确提示（不抛错，
    Observation 保持可读文本——工具错误也是喂回模型的信号）。
    """

    def __init__(self) -> None:
        from app.core.tools.base import ToolParameter

        super().__init__()
        self.name = "knowledge_search"
        self.description = "在企业知识库中检索与问题相关的资料片段（混合检索+精排）。"
        self.parameters = [
            ToolParameter(name="query", type="string", description="检索问句或关键词", required=True),
            ToolParameter(name="top_k", type="integer", description="返回片段数，默认5", required=False),
        ]

    _service: Optional["AgentService"] = None

    @property
    def _enabled(self) -> bool:
        return self._service is not None and self._service.knowledge_enabled

    async def execute(self, **kwargs: Any) -> str:
        if not self._enabled:
            return "知识库当前不可用（未配置向量库或嵌入模型），请改用其他信息源回答。"
        query = str(kwargs.get("query", "")).strip()
        if not query:
            raise ValueError("参数 query 不能为空")
        top_k = int(kwargs.get("top_k") or 5)
        results = await self._service.search(query, top_k=top_k)  # type: ignore[union-attr]
        if not results:
            return "知识库中未检索到相关内容。"
        return "\n\n".join(
            f"[{i + 1}] (score={r['score']:.4f}) {r['content']}" for i, r in enumerate(results)
        )


class AgentService:
    """Agent 能力服务：Java 主服务的直接对端。生命周期由 FastAPI lifespan 管理。"""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self.orchestrator: Optional[AgentOrchestrator] = None
        self.tracer: Any = None
        self.stm: Optional[ShortTermMemory] = None
        self.ltm: Optional[LongTermMemory] = None
        self.embedder: Any = None
        self.milvus: Any = None
        self.retriever: Any = None
        self.reranker: Any = None
        self.tools_registry: Any = None
        self.memory: Optional[AgentMemory] = None
        self._corpus: dict[str, str] = {}
        self._intent = IntentRecognizer()
        self._llm_router: Any = None

    # ------------------------------------------------------------------
    # 装配（分能力降级）
    # ------------------------------------------------------------------
    async def startup(self) -> None:
        await self._setup_llm()
        await self._setup_stm()
        await self._setup_milvus_stack()
        await self._setup_tools()
        await self._setup_orchestrator()
        logger.info(
            "AgentService 就绪: llm={} stm={} knowledge={} ltm={} tools={}",
            self._llm_router is not None,
            self.stm is not None,
            self.knowledge_enabled,
            self.ltm is not None,
            self.tools_registry is not None and len(self.tools_registry.get_all_tools()),
        )

    async def shutdown(self) -> None:
        if self._settings.redis_url:
            try:
                if self.stm is not None:
                    await self.stm._redis.aclose()  # noqa: SLF001
            except Exception as e:  # noqa: BLE001
                logger.warning("关闭 Redis 连接异常: {}", e)

    async def _setup_llm(self) -> None:
        s = self._settings
        if not s.openai_api_key:
            logger.warning("未配置 OPENAI_API_KEY：/agent/chat 将返回 503")
            return
        from app.infrastructure.llm.model_router import ModelConfig, ModelRouter

        cfg = ModelConfig(
            model_id=s.openai_model,
            api_key=s.openai_api_key,
            base_url=s.openai_api_base or None,
        )
        self._llm_router = ModelRouter([cfg])

    async def _setup_stm(self) -> None:
        s = self._settings
        try:
            import redis.asyncio as aioredis

            client = aioredis.from_url(s.redis_url, decode_responses=True)
            await client.ping()
            self.stm = ShortTermMemory(
                redis_client=client,
                llm=_LLMGateway(self._llm_router) if self._llm_router else None,
            )
            logger.info("短期记忆已启用 (Redis)")
        except Exception as e:  # noqa: BLE001
            logger.warning("Redis 不可用，短期记忆关闭: {}", e)
            self.stm = None

    async def _setup_milvus_stack(self) -> None:
        s = self._settings
        from app.infrastructure.embedding import create_embedder

        self.embedder = create_embedder(s.embedding_model)
        if self.embedder is None:
            logger.warning("嵌入模型不可用：知识库与长期记忆关闭")
            return

        from app.infrastructure.vectordb.milvus_client import MilvusManager

        mgr = MilvusManager(host=s.milvus_host, port=str(s.milvus_port))
        try:
            dim = self.embedder.dim or s.embedding_dim
            await mgr.create_collection(s.milvus_collection_name, dim, with_text=True)
            await mgr.create_memory_collection(s.memory_collection_name, dim)
            knowledge_coll = await mgr.get_collection(s.milvus_collection_name)
            memory_coll = await mgr.get_collection(s.memory_collection_name)
        except Exception as e:  # noqa: BLE001
            logger.warning("Milvus 不可用，知识库与长期记忆关闭: {}", e)
            return

        self.milvus = mgr
        from app.core.rag.retriever import MultiRetriever
        from app.core.rag.reranker import Reranker

        # 索引与检索统一用 L2（与建集合时的 IVF_FLAT/L2 一致）；
        # 嵌入向量已归一化，L2 排序与余弦等价
        self.retriever = MultiRetriever(knowledge_coll, self.embedder)
        self.reranker = Reranker()

        # 长期记忆（与 RAG 用同一个 embedder）
        self.ltm = LongTermMemory(memory_coll, self.embedder)

        # BM25 预热：从 Milvus 重建进程内关键词索引
        try:
            self._corpus = await mgr.fetch_all_texts(s.milvus_collection_name)
            self.retriever.register_keyword_documents(self._corpus)
            logger.info("BM25 预热完成，语料 {} 条", len(self._corpus))
        except Exception as e:  # noqa: BLE001
            logger.warning("BM25 预热失败（首次入库时会补建）: {}", e)

    async def _setup_tools(self) -> None:
        from app.core.tools.builtin.calculator import CalculatorTool
        from app.core.tools.builtin.search import WebSearchTool
        from app.core.tools.registry import ToolRegistry

        registry = ToolRegistry()
        registry.register(CalculatorTool())
        registry.register(WebSearchTool())

        tool = KnowledgeSearchTool()
        tool._service = self  # 回填服务引用
        registry.register(tool)

        # 平台业务工具（第一批只读）：配置了 Java 回连地址才注册
        if self._settings.java_base_url:
            from app.core.tools.builtin.life_api import build_life_api_tools

            life_tools = build_life_api_tools()
            for life_tool in life_tools:
                registry.register(life_tool)
            logger.info("平台业务工具已注册 {} 个", len(life_tools))

        self.tools_registry = registry

    async def _setup_orchestrator(self) -> None:
        if self._llm_router is None:
            return
        from app.infrastructure.trace.tracer import Tracer

        self.tracer = Tracer()
        self.memory = AgentMemory(self.stm, self.ltm)
        s = self._settings
        config = {
            "enable_reflection": s.agent_enable_reflection,
            "fallback_react_on_plan_failure": True,
            "react_max_steps": s.agent_react_max_steps,
            "max_replan_attempts": 2,
            "reflection_min_quality": 60,
        }
        self.orchestrator = AgentOrchestrator(
            config=config,  # type: ignore[arg-type]  dict 满足 .get 协议
            model_router=RouterGateway(self._llm_router),  # type: ignore[arg-type]
            memory_manager=self.memory,  # type: ignore[arg-type]
            tool_registry=ToolsGateway(self.tools_registry),  # type: ignore[arg-type]
            tracer=TracerGateway(self.tracer),  # type: ignore[arg-type]
        )

    # ------------------------------------------------------------------
    # 能力矩阵
    # ------------------------------------------------------------------
    @property
    def knowledge_enabled(self) -> bool:
        return self.retriever is not None

    def status(self) -> dict[str, Any]:
        return {
            "llm": self._llm_router is not None,
            "orchestrator": self.orchestrator is not None,
            "short_term_memory": self.stm is not None,
            "long_term_memory": self.ltm is not None,
            "knowledge_base": self.knowledge_enabled,
            "embedding": self.embedder is not None,
            "bm25_corpus_size": len(self._corpus),
            "tools": len(self.tools_registry.get_all_tools()) if self.tools_registry else 0,
        }

    # ------------------------------------------------------------------
    # 对话主入口（同步 JSON）
    # ------------------------------------------------------------------
    async def chat(
        self,
        question: str,
        session_id: str,
        mode: str = "react",
        force_answer: bool = False,
        on_event: Any = None,
        user_id: Optional[str] = None,
    ) -> dict[str, Any]:
        if self.orchestrator is None:
            raise RuntimeError("LLM 未配置，Agent 不可用")

        # 注入请求级身份上下文：业务工具经 ContextVar 取 user_id 回连 Java
        from app.core.context import RequestContext, reset_request_context, set_request_context

        token = set_request_context(RequestContext(user_id=user_id, session_id=session_id))
        try:
            intent = await self._intent.recognize(question)
            clarify = await self._intent.clarify(question, intent)
            if clarify and not force_answer:
                return {
                    "need_clarification": True,
                    "answer": clarify,
                    "intent": intent.intent,
                    "sub_intent": intent.sub_intent,
                    "confidence": intent.confidence,
                    "mode_used": None,
                    "success": True,
                    "degraded": False,
                    "trace_id": None,
                    "steps": [],
                    "reflection": None,
                }

            resp: AgentResponse = await self.orchestrator.run(
                user_input=question,
                session_id=session_id,
                mode=mode if mode in ("react", "plan_execute") else "react",
                intent=IntentContext(
                    intent=intent.intent,
                    confidence=intent.confidence,
                    slots=intent.slots,
                ),
                on_event=on_event,
            )
            return self._serialize(resp)
        finally:
            reset_request_context(token)

    def _serialize(self, resp: AgentResponse) -> dict[str, Any]:
        compact_steps = []
        for s in resp.steps[-20:]:
            item = {"phase": s.get("phase") or s.get("subtask_id") or "?"}
            if s.get("action"):
                item["action"] = s["action"]
            if s.get("observation"):
                item["observation"] = str(s["observation"])[:500]
            if s.get("error"):
                item["error"] = str(s["error"])[:300]
            compact_steps.append(item)
        reflection = None
        if resp.reflection is not None:
            r = resp.reflection
            reflection = {
                "quality_score": r.quality_score,
                "is_complete": r.is_complete,
                "likely_hallucination": r.likely_hallucination,
                "suggestions": r.suggestions[:3],
                "summary": r.summary,
            }
        return {
            "need_clarification": False,
            "answer": resp.answer,
            "intent": resp.intent.intent,
            "confidence": resp.intent.confidence,
            "mode_used": resp.mode_used,
            "success": resp.success,
            "degraded": resp.degraded,
            "trace_id": resp.trace_id,
            "error": resp.error,
            "steps": compact_steps,
            "reflection": reflection,
        }

    # ------------------------------------------------------------------
    # 知识库
    # ------------------------------------------------------------------
    async def ingest(self, filename: str, data: bytes, mime_type: str | None) -> dict[str, Any]:
        if not self.knowledge_enabled or self.milvus is None:
            raise RuntimeError("知识库未配置（需要 Milvus + 嵌入模型）")
        from app.etl.pipeline import ETLPipeline

        s = self._settings
        doc_id = str(uuid.uuid4())
        pipeline = ETLPipeline()

        async def on_chunks(chunks: list[str], _parsed: Any) -> None:
            if not chunks:
                return
            vectors = await self.embedder.aembed_texts(chunks)
            ids = [f"{doc_id}:{i}" for i in range(len(chunks))]
            await self.milvus.insert_chunks(s.milvus_collection_name, ids, vectors, chunks)
            self._corpus.update(dict(zip(ids, chunks)))
            self.retriever.register_keyword_documents(self._corpus)

        etl = await pipeline.run_bytes(data, filename, mime_type, on_chunks=on_chunks)
        logger.info("知识入库完成 file={} chunks={}", filename, len(etl.chunks))
        return {"document_id": doc_id, "filename": filename, "chunk_count": len(etl.chunks)}

    async def search(self, query: str, top_k: int = 5) -> list[dict[str, Any]]:
        if not self.knowledge_enabled:
            return []
        results = await self.retriever.retrieve(query, top_k=max(top_k * 2, 10), mode="hybrid")
        if self.reranker is not None and results:
            try:
                results = await self.reranker.rerank(query, results, top_k=top_k)
            except Exception as e:  # noqa: BLE001
                logger.warning("精排失败，使用融合原序: {}", e)
                results = results[:top_k]
        return [
            {
                "id": r.id,
                "snippet": r.content[:300],
                "content": r.content,
                "score": round(float(r.score), 6),
                "source": r.source,
            }
            for r in results[:top_k]
        ]


async def build_agent_service() -> AgentService:
    """lifespan 入口：构建并启动服务（任何子能力缺失只降级不抛错）。"""
    svc = AgentService(get_settings())
    await svc.startup()
    return svc
