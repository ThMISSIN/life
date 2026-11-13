import request from './request'

/**
 * AI 助手接口（对应后端 AiController /ai/**，需登录）
 *
 * Java 端只做鉴权、会话隔离与转发，AI 逻辑在 Python Agent（life-agent）；
 * Agent 未启动时的降级表现（链路依然可通，均为结构化返回）：
 *   - /ai/status  → code=1, data={ready:false, reason:...}（后端探活失败兜底）
 *   - /ai/chat    → code=0, msg="AI 服务暂时不可用，请稍后重试"（request 层 reject(Error)）
 */

// Agent 能力探活：data.ready 决定是否展示"可用"徽标与知识库入口
export function getAiStatus() {
  return request.get('/ai/status')
}

/**
 * 非流式编排对话
 * @param {string} question 用户问题
 * @param {object} opts sessionId-会话主题(后端强制拼 user:{uid} 前缀防越权)；
 *                      mode-react(默认)|plan_execute；forceAnswer-低置信度时跳过澄清直接作答
 * @returns {Promise<AgentAnswerVO>} {needClarification, answer, intent, subIntent, confidence,
 *                                   modeUsed, success, degraded, traceId, steps[], reflection}
 */
export function aiChat(question, { sessionId, mode = 'react', forceAnswer = false } = {}) {
  return request.post('/ai/chat', { question, sessionId, mode, forceAnswer })
}

// 知识库检索（绕过 Agent 循环直查 topK 片段，调试/搜索框用）
export function searchKnowledge(query, topK = 5) {
  return request.get('/ai/knowledge/search', { params: { query, topK } })
}

// 流式对话 POST /ai/chat/stream（SSE 桥接）后端已就绪；
// axios 不支持 POST + 流式读取，接入时需改用 fetch + ReadableStream，页面预留二期
