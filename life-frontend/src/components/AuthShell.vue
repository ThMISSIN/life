<template>
  <div class="auth-shell">
    <!-- 顶部品牌栏 -->
    <header class="auth-shell__header">
      <router-link to="/login" class="auth-shell__brand">
        <span class="ms-icon ms-icon--fill auth-shell__brand-icon">all_inclusive</span>
        <span class="auth-shell__brand-name">Omni-Life 全能生活</span>
      </router-link>
    </header>

    <!-- 主体：左侧表单内容（插槽） + 右侧品牌大 Logo -->
    <main class="auth-shell__main">
      <div class="auth-shell__container">
        <section class="auth-shell__content">
          <slot />
        </section>

        <section class="auth-shell__visual" aria-hidden="true">
          <span class="ms-icon ms-icon--fill auth-shell__mark">all_inclusive</span>
          <span class="auth-shell__mark-dot" />
        </section>
      </div>
    </main>

    <!-- 底部链接栏 -->
    <footer class="auth-shell__footer">
      <nav class="auth-shell__footer-links">
        <a v-for="link in footerLinks" :key="link" class="auth-shell__footer-link" @click.prevent>
          {{ link }}
        </a>
        <span class="auth-shell__footer-copy">© 2026 Omni-Life Corp.</span>
      </nav>
    </footer>
  </div>
</template>

<script setup>
const footerLinks = [
  '关于',
  '下载 App',
  '权益规则',
  '帮助中心',
  '条款',
  '隐私政策',
  'Cookie 管理',
  '开发者平台',
  '商务合作',
]
</script>

<style scoped>
.auth-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-surface);
}

/* ===== 顶部品牌栏 ===== */
.auth-shell__header {
  padding: 28px 48px 0;
}

.auth-shell__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text);
}

.auth-shell__brand:hover {
  text-decoration: none;
}

.auth-shell__brand-icon {
  font-size: 28px;
}

.auth-shell__brand-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

/* ===== 主体 ===== */
.auth-shell__main {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 32px 48px;
}

.auth-shell__container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
  width: 100%;
  max-width: 1140px;
  margin: 0 auto;
}

.auth-shell__content {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}

.auth-shell__visual {
  position: relative;
  flex-shrink: 0;
  line-height: 1;
}

.auth-shell__mark {
  font-size: 300px;
  color: var(--color-text);
  font-variation-settings: 'FILL' 1, 'wght' 700, 'GRAD' 0, 'opsz' 48;
}

.auth-shell__mark-dot {
  position: absolute;
  top: 14%;
  left: 27%;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-primary);
}

/* ===== 底部链接栏 ===== */
.auth-shell__footer {
  padding: 20px 48px 28px;
}

.auth-shell__footer-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px 20px;
}

.auth-shell__footer-link,
.auth-shell__footer-copy {
  font-size: 13px;
  color: var(--color-text-muted);
  cursor: pointer;
}

.auth-shell__footer-link:hover {
  color: var(--color-text);
  text-decoration: underline;
}

.auth-shell__footer-copy {
  cursor: default;
}

@media (max-width: 960px) {
  .auth-shell__visual {
    display: none;
  }

  .auth-shell__header,
  .auth-shell__main,
  .auth-shell__footer {
    padding-left: 24px;
    padding-right: 24px;
  }
}
</style>
