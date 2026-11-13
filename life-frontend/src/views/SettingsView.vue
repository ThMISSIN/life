<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="settings-main">
      <!-- 吸顶标题栏 -->
      <div class="settings-header">
        <button class="settings-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <h2 class="settings-header__title">设置</h2>
      </div>

      <div class="settings-body">
        <!-- 账号卡片 -->
        <div class="profile-cell" @click="goProfile">
          <el-avatar :size="52" :src="user.avatar" class="profile-cell__avatar" />
          <div class="profile-cell__info">
            <p class="profile-cell__name">{{ user.nickname || user.username }}</p>
            <p class="profile-cell__meta">
              {{ membership.currentLevel }} · {{ formatNumber(user.points) }} 积分
            </p>
          </div>
          <span class="profile-cell__link">查看资料</span>
          <span class="ms-icon profile-cell__arrow">chevron_right</span>
        </div>

        <!-- 账户 -->
        <section class="settings-card">
          <h3 class="settings-card__title">账户</h3>
          <div class="cell" @click="rechargeVisible = true">
            <span class="ms-icon cell__icon">account_balance_wallet</span>
            <span class="cell__title">余额与充值</span>
            <span class="cell__value">¥{{ fenToYuan(user.money) }}</span>
            <span class="ms-icon cell__arrow">chevron_right</span>
          </div>
          <div class="cell" @click="goAddress">
            <span class="ms-icon cell__icon">location_on</span>
            <span class="cell__title">收货地址管理</span>
            <span class="cell__value">{{ addressCount ? `${addressCount} 条` : '未设置' }}</span>
            <span class="ms-icon cell__arrow">chevron_right</span>
          </div>
          <div class="cell" @click="openPassword">
            <span class="ms-icon cell__icon">lock</span>
            <span class="cell__title">修改密码</span>
            <span class="ms-icon cell__arrow">chevron_right</span>
          </div>
        </section>

        <!-- 通知 -->
        <section class="settings-card">
          <h3 class="settings-card__title">通知</h3>
          <div class="cell">
            <span class="ms-icon cell__icon">notifications</span>
            <span class="cell__title">积分变动提醒</span>
            <el-switch v-model="notifyPoints" @change="saveSetting('notifyPoints', $event)" />
          </div>
          <div class="cell">
            <span class="ms-icon cell__icon">thumb_up</span>
            <span class="cell__title">推荐动态</span>
            <el-switch v-model="recommendFeed" @change="saveSetting('recommendFeed', $event)" />
          </div>
          <p class="settings-card__tip">通知偏好已接入后端（PUT /user/settings），随账号云端同步</p>
        </section>

        <!-- 关于 -->
        <section class="settings-card">
          <h3 class="settings-card__title">关于</h3>
          <div class="cell">
            <span class="ms-icon cell__icon">info</span>
            <span class="cell__title">版本</span>
            <span class="cell__value">v1.0.0</span>
          </div>
          <div class="cell" @click="onHelp">
            <span class="ms-icon cell__icon">help</span>
            <span class="cell__title">帮助中心</span>
            <span class="ms-icon cell__arrow">chevron_right</span>
          </div>
        </section>

        <!-- 退出登录 -->
        <el-button class="logout-btn" round @click="onLogout">退出登录</el-button>
      </div>
    </main>

    <!-- 余额充值弹窗 -->
    <el-dialog v-model="rechargeVisible" title="余额充值" width="420px" destroy-on-close>
      <div class="recharge">
        <p class="recharge__current">当前余额：¥{{ fenToYuan(user.money) }}</p>
        <div class="recharge__presets">
          <button
            v-for="a in [10, 50, 100, 200, 500]"
            :key="a"
            class="recharge__preset"
            :class="{ 'is-active': rechargeAmount === a }"
            @click="rechargeAmount = a"
          >
            ¥{{ a }}
          </button>
        </div>
        <el-input-number
          v-model="rechargeAmount"
          :min="1"
          :max="10000"
          :precision="2"
          class="recharge__input"
          placeholder="自定义金额"
        />
        <p class="recharge__tip">一期为模拟支付渠道，确认后立即到账</p>
      </div>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="onRecharge">
          充值 ¥{{ rechargeAmount }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="440px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="6-20 位，建议字母 + 数字"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
            @keyup.enter="onChangePassword"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="onChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 设置页（与全站一致的卡片式桌面布局）
 * - 顶部账号卡片 + 分组卡片（账户 / 通知 / 关于）
 * - 修改密码（后端 PUT /user/user/update/password）
 * - 通知偏好（后端暂无用户设置接口，仅本地生效）
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import { getMembership, updatePassword, recharge as rechargeApi, getSettings, saveSettings } from '@/api/user'
import { getAddressList } from '@/api/address'
import { fenToYuan } from '@/api/order'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { user, loadUser, logout } = useUserStore()

// 通知偏好: 后端 0/1 <-> el-switch boolean, 变更即存(失败回滚开关)
const notifyPoints = ref(true)
const recommendFeed = ref(true)

async function saveSetting(key, on) {
  try {
    await saveSettings({ [key]: on ? 1 : 0 })
  } catch (e) {
    // 回滚 UI 状态
    if (key === 'notifyPoints') notifyPoints.value = !on
    else recommendFeed.value = !on
    ElMessage.error(e.message || '设置保存失败')
  }
}

const addressCount = ref(0)

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')
const membership = ref({ currentLevel: '', nextLevel: '', nextLevelPoints: 0, progress: 0 })

// —— 余额充值 ——
const rechargeVisible = ref(false)
const rechargeAmount = ref(50)
const recharging = ref(false)

async function onRecharge() {
  const amount = Number(rechargeAmount)
  if (!amount || amount < 1) {
    ElMessage.warning('请输入不少于 1 元的充值金额')
    return
  }
  recharging.value = true
  try {
    await rechargeApi(amount)
    ElMessage.success(`充值成功，已到账 ¥${amount}`)
    rechargeVisible.value = false
    loadUser(true)
  } catch (e) {
    ElMessage.error(e.message || '充值失败，请稍后重试')
  } finally {
    recharging.value = false
  }
}

// —— 修改密码 ——
const passwordVisible = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPassword = ref(false)

function openPassword() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

async function onChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm
  if (!oldPassword || !newPassword) {
    ElMessage.warning('请填写当前密码与新密码')
    return
  }
  if (newPassword.length < 6 || newPassword.length > 20) {
    ElMessage.warning('新密码长度需为 6-20 位')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await updatePassword({ oldPassword, newPassword })
    ElMessage.success('修改密码成功，下次登录请使用新密码')
    passwordVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '修改失败，请稍后重试')
  } finally {
    changingPassword.value = false
  }
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

function onHelp() {
  ElMessage.info('帮助中心（待接入后端）')
}

function goAddress() {
  router.push('/address')
}

function goProfile() {
  router.push('/profile')
}

function goBack() {
  router.back()
}

onMounted(async () => {
  await loadUser(true)
  membership.value = getMembership(user.points || 0)
  // 通知偏好回填(后端 0/1 -> boolean)
  getSettings().then((s) => {
    notifyPoints.value = Number(s?.notifyPoints) === 1
    recommendFeed.value = Number(s?.recommendFeed) === 1
  })
  try {
    const list = await getAddressList()
    addressCount.value = (list || []).length
  } catch {
    addressCount.value = 0
  }
})
</script>

<style scoped>
.settings-main {
  flex: 1;
  min-width: 0;
  width: 100%;
  max-width: 760px;
  min-height: 100vh;
  border-right: 1px solid var(--color-border);
}

.settings-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: rgba(250, 248, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.settings-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
  transition: background-color 0.2s;
}

.settings-header__back:hover {
  background: var(--color-surface-hover);
}

.settings-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.settings-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 16px 60px;
}

/* 顶部账号卡片 */
.profile-cell {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.profile-cell:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(15, 20, 25, 0.06);
}

.profile-cell__avatar {
  flex-shrink: 0;
  background: var(--color-surface-low);
}

.profile-cell__info {
  flex: 1;
  min-width: 0;
}

.profile-cell__name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
}

.profile-cell__meta {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.profile-cell__link {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
  flex-shrink: 0;
}

.profile-cell__arrow {
  font-size: 20px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* 分组卡片：与右侧栏「积分明细」等同款语言 */
.settings-card {
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
}

.settings-card__title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.settings-card__tip {
  margin: 8px 0 2px;
  padding-left: 52px;
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 单元格行 */
.cell {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  margin: 0 -8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.cell + .cell {
  border-top: 1px solid var(--color-border);
}

.cell:hover {
  background: var(--color-surface-hover);
}

.cell__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--color-surface-low);
  color: var(--color-primary);
  font-size: 20px;
  flex-shrink: 0;
}

.cell__title {
  flex: 1;
  font-size: 15px;
  color: var(--color-text);
}

.cell__value {
  font-size: 13px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.cell__arrow {
  font-size: 20px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* 退出登录 */
.logout-btn.el-button {
  width: 100%;
  padding: 14px 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-danger);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
}

.logout-btn.el-button:hover {
  color: #fff;
  background: var(--color-danger);
  border-color: var(--color-danger);
}

/* 充值弹窗 */
.recharge__current {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.recharge__presets {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.recharge__preset {
  min-width: 76px;
  padding: 10px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  background: var(--color-surface-low);
  border: 1px solid var(--color-border-strong);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.recharge__preset:hover {
  border-color: var(--color-primary);
}

.recharge__preset.is-active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: rgba(29, 155, 240, 0.06);
}

.recharge__input {
  width: 100%;
}

.recharge__tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
