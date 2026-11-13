# 生活权益社交平台（life）

融合 **仿 X（推特）信息流** 与 **积分权益商城** 的社交电商平台：用户在信息流中发帖互动、通过签到/任务赚取积分，积分可兑换权益券码或在商城购物。项目采用 **C 端服务 + RuoYi 管理端 + Vue3 前端 + Python AI Agent** 的多服务架构，重点解决积分超发、并发超兑、重复下单、热点写入、内容风控等业务难题。

## 系统架构

```
                    ┌──────────────────────┐
                    │    life-frontend     │  Vue3 + Element Plus (:5173)
                    └──────────┬───────────┘
          ┌────────────────────┼─────────────────────┐
          ▼                    ▼                     ▼
 ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐
 │   life-social   │  │   life-admin    │  │    life-agent    │
 │  C端 + 内置管理端 │  │  RuoYi 管理端    │  │  Python AI Agent │
 │ SpringBoot3 :8080│  │  SpringBoot :8090│  │   FastAPI :8000  │
 └───────┬─────────┘  └───────┬─────────┘  └────────▲─────────┘
         │  HTTP+内部令牌(工具回调 /ai-tools) ─────────┘
         │  Redis Pub/Sub(公告事件) / 共享 MySQL
         ▼
 ┌─────────────────────────────────────────────────────┐
 │ MySQL 8 │ Redis + Redisson │ RabbitMQ │ Elasticsearch│ OSS/本地存储 │
 └─────────────────────────────────────────────────────┘
```

| 子项目 | 技术 | 职责 |
|---|---|---|
| `life-java/life-social` | SpringBoot 3.4 / Java 21 | C 端全部业务 + 内置管理端接口（审核/敏感词/风控/退款/运维），三模块：common / pojo / server |
| `life-admin` | RuoYi-Vue | 运营管理端：权益、商品、订单、公告、通知模板、积分/余额流水 |
| `life-frontend` | Vue3 + Vite + Element Plus | C 端 Web（信息流 / 商城 / 购物车 / 订单 / 消息 / 个人中心，17 个页面） |
| `life-agent` | FastAPI + LangGraph + Milvus | AI 智能助手：ReAct/规划/反思编排、RAG 知识库、长短期记忆；Java 端只做鉴权与 SSE 流式桥接 |

## 核心功能

**社交信息流**：发帖 / Redis 草稿 / 转发 / 评论（游客可看、本人+帖主双权限删除）/ 点赞 / 关注与粉丝流 / 热度推荐流（时间衰减 + 互动加权 + 新帖扶持）/ ES 全文检索（不可用自动降级 SQL LIKE）

**积分体系**：bitmap 签到（连签阶梯奖励、落库归档）/ 任务中心（完成事件 + 领取幂等）/ 月度积分榜与自动结算发奖 / 积分过期回收 / **可用 + 冻结双余额账户**、流水状态机 + `biz_no` 唯一幂等键、`changePoints` 统一变更入口

**权益与商品交易**：权益兑换（券码发放/核销/作废退回）、商品双通道（现金/积分）、购物车（并发锁 + 失效置灰 + 随下单同事务清车）、唯一下单入口、余额支付（支付单+流水）、管理端退款、TTL 延迟关单 + 扫描兜底、超时自动确认收货

**通知触达**：站内收件箱（已读/未读）+ WebSocket 在线推送 + MQ 业务事件（兑换/支付/退款通知），管理端公告经 Redis Pub/Sub 跨服务 fan-out

**内容与风控**：敏感词 DFA 过滤 + 帖子人工审核流（管理端审核队列）/ 设备指纹 + Redis 滑动窗口风控（命中限流标记、自动解除、fail-open）/ Sentinel 接口限流 + 热点参数流控

**AI 助手**：`/ai/chat`（SSE 流式）、知识库文档上传与检索；Agent 通过内部令牌回调 `/ai-tools/**` 查询业务数据

## 技术亮点

1. **积分资产安全**：积分/余额/库存统一"条件原子 UPDATE"（`points>=n` 同行判定，0 行即整单回滚）；流水表 append-only + 状态机 + 唯一 `biz_no` 幂等；签到/发帖/任务奖励经 RabbitMQ 异步入账（消费端入账+流水同事务，at-least-once 靠幂等键去重）；冻结积分支撑"下单冻结→支付转扣→取消解冻"；定时任务回收过期积分。
2. **热度推荐流**：热度公式（基础分 + 互动权重 − 时间衰减）收敛为 SQL 表达式单语句原子重算，阅读按"同用户同帖每日一次"去重；**Caffeine(L1) + Redis(L2) 多级缓存**首页热点页，结构性变更即时失效、计数类靠短 TTL 收敛；布隆过滤器防缓存穿透；ES 经 RestClient 直连 REST API（无客户端版本绑定），双写失败告警 + 管理端一键 reindex。
3. **库存并发控制**：热点商品库存预热至 Redis，**Lua 脚本原子完成"存在性检查+余量校验+DECRBY"**（-1 未预热 / -2 不足 / ≥0 余量），DB 条件扣减保留为最终权威，Redis 只挡量不记账；对账定时任务以 DB 为准周期校正漂移。
4. **订单全生命周期**：状态机 0待支付→1已支付→2已发货→3已完成（/4取消/5退款）全闭环；**TTL+DLX 延迟消息关单**与定时扫描兜底幂等共存（条件更新，先到先执行）；购物车 `cartIds` 随 `submitOrder` 同事务清理——下单失败全保留、成功全清理。
5. **热点写优化**：点赞 Redis Set 去重 + ZSet 缓冲计数，回写任务排空后投 MQ 批量落库（终值覆盖幂等，可重入不漂移）；列表点赞状态 pipeline 批查。
6. **MQ 治理与全链路追踪**：业务队列统一挂死信（`life.dlx.exchange → life.dead.queue`），消费端手动 ack、业务异常 nack 进死信留痕，毒消息不阻塞主队列；`TraceIdFilter` 生成 traceId → MDC → MQ Header → 消费端/定时任务还原，日志全链路可串。
7. **多层防刷**：Sentinel 注解式接口限流 + 热点参数流控；前端设备指纹（`X-Device-Fingerprint`）+ Redis ZSet 滑动窗口计数，命中阈值即拦截并短期标记（教育型风控，自动解除，全程 fail-open 不阻断正常业务）。
8. **安全与多实例正确性**：C 端/管理端 JWT **独立密钥 + 独立请求头 + userType 声明三重隔离**，双端令牌互不通用；登出令牌进 Redis 黑名单；定时任务统一经 Redisson `tryLock`（看门狗续期）包装，多实例部署仅一实例执行。

## 技术栈

| 类别 | 技术 |
|---|---|
| 后端 | SpringBoot 3.4.1、Java 21、MyBatis-Plus 3.5.16、JWT、SpringDoc OpenAPI |
| 数据 | MySQL 8、Redis（Redisson 3.47）、Elasticsearch（可选，自动降级） |
| 消息 | RabbitMQ（direct / TTL+DLX 延迟消息 / 死信队列 / 手动 ack） |
| 缓存 | Caffeine（L1）+ Redis（L2）、布隆过滤器 |
| 稳定性 | Sentinel 1.8.8（限流/热点参数）、Redisson 分布式锁、全链路 traceId |
| 实时 | WebSocket、Redis Pub/Sub、SSE |
| 前端 | Vue 3.5、Vite 6、Element Plus、Axios |
| AI | FastAPI、LangChain/LangGraph、Milvus（RAG）、OpenAI 兼容 API |
| 管理端 | RuoYi-Vue（SpringBoot + Vue2 ElementUI） |
| 存储 | 阿里云 OSS / 本地存储（双通道可切换） |

## 快速开始

### 环境要求

JDK 21、Maven 3.8+、Node 18+、Python 3.11+、MySQL 8、Redis、RabbitMQ、Elasticsearch 8.x（可选）

### 1. 初始化数据库

```sql
CREATE DATABASE life_social DEFAULT CHARACTER SET utf8mb4;
```

依次执行 `life-java/life-social/sql/` 下脚本：`all.sql`（全量基线）→ `06_content_upgrade.sql` → `07_hot_feed_upgrade.sql` → `08_p0_lifecycle_upgrade.sql`。
RabbitMQ 无需手工建队列，交换机/队列（含死信与延迟队列）由应用启动时声明式自动创建。

### 2. 启动 life-social（C 端服务，:8080）

修改 `life-server/src/main/resources/application-dev.yml`：MySQL、Redis、RabbitMQ、ES（`life.es`，不配则检索自动降级）、OSS/本地存储、`life.agent`（AI 服务地址与内部令牌）。敏感信息建议改用环境变量注入，勿提交明文密钥。

```bash
cd life-java/life-social
mvn spring-boot:run -pl life-server
```

接口文档：`http://localhost:8080/swagger-ui.html`

### 3. 启动 life-admin（RuoYi 管理端，:8090）

按 `life-admin/README.md` 标准若依流程启动（共用 `life_social` 库），承担权益/商品/订单/公告/流水的运营管理。

### 4. 启动 life-agent（AI 服务，:8000，可选）

```bash
cd life-agent
cp .env.example .env    # 配置 OPENAI_API_KEY、回调 life-social 的内部令牌等
pip install -r requirements.txt && pip install -e .
uvicorn app.main:app --port 8000     # 或 docker-compose up
```

不启动时 C 端 AI 入口自动不可用，不影响其余业务。

### 5. 启动 life-frontend（:5173）

```bash
cd life-frontend
npm install
npm run dev        # 代理已指向 http://localhost:8080
```

## 项目结构

```
life
├── life-java/life-social          # C端服务（Maven 三模块）
│   ├── life-common                #   常量/异常/工具/拦截器基类/配置属性
│   ├── life-pojo                  #   entity / dto / vo / query
│   ├── life-server                #   controller / service / mapper / listener(MQ) /
│   │                              #   task(定时) / websocket / interceptor / config
│   ├── sql                        #   建库脚本（all.sql + 06/07/08 增量）
│   └── docs                       #   设计文档（见下）
├── life-admin                     # RuoYi 管理端（含 life 业务模块）
├── life-frontend                  # Vue3 C端前端（views / api / components / stores）
└── life-agent                     # Python AI Agent（FastAPI + LangGraph + Milvus）
```

## 核心链路

- **积分发放**：签到/发帖/任务完成 → RabbitMQ（`points.direct`）→ 消费者幂等入账（原子加分 + 流水同事务，`biz_no` 去重）→ 失败进死信留痕
- **信息流查询**：请求 → Sentinel 限流 → L1 Caffeine / L2 Redis → 未命中回源 MySQL（hot_score 排序，布隆过滤器防穿透）→ 关键词走 ES（降级 LIKE）→ 批量组装作者/关注/点赞状态
- **兑换下单**：加购（setnx 锁）→ 结算走唯一下单入口 → Redis+Lua 预扣 → DB 原子扣减/冻结积分 → 落单 + 同事务清车 → TTL 延迟关单兜底 → 对账任务周期校正

## 设计文档

详见 `life-java/life-social/docs/`：`gap-analysis-roadmap.md`（差距分析与路线图）、`api-todo-checklist.md`（接口盘点）、`db-tables-by-module.md`（库表归属），以及关注/积分任务/订单模块的接口设计约定文档。
