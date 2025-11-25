import { httpClient } from './httpClient'
import type { ApiResponse, CashierOrder, OrderStateResponse } from '../types'

/**
 * 获取订单详情
 */
export async function getOrderDetail(orderId: string): Promise<CashierOrder> {
  const response = await httpClient.get<ApiResponse<CashierOrder>>(
    `/api/cashier/orders/${orderId}`
  )
  return response.data.data
}

/**
 * 获取订单状态（用于轮询）
 */
export async function getOrderState(orderId: string): Promise<OrderStateResponse> {
  const response = await httpClient.get<ApiResponse<OrderStateResponse>>(
    `/api/cashier/orders/${orderId}/state`
  )
  return response.data.data
}
