import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const httpClient = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 响应拦截器
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('HTTP Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)
