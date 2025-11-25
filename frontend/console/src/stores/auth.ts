import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CurrentUser, TokenResponse } from '../types'
import { httpClient } from '../services/httpClient'

export const useAuthStore = defineStore(
  'auth',
  () => {
    // 状态
    const accessToken = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const user = ref<CurrentUser | null>(null)

    // 计算属性
    const isAuthenticated = computed(() => !!accessToken.value)
    const isAdmin = computed(() => user.value?.role === 1)

    // 登录
    async function login(username: string, password: string) {
      const response = await httpClient.post<{ data: TokenResponse }>('/api/auth/login', {
        username,
        password,
      })
      const tokenData = response.data.data
      accessToken.value = tokenData.accessToken
      refreshToken.value = tokenData.refreshToken

      // 获取用户信息
      await fetchCurrentUser()
    }

    // 获取当前用户信息
    async function fetchCurrentUser() {
      try {
        const response = await httpClient.get<{ data: CurrentUser }>('/api/auth/me')
        user.value = response.data.data
      } catch {
        // 获取用户信息失败，清除 token
        logout()
        throw new Error('获取用户信息失败')
      }
    }

    // 刷新 Token
    async function refreshAccessToken() {
      if (!refreshToken.value) {
        throw new Error('No refresh token')
      }

      try {
        const response = await httpClient.post<{ data: TokenResponse }>('/api/auth/refresh', {
          refreshToken: refreshToken.value,
        })
        const tokenData = response.data.data
        accessToken.value = tokenData.accessToken
        refreshToken.value = tokenData.refreshToken
        return tokenData.accessToken
      } catch {
        logout()
        throw new Error('Token 刷新失败')
      }
    }

    // 登出
    function logout() {
      accessToken.value = null
      refreshToken.value = null
      user.value = null
    }

    // 初始化 (应用启动时调用)
    async function initialize() {
      if (accessToken.value) {
        try {
          await fetchCurrentUser()
        } catch {
          // 忽略错误，已在 fetchCurrentUser 中处理
        }
      }
    }

    return {
      // 状态
      accessToken,
      refreshToken,
      user,
      // 计算属性
      isAuthenticated,
      isAdmin,
      // 方法
      login,
      logout,
      fetchCurrentUser,
      refreshAccessToken,
      initialize,
    }
  },
  {
    persist: {
      pick: ['accessToken', 'refreshToken'],
    },
  }
)
