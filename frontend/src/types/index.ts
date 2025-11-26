// API 响应通用结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// Token 响应
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

// 当前用户信息
export interface CurrentUser {
  pid: number
  username: string
  email: string
  role: number
  roleName: string
}

// 订单
export interface Order {
  id: number
  orderId: string
  pid: number
  type: string
  outTradeNo: string
  notifyUrl: string
  returnUrl: string
  name: string
  money: number
  reallyPrice: number
  clientip: string
  device: string
  param: string
  state: number
  createTime: string
  closeTime: string
  payTime: string
  platformOrder: string
}

// 收入统计
export interface RevenueStatistics {
  totalRevenue: number
  todayRevenue: number
  orderCount: number
  todayOrderCount: number
  successRate: number
}

// 支付类型统计
export interface PaymentTypeStatistics {
  type: string
  count: number
  amount: number
  percentage: number
}

// 订单趋势
export interface OrderTrend {
  date: string
  count: number
  amount: number
}

// 收款账号
export interface PayAccount {
  id: number
  pid: number
  platform: string
  account: string
  state: number
  pattern: number
  createdAt: string
  updatedAt: string
}

// 收款通道
export interface PayChannel {
  id: number
  accountId: number
  channel: string
  qrcode: string
  lastTime: string
  state: number
  type: string
}

// 账号摘要 (包含通道)
export interface AccountSummary {
  id: number
  platform: string
  account: string
  state: number
  pattern: number
  channels: PayChannel[]
}

// 收银台类型
export * from './cashier'
