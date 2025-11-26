import axios from 'axios'
import type { ApiResponse, CashierOrder, OrderStateResponse } from '../types'

// 收银台专用的 HTTP 客户端（不需要认证）
const cashierClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

cashierClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('Cashier API Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

/**
 * 获取订单详情
 */
export async function getOrderDetail(orderId: string): Promise<CashierOrder> {
  const response = await cashierClient.get<ApiResponse<CashierOrder>>(
    `/api/cashier/orders/${orderId}`
  )
  return response.data.data
}

/**
 * 获取订单状态（用于轮询）
 */
export async function getOrderState(orderId: string): Promise<OrderStateResponse> {
  const response = await cashierClient.get<ApiResponse<OrderStateResponse>>(
    `/api/cashier/orders/${orderId}/state`
  )
  return response.data.data
}
