import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    // 将体积较大的依赖拆分为独立 chunk
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
        },
      },
    },
  },
  server: {
    port: 5173,
    host: true,
    // 后端接口文档确定后，将 proxy.target 改为真实后端地址，
    // 前端统一通过 /api 前缀调用，避免跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 剥离 /api 前缀：/api/user/user/login -> http://localhost:8080/user/user/login
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 本地存储的上传文件静态访问（后端 /uploads/** 静态映射）
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
