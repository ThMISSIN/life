<template>
  <AuthShell>
    <h1 class="login__title">正在发生</h1>
    <h2 class="login__subtitle">探索专属权益与全能社交</h2>
    <p class="login__desc">登录您的账户，立即连接数字资产、社区动态与全景特权生态。</p>

    <el-form class="login__form" @submit.prevent="handleLogin">
      <el-input
        v-model="account"
        class="login__input"
        size="large"
        placeholder="请输入用户名/电话号码/邮箱"
        autocomplete="username"
        :disabled="logging"
      />
      <el-input
        v-model="password"
        class="login__input"
        size="large"
        type="password"
        placeholder="请输入密码"
        autocomplete="current-password"
        show-password
        :disabled="logging"
        @keyup.enter="handleLogin"
      />

      <div class="login__links">
        <router-link class="login__link-forgot" to="/forgot">忘记密码?</router-link>
        <router-link class="login__link-register" to="/register">注册账号</router-link>
      </div>

      <el-button
        class="login__submit"
        type="primary"
        size="large"
        round
        :loading="logging"
        @click="handleLogin"
      >
        登录
      </el-button>
    </el-form>

    <p class="login__agreement">
      继续则表示您同意我们的
      <a @click.prevent>服务条款</a>、<a @click.prevent>隐私政策</a> 和
      <a @click.prevent>Cookie 使用政策</a>。
    </p>
  </AuthShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { login, logging } = useUserStore()

const account = ref('')
const password = ref('')

async function handleLogin() {
  if (logging.value) return
  const acc = account.value.trim()
  if (!acc) return ElMessage.warning('请输入用户名/电话号码/邮箱')
  if (!password.value) return ElMessage.warning('请输入密码')

  const res = await login(acc, password.value)
  if (!res.ok) {
    ElMessage.error(res.message)
    return
  }
  ElMessage.success('登录成功')
  // 回跳来源页，避免 open redirect（仅允许站内路径）
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.replace(redirect.startsWith('/') ? redirect : '/')
}
</script>

<style scoped>
.login__title {
  margin: 0 0 10px;
  font-size: 56px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--color-text);
  line-height: 1.15;
}

.login__subtitle {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
}

.login__desc {
  margin: 0 0 36px;
  font-size: 15px;
  color: var(--color-text-muted);
}

.login__form {
  display: flex;
  flex-direction: column;
}

.login__input {
  margin-bottom: 16px;
}

.login__input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 16px;
  box-shadow: 0 0 0 1px var(--color-border-strong) inset;
}

.login__input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.login__links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  font-size: 14px;
}

.login__link-forgot {
  color: var(--color-text);
}

.login__link-register {
  color: var(--color-primary);
  font-weight: 600;
}

.login__submit {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
}

.login__agreement {
  margin: 18px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.login__agreement a {
  color: var(--color-primary);
  cursor: pointer;
}

@media (max-width: 480px) {
  .login__title {
    font-size: 40px;
  }
}
</style>
