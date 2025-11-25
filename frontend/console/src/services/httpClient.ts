import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/auth'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const httpClient = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加 Token
httpClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理错误和 Token 刷新
httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const authStore = useAuthStore()
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // 401 错误处理
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      // 尝试刷新 Token
      if (authStore.refreshToken) {
        try {
          const newToken = await authStore.refreshAccessToken()
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return httpClient(originalRequest)
        } catch {
          // 刷新失败，跳转登录页
          authStore.logout()
          window.location.href = '/login'
          return Promise.reject(error)
        }
      } else {
        // 没有 refresh token，跳转登录页
        authStore.logout()
        window.location.href = '/login'
      }
    }

    // 403 错误 - 权限不足
    if (error.response?.status === 403) {
      console.error('权限不足')
    }

    return Promise.reject(error)
  }
)

export default httpClient
