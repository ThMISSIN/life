# -*- coding: utf-8 -*-
"""意图识别：树形分类、置信度与澄清引导。"""

from __future__ import annotations

import re
from typing import Any

from loguru import logger
from pydantic import BaseModel, Field


class IntentResult(BaseModel):
    """意图识别结果。"""

    intent: str = Field(description="主意图标签")
    confidence: float = Field(ge=0.0, le=1.0, description="置信度 0~1")
    slots: dict[str, Any] = Field(default_factory=dict, description="槽位信息")
    sub_intent: str | None = Field(default=None, description="子意图（树形第二层）")
    rationale: str = Field(default="", description="简要理由（可给模型或日志用）")


# 树形意图：根 -> 子意图（关键词规则，可替换为分类模型）
# 平台业务分支对齐 life-agent 已注册的 17 个只读工具，命中即高置信直过澄清闸门；
# 排序在"问答"之前：关键词平票时先遍历到的分支胜出，业务查询优先于知识问答归类
_INTENT_TREE: dict[str, dict[str, list[str]]] = {
    "平台业务": {
        "积分": ["积分", "余额", "流水", "排名", "名次", "榜", "本月获得", "本月消耗"],
        "签到": ["签到", "打卡", "连签", "签了几天", "签过"],
        "订单": ["订单", "物流", "发货", "待支付", "签收", "已完成", "退款", "取消", "快递", "单号", "收货"],
        "权益": ["优惠券", "券", "权益", "核销", "兑换", "过期", "有效期", "抵扣"],
        "商品": ["商品", "商城", "库存", "上架", "积分商城", "价格"],
        "社区": ["帖子", "动态", "发帖", "评论", "点赞", "关注", "粉丝", "社区", "讨论", "话题"],
        "购物车": ["购物车", "加购", "结算"],
        "通知": ["通知", "消息", "公告", "未读", "新消息"],
        "任务中心": ["任务", "赚积分", "做任务"],
        "地址": ["收货地址", "地址", "默认地址"],
    },
    "问答": {
        "知识问答": ["为什么", "如何", "解释", "含义", "规则", "政策", "什么是", "怎么规定", "怎么样", "怎样"],
        "闲聊": ["你好", "谢谢", "再见", "聊天", "在吗", "你是谁"],
    },
    "任务": {
        "搜索": ["搜索", "查一下", "帮我找", "search", "查找"],
        "计算": ["计算", "等于", "算一下", "加减乘除", "算式"],
    },
    "文档": {
        "上传": ["上传", "文档", "pdf"],
        "总结": ["总结", "摘要", "概括"],
    },
}


class IntentRecognizer:
    """意图识别器：树形分类 + 置信度 + 引导澄清。"""

    def __init__(self, confidence_threshold: float = 0.55) -> None:
        self._threshold = confidence_threshold

    @property
    def confidence_threshold(self) -> float:
        """置信度阈值：低于该值时建议澄清。"""
        return self._threshold

    def _score_branch(self, query: str) -> tuple[str, str | None, float, str]:
        """返回 (根意图, 子意图, 置信度, 理由)。"""
        q = query.strip().lower()
        best_root = "未知"
        best_child: str | None = None
        best_hits = 0
        total_kw = 0

        for root, children in _INTENT_TREE.items():
            for child, kws in children.items():
                hits = sum(1 for kw in kws if kw.lower() in q)
                if hits > best_hits:
                    best_hits = hits
                    best_root = root
                    best_child = child
                total_kw += len(kws)

        if best_hits == 0:
            # 无关键词命中：根据长度与问号弱推断
            conf = 0.35 if "?" in q or "？" in q else 0.25
            return "问答", "知识问答", conf, "未命中关键词，弱规则推断"

        conf = min(0.95, 0.45 + 0.12 * best_hits)
        rationale = f"关键词命中 {best_hits} 次，归类为 {best_root}/{best_child}"
        return best_root, best_child, conf, rationale

    async def recognize(self, query: str, context: dict | None = None) -> IntentResult:
        """识别用户意图；context 可包含历史轮次等（当前规则引擎未使用）。"""
        _ = context
        root, child, conf, why = self._score_branch(query)
        slots: dict[str, Any] = {}
        nums = re.findall(r"[-+]?\d*\.\d+|\d+", query)
        if nums:
            slots["numbers"] = nums[:5]

        result = IntentResult(
            intent=root,
            confidence=conf,
            slots=slots,
            sub_intent=child,
            rationale=why,
        )
        logger.info(
            "意图识别 query_preview={} -> {} / {} conf={}",
            query[:100],
            root,
            child,
            conf,
        )
        return result

    async def clarify(self, query: str, intent_result: IntentResult) -> str:
        """置信度不足时生成澄清提示语。"""
        if intent_result.confidence >= self._threshold:
            return ""

        return (
            "我不太确定您的具体需求。"
            f"您是想了解「{intent_result.intent}」相关（当前置信度 {intent_result.confidence:.2f}）吗？"
            "请补充场景、对象或期望的输出格式（例如：只要结论 / 需要步骤）。"
        )
