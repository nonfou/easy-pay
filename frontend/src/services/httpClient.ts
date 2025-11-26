import axios from 'axios'

// 开发环境使用空字符串让 Vite 代理生效，生产环境使用配置的 API 地址
const baseURL = import.meta.env.VITE_API_BASE || ''

export const httpClient = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器 - 处理错误
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API 请求错误:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default httpClient
