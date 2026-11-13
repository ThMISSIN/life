<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="ai-main">
      <div class="ai-header">
        <h2 class="ai-header__title">
          <span class="ms-icon ai-header__icon">smart_toy</span>
          AI 助手
        </h2>
        <span class="ai-status" :class="`ai-status--${status.state}`" :title="status.reason">
          <span class="ai-status__dot" />
          {{ statusText }}
        </span>
      </div>

      <div class="ai-body card">
        <!-- 消息区 -->
        <div ref="listRef" class="ai-msgs">
          <!-- 空态: 欢迎语 + 快捷提问 -->
          <div v-if="!messages.length" class="ai-welcome">
            <span class="ms-icon ms-icon--fill ai-welcome__icon">forum</span>
            <p class="ai-welcome__text">你好，我是平台 AI 助手，可以帮你查积分、找权益、答疑平台规则。</p>
            <div class="ai-welcome__chips">
              <button
                v-for="q in suggestions"
                :key="q"
                class="ai-chip"
                @click="input = q"
              >{{ q }}</button>
            </div>
          </div>

          <div
            v-for="(m, i) in messages"
            :key="i"
            class="msg"
            :class="`msg--${m.role}`"
          >
            <div class="msg__bubble">
              <!-- 澄清反问: 打标 + 一键直接作答 -->
              <div v-if="m.data?.needClarification" class="msg__clarify">
                <el-tag size="small" type="warning">需要澄清</el-tag>
                <el-button
                  size="small"
                  text
                  type="primary"
                  :disabled="sending"
                  @click="send(m.question, true)"
                >跳过澄清，直接作答</el-button>
              </div>

              <p class="msg__text">{{ m.text }}</p>

              <!-- 应答元信息: 意图/模式/置信度 -->
              <div v-if="m.data && m.data.answer" class="msg__meta">
                <span v-if="m.data.intent">意图: {{ m.data.intent }}<template v-if="m.data.subIntent"> / {{ m.data.subIntent }}</template></span>
                <span v-if="m.data.modeUsed">模式: {{ m.data.modeUsed }}</span>
                <span v-if="m.data.confidence != null">置信度: {{ Math.round(m.data.confidence * 100) }}%</span>
                <span v-if="m.data.degraded" class="msg__degraded">已降级 react</span>
              </div>

              <!-- 执行轨迹(折叠): phase/action/observation -->
              <details v-if="m.data?.steps?.length" class="msg__steps">
                <summary>执行轨迹（{{ m.data.steps.length }} 步）</summary>
                <ol>
                  <li v-for="(s, si) in m.data.steps" :key="si" class="msg__step">
                    <b>[{{ s.phase }}]</b> {{ s.action }}
                    <span v-if="s.observation" class="msg__step-obs">→ {{ s.observation }}</span>
                    <span v-if="s.status && s.status !== 'ok'" class="msg__step-err">({{ s.status }}{{ s.error ? ': ' + s.error : '' }})</span>
                  </li>
                </ol>
              </details>

              <!-- 反思报告(折叠) -->
              <details v-if="m.data?.reflection" class="msg__steps">
                <summary>反思审查{{ m.data.reflection.qualityScore != null ? `（质量分 ${m.data.reflection.qualityScore}）` : '' }}</summary>
                <p v-if="m.data.reflection.summary">{{ m.data.reflection.summary }}</p>
                <ul v-if="m.data.reflection.suggestions?.length">
                  <li v-for="(sg, gi) in m.data.reflection.suggestions" :key="gi">{{ sg }}</li>
                </ul>
              </details>
            </div>
          </div>

          <!-- 等待应答 -->
          <div v-if="sending" class="msg msg--assistant">
            <div class="msg__bubble msg__bubble--thinking">
              <span class="ms-icon ai-thinking">more_horiz</span> 思考中…
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="ai-input">
          <el-select v-model="mode" class="ai-input__mode" size="default">
            <el-option label="ReAct" value="react" />
            <el-option label="规划执行" value="plan_execute" />
          </el-select>
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="500"
            placeholder="输入问题，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="send()"
          />
          <el-button
            type="primary"
            class="ai-input__send"
            :loading="sending"
            :disabled="!input.trim()"
            @click="send()"
          >发送</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * AI 助手页 — 对接后端 AiController(/ai/**):
 *   探活 GET /ai/status(data.ready) → 状态徽标;
 *   对话 POST /ai/chat → AgentAnswerVO(answer/needClarification/steps/reflection...)。
 * Java 端只做鉴权与转发, AI 编排在 life-agent(Python); Agent 未启动时
 * chat 走 code=0 优雅降级(request 层 reject), 本页以错误气泡呈现, 链路不炸。
 * 会话: sessionId 每次进页生成, 后端拼 user:{uid}:{sid} 隔离多话题记忆。
 * 流式(/ai/chat/stream)待二期用 fetch+ReadableStream 接入。
 */
import { computed, nextTick, onMounted, ref } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { aiChat, getAiStatus } from '@/api/ai'

const suggestions = [
  '我现在有多少积分？',
  '有什么热门的积分权益推荐？',
  '签到奖励规则是什么？',
]

// 状态徽标: checking 探活中 / online 可用 / offline 不可用(reason 悬浮提示)
const status = ref({ state: 'checking', reason: '' })
const statusText = computed(() =>
  ({ checking: '检测中…', online: 'AI 在线', offline: 'AI 不可用' })[status.value.state],
)

const messages = ref([])   // {role: user|assistant|error, text, data?, question?}
const input = ref('')
const mode = ref('react')
const sending = ref(false)
const listRef = ref(null)
// 会话主题ID: 一次进页一个话题(时间戳即可, 后端会强制加 user:{uid} 前缀)
const sessionId = `web-${Date.now()}`

onMounted(async () => {
  try {
    const data = await getAiStatus()
    status.value = data?.ready
      ? { state: 'online', reason: '' }
      : { state: 'offline', reason: data?.reason || 'Agent 服务未就绪' }
  } catch (e) {
    status.value = { state: 'offline', reason: e.message }
  }
})

async function send(question, forceAnswer = false) {
  const q = (question ?? input.value).trim()
  if (!q || sending.value) return
  if (!question) input.value = ''
  messages.value.push({ role: 'user', text: q })
  scrollToBottom()

  sending.value = true
  try {
    const vo = await aiChat(q, { sessionId, mode: mode.value, forceAnswer })
    messages.value.push({
      role: 'assistant',
      // 澄清反问时 answer 是追问文案; success=false 时展示 error
      text: vo?.answer || vo?.error || '(空应答)',
      data: vo,
      question: q, // 供"直接作答"按钮重发
    })
  } catch (e) {
    // code=0 业务失败(Agent 未启动即走这里)或网络异常: 以错误气泡降级呈现
    messages.value.push({ role: 'error', text: e.message || 'AI 服务暂时不可用，请稍后重试' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}
</script>

<style scoped>
.ai-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ai-header__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
}
.ai-header__icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

/* 状态徽标 */
.ai-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.ai-status__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
}
.ai-status--online .ai-status__dot { background: var(--el-color-success); }
.ai-status--offline .ai-status__dot { background: var(--el-color-danger); }
.ai-status--checking .ai-status__dot { background: var(--el-color-warning); }

/* 主体卡片 */
.ai-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.ai-msgs {
  flex: 1;
  min-height: 320px;
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 空态欢迎 */
.ai-welcome {
  margin: auto;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.ai-welcome__icon {
  font-size: 44px;
  color: var(--el-color-primary-light-3);
}
.ai-welcome__text {
  margin: 8px 0 14px;
  font-size: 14px;
}
.ai-welcome__chips {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}
.ai-chip {
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color-blank);
  border-radius: 16px;
  padding: 5px 12px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.15s;
}
.ai-chip:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

/* 消息气泡 */
.msg {
  display: flex;
}
.msg--user { justify-content: flex-end; }
.msg__bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.msg--user .msg__bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg--assistant .msg__bubble { border-bottom-left-radius: 4px; }
.msg--error .msg__bubble {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  border: 1px solid var(--el-color-danger-light-7);
}
.msg__bubble--thinking {
  color: var(--el-text-color-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.ai-thinking {
  animation: ai-blink 1s infinite;
}
@keyframes ai-blink {
  50% { opacity: 0.3; }
}

/* 澄清条 */
.msg__clarify {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.msg__text {
  margin: 0;
  white-space: pre-wrap;
}

/* 元信息 */
.msg__meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.msg__degraded { color: var(--el-color-warning); }

/* 轨迹/反思折叠块 */
.msg__steps {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.msg__steps summary {
  cursor: pointer;
  user-select: none;
}
.msg__steps ol,
.msg__steps ul {
  margin: 6px 0 0;
  padding-left: 18px;
}
.msg__step { margin-bottom: 4px; }
.msg__step-obs { color: var(--el-text-color-regular); }
.msg__step-err { color: var(--el-color-danger); }

/* 输入区 */
.ai-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.ai-input__mode { width: 118px; flex-shrink: 0; }
.ai-input :deep(.el-textarea) { flex: 1; }
.ai-input__send { flex-shrink: 0; }
</style>
