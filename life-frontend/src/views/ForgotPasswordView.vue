<template>
  <AuthShell>
    <h1 class="forgot__title">重置密码</h1>
    <p class="forgot__desc">输入您的账号与新密码，完成重置后即可使用新密码登录。</p>

    <el-form class="forgot__form" @submit.prevent="handleSubmit">
      <el-input
        v-model="account"
        class="forgot__input"
        size="large"
        placeholder="请输入用户名/电话号码/邮箱"
        :disabled="submitting"
      />
      <el-input
        v-model="password"
        class="forgot__input"
        size="large"
        type="password"
        placeholder="请输入新密码"
        show-password
        :disabled="submitting"
      />
      <el-input
        v-model="confirmPassword"
        class="forgot__input"
        size="large"
        type="password"
        placeholder="请再次输入新密码"
        show-password
        :disabled="submitting"
        @keyup.enter="handleSubmit"
      />

      <el-button
        class="forgot__submit"
        type="primary"
        size="large"
        round
        :loading="submitting"
        @click="handleSubmit"
      >
        重置密码
      </el-button>

      <p class="forgot__to-login">
        想起密码了？
        <router-link class="forgot__login-link" to="/login">返回登录</router-link>
      </p>
    </el-form>
  </AuthShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { forgetPassword } from '@/api/auth'

const router = useRouter()
const submitting = ref(false)
const account = ref('')
const password = ref('')
const confirmPassword = ref('')

async function handleSubmit() {
  if (submitting.value) return
  if (!account.value.trim()) return ElMessage.warning('请输入用户名/电话号码/邮箱')
  if (!password.value) return ElMessage.warning('请输入新密码')
  if (password.value !== confirmPassword.value) return ElMessage.warning('两次输入的密码不一致')

  submitting.value = true
  try {
    await forgetPassword({
      account: account.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '密码重置失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.forgot__title {
  margin: 0 0 12px;
  font-size: 44px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--color-text);
  line-height: 1.15;
}

.forgot__desc {
  margin: 0 0 32px;
  font-size: 15px;
  color: var(--color-text-muted);
}

.forgot__form {
  display: flex;
  flex-direction: column;
}

.forgot__input {
  margin-bottom: 16px;
}

.forgot__input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 16px;
  box-shadow: 0 0 0 1px var(--color-border-strong) inset;
}

.forgot__input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.forgot__submit {
  width: 100%;
  height: 48px;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 700;
}

.forgot__to-login {
  margin: 18px 0 0;
  font-size: 14px;
  color: var(--color-text-muted);
  text-align: center;
}

.forgot__login-link {
  color: var(--color-primary);
  font-weight: 600;
}

@media (max-width: 480px) {
  .forgot__title {
    font-size: 34px;
  }
}
</style>
