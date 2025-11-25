/**
 * 订单状态枚举
 */
export const OrderState = {
  PENDING: 0,   // 待支付
  SUCCESS: 1,   // 支付成功
  FAILED: 2,    // 支付失败
  EXPIRED: 3,   // 已过期
} as const

export type OrderStateType = typeof OrderState[keyof typeof OrderState]

/**
 * 订单状态文本映射
 */
export const OrderStateText: Record<OrderStateType, string> = {
  [OrderState.PENDING]: '待支付',
  [OrderState.SUCCESS]: '支付成功',
  [OrderState.FAILED]: '支付失败',
  [OrderState.EXPIRED]: '已过期',
}

/**
 * 支付类型
 */
export const PaymentType = {
  WXPAY: 'wxpay',
  ALIPAY: 'alipay',
} as const

export type PaymentTypeValue = typeof PaymentType[keyof typeof PaymentType]

/**
 * 支付类型文本映射
 */
export const PaymentTypeText: Record<string, string> = {
  [PaymentType.WXPAY]: '微信支付',
  [PaymentType.ALIPAY]: '支付宝',
}

/**
 * 收银台订单详情
 */
export interface CashierOrder {
  orderId: string
  type: string
  name: string
  money: number
  reallyPrice: number
  qrcodeUrl: string | null
  state: OrderStateType
  createTime: string
  closeTime: string
  returnUrl: string | null
}

/**
 * 订单状态响应
 */
export interface OrderStateResponse {
  orderId: string
  state: OrderStateType
  expireIn: number
  returnUrl: string | null
}

/**
 * API 响应包装
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
