<template>
  <div class="search-bar">
    <el-icon class="search-bar__icon"><Search /></el-icon>
    <input
      v-model="keyword"
      class="search-bar__input"
      type="text"
      placeholder="搜索"
      @keyup.enter="handleSearch"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const keyword = ref('')
const emit = defineEmits(['search'])

function handleSearch() {
  emit('search', keyword.value.trim())
}
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--color-search-bg);
  border: 1px solid transparent;
  border-radius: 9999px;
  transition:
    border-color 0.2s,
    background-color 0.2s;
}

.search-bar:focus-within {
  background: var(--color-surface);
  border-color: var(--color-primary);
}

.search-bar__icon {
  color: var(--color-text-secondary);
  font-size: 18px;
}

.search-bar:focus-within .search-bar__icon {
  color: var(--color-primary);
}

.search-bar__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text);
}

.search-bar__input::placeholder {
  color: var(--color-text-secondary);
}
</style>
