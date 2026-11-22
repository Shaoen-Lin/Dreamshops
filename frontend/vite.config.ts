import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const base = env.VITE_API_BASE_URL || '/api/v1'
  const proxy = base.startsWith('/') && !base.startsWith('//') ? {
    [base.replace(/\/$/, '')]: {
      target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  } : undefined

  return {
    plugins: [react(), tailwindcss()],
    server: { proxy },
    preview: { proxy },
    test: { environment: 'jsdom', setupFiles: ['src/test-setup.ts'] },
  }
})
