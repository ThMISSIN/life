# -*- coding: utf-8 -*-
"""请求级上下文：用 ContextVar 贯穿一次 Agent 运行，避免污染各层函数签名。

典型用途：业务工具（LifeApiTool）执行时需要当前用户 ID 回连 Java，
但 core 层的 ToolRegistry.invoke(name, arguments) 协议不带用户上下文，
用 ContextVar 在路由入口 set、工具内 get，天然协程安全、互不串扰。
"""

from __future__ import annotations

import contextvars
from dataclasses import dataclass
from typing import Optional


@dataclass
class RequestContext:
    """一次请求的身份快照。"""

    user_id: Optional[str] = None
    session_id: str = "default"


_current: contextvars.ContextVar[Optional[RequestContext]] = contextvars.ContextVar(
    "request_context", default=None
)


def set_request_context(ctx: RequestContext) -> contextvars.Token:
    """设置当前上下文，返回 Token 供 reset（建议在 try/finally 使用）。"""
    return _current.set(ctx)


def reset_request_context(token: contextvars.Token) -> None:
    _current.reset(token)


def get_request_context() -> Optional[RequestContext]:
    return _current.get()


def current_user_id() -> Optional[str]:
    ctx = _current.get()
    return ctx.user_id if ctx else None
