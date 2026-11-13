<template>
  <nav class="side-footer">
    <a href="javascript:;" @click.prevent>服务条款</a>
    <a href="javascript:;" @click.prevent>隐私政策</a>
    <a href="javascript:;" @click.prevent>Cookie政策</a>
    <a href="javascript:;" @click.prevent>无障碍</a>
    <a href="javascript:;" @click.prevent>广告信息</a>
    <a href="javascript:;" @click.prevent>更多</a>
    <span>© 2024 Omni-Life</span>
  </nav>
</template>

<style scoped>
.side-footer {
  display: flex;
  flex-wrap: wrap;
  column-gap: 12px;
  row-gap: 4px;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.side-footer a {
  color: inherit;
}

.side-footer a:hover {
  text-decoration: underline;
}
</style>
