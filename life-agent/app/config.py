"""应用配置：基于 pydantic-settings，支持环境变量与 .env 文件。"""

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = Field(default="enterprise-ai-agent", description="服务名称")
    app_env: str = Field(default="development", description="运行环境")
    debug: bool = Field(default=False, description="调试模式")
    api_prefix: str = Field(default="/api/v1", description="API 前缀")
    host: str = Field(default="0.0.0.0", description="监听地址")
    port: int = Field(default=8000, description="监听端口")

    openai_api_key: str = Field(default="", description="OpenAI API Key")
    openai_api_base: str = Field(
        default="https://api.openai.com/v1",
        description="OpenAI 兼容 API Base",
    )
    openai_model: str = Field(default="gpt-4o-mini", description="默认对话模型")

    database_url: str = Field(
        default="postgresql+asyncpg://postgres:postgres@localhost:5432/agent_db",
        description="SQLAlchemy 异步数据库 URL（推荐 postgresql+asyncpg）",
    )
    redis_url: str = Field(default="redis://localhost:6379/0", description="Redis URL")

    milvus_host: str = Field(default="localhost", description="Milvus 主机")
    milvus_port: int = Field(default=19530, description="Milvus 端口")
    milvus_user: str = Field(default="", description="Milvus 用户名")
    milvus_password: str = Field(default="", description="Milvus 密码")
    milvus_collection_name: str = Field(
        default="agent_knowledge",
        description="知识库向量集合名（id+embedding+text）",
    )
    memory_collection_name: str = Field(
        default="agent_memory",
        description="长期记忆向量集合名（pk+embedding+content+session_id+meta）",
    )

    # 嵌入模型（本地 sentence-transformers；未安装依赖时知识库/长期记忆自动降级关闭）
    embedding_model: str = Field(
        default="BAAI/bge-small-zh-v1.5",
        description="嵌入模型名或本地路径",
    )
    embedding_dim: int = Field(default=512, description="嵌入维度（建集合用）")

    # Agent 服务（Java 主服务为唯一受信调用方）
    agent_internal_token: str = Field(
        default="",
        description="内部令牌：非空时 /agent/* 必须携带匹配的 X-Agent-Token；空=不校验(仅限本地开发)",
    )
    agent_react_max_steps: int = Field(default=10, description="ReAct 最大步数")
    agent_enable_reflection: bool = Field(default=True, description="启用反思阶段")

    # Java 主服务（业务工具网关回连地址；空=不注册平台业务工具，Agent 仅保留知识/搜索/计算能力）
    java_base_url: str = Field(
        default="",
        description="life-social 服务地址，如 http://127.0.0.1:8080；Agent 经其 /ai-tools/* 查询业务数据",
    )

    log_level: str = Field(default="INFO", description="日志级别")


@lru_cache
def get_settings() -> Settings:
    return Settings()
