import axios from 'axios'

const baseURL = import.meta.env.VITE_PAYMENT_BASE || 'http://localhost:8100'

export const httpClient = axios.create({
  baseURL,
  timeout: 10000,
})
