# -*- coding: utf-8 -*-
"""本地嵌入模型：sentence-transformers 惰性加载，输出归一化向量。

归一化后 L2 距离与余弦排序等价，与 Milvus IVF_FLAT/L2 索引及
retriever/long_term 的 metric_param（L2）语义一致。
依赖缺失（未安装 sentence-transformers 或模型下载失败）时
``create_embedder`` 返回 None，由上层组合根决定降级策略。
"""

from __future__ import annotations

import asyncio
from typing import Protocol, runtime_checkable

from loguru import logger


class Embedder(Protocol):
    """嵌入接口约定（与 core 层 EmbeddingProtocol/LTMEmbedProtocol 兼容）。"""

    def embed_query(self, text: str) -> list[float]: ...

    def embed_texts(self, texts: list[str]) -> list[list[float]]: ...


class LocalSTEmbedder:
    """基于 sentence-transformers 的本地嵌入器。"""

    def __init__(self, model_name: str) -> None:
        from sentence_transformers import SentenceTransformer  # 惰性导入

        self._model = SentenceTransformer(model_name)
        self.dim = int(self._model.get_sentence_embedding_dimension() or 0)
        logger.info("嵌入模型已加载: {} (dim={})", model_name, self.dim)

    def embed_query(self, text: str) -> list[float]:
        return self.embed_texts([text])[0]

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        import numpy as np

        vecs = self._model.encode(
            list(texts),
            normalize_embeddings=True,
            convert_to_numpy=True,
            show_progress_bar=False,
        )
        return [np.asarray(v, dtype=float).tolist() for v in vecs]

    async def aembed_texts(self, texts: list[str]) -> list[list[float]]:
        """异步包装：编码是 CPU 密集同步操作，挪出事件循环。"""
        return await asyncio.to_thread(self.embed_texts, texts)


def create_embedder(model_name: str) -> LocalSTEmbedder | None:
    """尝试创建嵌入器；任何失败返回 None（上层降级）。"""
    try:
        return LocalSTEmbedder(model_name)
    except Exception as e:  # noqa: BLE001
        logger.warning("嵌入模型不可用（知识库/长期记忆将降级）: {}", e)
        return None
