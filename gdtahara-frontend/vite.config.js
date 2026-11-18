import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Base path: '/' for dev, '/MES/' for production build
  // Set VITE_PROD=1 when building for production (avoids Git Bash path mangling)
  base: process.env.VITE_PROD ? '/MES/' : '/',
  server: {
    hmr: { overlay: false },
    host: 'localhost',
    port: 5173,
    proxy: {
      // Forward /api calls to Spring Boot during development
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  resolve: {
    // Avoid resolving to real paths on Windows/OneDrive which can trigger spawn EPERM in Vite's safe realpath checks
    preserveSymlinks: true,
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  // Fix for Windows path issues with spaces and special characters
  optimizeDeps: {
    esbuildOptions: {
      target: 'es2020',
    },
  },
  esbuild: {
    logOverride: { 'this-is-undefined-in-esm': 'silent' }
  }
})
