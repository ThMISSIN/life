# -*- coding: utf-8 -*-
"""内部服务间调用鉴权：Java 主服务是唯一受信调用方。

信任模型：终端用户 JWT 校验只发生在 Java 侧；Python 侧不解析 JWT，
仅校验共享内部令牌（X-Agent-Token），并从 Java 注入的 X-User-Id 取用户身份。
"""

from __future__ import annotations

from fastapi import Header, HTTPException

from app.config import get_settings


async def require_internal_token(
    x_agent_token: str | None = Header(default=None, alias="X-Agent-Token"),
) -> None:
    """校验内部令牌。配置为空表示本地开发模式（不校验，启动时应告警）。"""
    token = get_settings().agent_internal_token
    if not token:
        return
    if x_agent_token != token:
        raise HTTPException(status_code=401, detail="无效的内部令牌")


def resolve_user_id(x_user_id: str | None = Header(default=None, alias="X-User-Id")) -> str:
    """取 Java 侧解析 JWT 后传入的用户标识；缺省按匿名会话处理。"""
    return (x_user_id or "").strip() or "anonymous"
