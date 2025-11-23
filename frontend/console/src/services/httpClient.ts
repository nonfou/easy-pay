import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const httpClient = axios.create({
  baseURL,
  timeout: 10000,
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // eslint-disable-next-line no-console
    console.error('API Error', error)
    return Promise.reject(error)
  },
)
