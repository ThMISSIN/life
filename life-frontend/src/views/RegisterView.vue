<template>
  <AuthShell>
    <h1 class="register__title">加入 Omni-Life</h1>
    <p class="register__desc">创建您的账户，开启全能生活权益与全景特权生态。</p>

    <el-form class="register__form" @submit.prevent="handleRegister">
      <el-input
        v-model="form.username"
        class="register__input"
        size="large"
        placeholder="请输入用户名"
        :disabled="submitting"
      />
      <el-input
        v-model="form.phone"
        class="register__input"
        size="large"
        placeholder="请输入手机号"
        maxlength="11"
        :disabled="submitting"
      />
      <el-input
        v-model="form.email"
        class="register__input"
        size="large"
        placeholder="请输入邮箱（选填）"
        :disabled="submitting"
      />
      <el-input
        v-model="form.password"
        class="register__input"
        size="large"
        type="password"
        placeholder="请输入密码"
        show-password
        :disabled="submitting"
      />
      <el-input
        v-model="form.confirmPassword"
        class="register__input"
        size="large"
        type="password"
        placeholder="请再次输入密码"
        show-password
        :disabled="submitting"
        @keyup.enter="handleRegister"
      />

      <el-button
        class="register__submit"
        type="primary"
        size="large"
        round
        :loading="submitting"
        @click="handleRegister"
      >
        注册
      </el-button>

      <p class="register__to-login">
        已有账号？
        <router-link class="register__login-link" to="/login">直接登录</router-link>
      </p>
    </el-form>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { register } from '@/api/auth'

const router = useRouter()
const submitting = ref(false)
const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

async function handleRegister() {
  if (submitting.value) return
  const username = form.username.trim()
  const phone = form.phone.trim()
  const email = form.email.trim()

  if (!username) return ElMessage.warning('请输入用户名')
  if (!/^1[3-9]\d{9}$/.test(phone)) return ElMessage.warning('请输入正确的手机号')
  if (email && !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(email))
    return ElMessage.warning('请输入正确的邮箱地址')
  if (!form.password) return ElMessage.warning('请输入密码')
  if (form.password !== form.confirmPassword) return ElMessage.warning('两次输入的密码不一致')

  submitting.value = true
  try {
    await register({ username, phone, email: email || null, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register__title {
  margin: 0 0 12px;
  font-size: 44px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--color-text);
  line-height: 1.15;
}

.register__desc {
  margin: 0 0 32px;
  font-size: 15px;
  color: var(--color-text-muted);
}

.register__form {
  display: flex;
  flex-direction: column;
}

.register__input {
  margin-bottom: 16px;
}

.register__input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 16px;
  box-shadow: 0 0 0 1px var(--color-border-strong) inset;
}

.register__input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.register__submit {
  width: 100%;
  height: 48px;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 700;
}

.register__to-login {
  margin: 18px 0 0;
  font-size: 14px;
  color: var(--color-text-muted);
  text-align: center;
}

.register__login-link {
  color: var(--color-primary);
  font-weight: 600;
}

@media (max-width: 480px) {
  .register__title {
    font-size: 34px;
  }
}
</style>
