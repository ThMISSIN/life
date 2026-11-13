# -*- coding: utf-8 -*-
"""平台业务工具（第一批：只读）。

经 Java 工具网关 POST /ai-tools/{name} 回连 life-social，以当前用户身份
（ContextVar 注入的 X-User-Id）查询积分/商城/权益/订单/社区等实时业务数据。

设计要点：
- 声明式工具规格表 + 单一执行器，新增工具 = 加一条 spec + Java 侧注册 handler；
- 所有失败都转成可读文本 Observation（错误也是喂回模型的信号），不抛裸异常；
- 工具描述写清触发场景与边界（尤其 search_posts vs knowledge_search），
  描述含糊是 ReAct 选错工具的头号原因。
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Any, Optional

import httpx
from loguru import logger

from app.config import get_settings
from app.core.context import current_user_id
from app.core.tools.base import BaseTool, ToolParameter


@dataclass
class ToolSpec:
    """业务工具声明：名称/描述/参数/网关工具名。"""

    name: str
    gateway_name: str
    description: str
    parameters: list[ToolParameter] = field(default_factory=list)


def _p(name: str, type_: str = "string", desc: str = "", required: bool = False) -> ToolParameter:
    return ToolParameter(name=name, type=type_, description=desc, required=required)


_PAGE_PARAMS = [
    _p("page_num", "integer", "页码，默认1"),
    _p("page_size", "integer", "每页条数，默认5，最大10"),
]

TOOL_SPECS: list[ToolSpec] = [
    # ---- 积分域 ----
    ToolSpec(
        name="get_points_summary",
        gateway_name="get_points_summary",
        description="查询当前用户的积分总览：积分余额、本月获得、本月消耗。用户问『我有多少积分/这个月积分收支』时用，无需参数。",
    ),
    ToolSpec(
        name="get_points_records",
        gateway_name="get_points_records",
        description="分页查询当前用户的积分流水（每笔变动的原因与数额）。用户追问『积分为什么变少/最近积分怎么来的』时用。",
        parameters=_PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_points_rank",
        gateway_name="get_points_rank",
        description="查询本月积分榜：返回我的名次与榜首前三名。用户问『我排名多少/谁积分最多』时用，无需参数。",
    ),
    ToolSpec(
        name="get_points_tasks",
        gateway_name="get_points_tasks",
        description="分页查询当前可做的积分任务列表（任务名与可得积分）。用户问『怎么赚积分/有什么任务』时用。",
        parameters=_PAGE_PARAMS,
    ),
    # ---- 商城/权益域 ----
    ToolSpec(
        name="search_products",
        gateway_name="search_products",
        description="按关键词/分类搜索积分商城已上架商品。sort_type: 1综合 2销量 3价格升 4价格降。用户问『有什么可以兑换/XX多少钱』时用。",
        parameters=[_p("keyword", desc="商品名称关键词，可空"),
                    _p("sort_type", "integer", "排序方式1-4，默认1")] + _PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_product_detail",
        gateway_name="get_product_detail",
        description="按商品ID查询商品详情（积分价格、库存、描述）。通常配合 search_products 的结果使用。",
        parameters=[_p("product_id", "integer", "商品ID", True)],
    ),
    ToolSpec(
        name="search_benefits",
        gateway_name="search_benefits",
        description="搜索平台在售权益（优惠券/直充等）。benefit_type: 1券码 2直充。用户问『有什么优惠/券』时用。",
        parameters=[_p("keyword", desc="权益名称关键词，可空"),
                    _p("benefit_type", "integer", "1券码 2直充，可空")] + _PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_benefit_detail",
        gateway_name="get_benefit_detail",
        description="按权益ID查询权益详情（面额、适用条件、有效期）。",
        parameters=[_p("benefit_id", "integer", "权益ID", True)],
    ),
    ToolSpec(
        name="get_my_benefits",
        gateway_name="get_my_benefits",
        description="分页查询我的券包（已持有权益）。status: 0待生效 1未使用 2已使用 3已过期，不传查全部。用户问『我有什么券/券过期了吗』时用。",
        parameters=[_p("status", "integer", "状态筛选，可空")] + _PAGE_PARAMS,
    ),
    # ---- 订单/购物车域 ----
    ToolSpec(
        name="get_my_orders",
        gateway_name="get_my_orders",
        description="分页查询我的订单。status: 0待支付 1已支付 2已发货 3已完成 4已取消 5已退款。用户问『我的订单/物流到哪了』时用。",
        parameters=[_p("status", "integer", "订单状态筛选，可空")] + _PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_order_detail",
        gateway_name="get_order_detail",
        description="按订单ID查询订单详情（状态、金额、商品明细）。",
        parameters=[_p("order_id", "integer", "订单ID", True)],
    ),
    ToolSpec(
        name="get_cart",
        gateway_name="get_cart",
        description="查询当前用户购物车列表（含实时价格与失效标记）。用户问『购物车里有什么』时用，无需参数。",
    ),
    # ---- 社区域 ----
    ToolSpec(
        name="search_posts",
        gateway_name="search_posts",
        description="按关键词搜索社区用户帖子（UGC实时内容）。与 knowledge_search 区分：本工具搜用户动态，官方文档/规则用 knowledge_search。",
        parameters=[_p("keyword", desc="帖子标题/内容关键词", required=True)] + _PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_user_profile",
        gateway_name="get_user_profile",
        description="查询某用户的公开资料（昵称/等级/关注与粉丝数）。user_id 不传则查当前用户自己。",
        parameters=[_p("user_id", "integer", "目标用户ID，可空")],
    ),
    # ---- 通知/签到/地址域 ----
    ToolSpec(
        name="get_notifications",
        gateway_name="get_notifications",
        description="查询站内通知（含未读总数）。is_read: 0未读 1已读。用户问『有没有新消息/公告』时用。",
        parameters=[_p("is_read", "integer", "已读筛选，可空")] + _PAGE_PARAMS,
    ),
    ToolSpec(
        name="get_sign_calendar",
        gateway_name="get_sign_calendar",
        description="查询本月签到日历：已签日期列表、累计天数、今日是否已签。用户问『签了几天/今天还签吗』时用，无需参数。",
    ),
    ToolSpec(
        name="get_my_addresses",
        gateway_name="get_my_addresses",
        description="查询当前用户的收货地址列表。用户问『默认地址是哪个/我存了哪些地址』时用，无需参数。",
    ),
]


class LifeApiTool(BaseTool):
    """通用业务工具执行器：POST Java /ai-tools/{gateway_name}，透传 LLM 生成的参数。"""

    def __init__(self, spec: ToolSpec) -> None:
        super().__init__()
        self._spec = spec
        self.name = spec.name
        self.description = spec.description
        self.parameters = list(spec.parameters)

    async def execute(self, **kwargs: Any) -> str:
        settings = get_settings()
        if not settings.java_base_url:
            return "平台业务接口未配置（JAVA_BASE_URL 为空），无法查询该数据。"

        user_id = current_user_id()
        if not user_id or user_id == "anonymous":
            return "当前会话未绑定平台用户身份，无法查询个人数据；请引导用户先登录。"

        url = settings.java_base_url.rstrip("/") + "/ai-tools/" + self._spec.gateway_name
        headers = {
            "X-Agent-Token": settings.agent_internal_token,
            "X-User-Id": user_id,
        }
        # 只保留非空参数，避免 LLM 生成的 null 干扰服务端默认值
        args = {k: v for k, v in kwargs.items() if v is not None and v != ""}
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.post(url, json=args, headers=headers)
            if resp.status_code == 401:
                return "平台拒绝了工具调用（内部令牌或用户身份无效）。"
            if resp.status_code != 200:
                return f"平台接口异常 status={resp.status_code}"
            payload = resp.json()
        except Exception as e:  # noqa: BLE001
            logger.warning("业务工具 {} 调用失败: {}", self.name, e)
            return f"平台接口暂时不可用: {e!s}"

        if payload.get("code") != 1:
            return f"查询失败：{payload.get('msg') or '未知错误'}"
        data = payload.get("data")
        if data in (None, [], {}, 0):
            return "查询成功，但没有符合条件的数据（空结果）。"
        return json.dumps(data, ensure_ascii=False, default=str)


def build_life_api_tools() -> list[LifeApiTool]:
    """按声明表实例化全部业务工具。"""
    return [LifeApiTool(spec) for spec in TOOL_SPECS]
