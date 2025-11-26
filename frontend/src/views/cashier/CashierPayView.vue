<template>
  <div class="pay-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载订单信息...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">!</div>
      <h2>订单加载失败</h2>
      <p>{{ error }}</p>
      <button @click="router.push('/cashier')">返回首页</button>
    </div>

    <!-- 订单信息 -->
    <div v-else-if="order" class="order-container">
      <!-- 头部信息 -->
      <header class="header">
        <div class="payment-type" :class="order.type">
          <span class="icon">{{ paymentIcon }}</span>
          <span class="name">{{ paymentName }}</span>
        </div>
        <div class="amount">
          <span class="currency">¥</span>
          <span class="price">{{ order.reallyPrice.toFixed(2) }}</span>
        </div>
      </header>

      <!-- 订单详情 -->
      <section class="order-info">
        <div class="info-row">
          <span class="label">商品名称</span>
          <span class="value">{{ order.name }}</span>
        </div>
        <div class="info-row">
          <span class="label">订单号</span>
          <span class="value order-id">{{ order.orderId }}</span>
        </div>
        <div v-if="order.money !== order.reallyPrice" class="info-row">
          <span class="label">原价</span>
          <span class="value original-price">¥{{ order.money.toFixed(2) }}</span>
        </div>
      </section>

      <!-- 二维码区域 -->
      <section v-if="isPending" class="qrcode-section">
        <div class="qrcode-wrapper">
          <!-- 如果有后端提供的二维码图片 -->
          <img v-if="order.qrcodeUrl" :src="qrcodeImageUrl" alt="支付二维码" class="qrcode-image" />
          <!-- 否则显示占位提示 -->
          <div v-else class="qrcode-placeholder">
            <p>请使用{{ paymentName }}扫码支付</p>
            <p class="sub">收款码加载中...</p>
          </div>
        </div>
        <p class="scan-tip">请使用{{ paymentName }}扫描二维码完成支付</p>
      </section>

      <!-- 倒计时 -->
      <section v-if="isPending" class="countdown">
        <div class="countdown-bar">
          <div class="progress" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <p class="countdown-text">
          订单将在 <strong>{{ formatTime(expireIn) }}</strong> 后关闭
        </p>
      </section>

      <!-- 非待支付状态 -->
      <section v-if="!isPending" class="status-section">
        <div class="status-icon" :class="statusClass">
          {{ statusIcon }}
        </div>
        <h3 class="status-text">{{ statusText }}</h3>
        <button v-if="order.returnUrl" class="return-btn" @click="goBack">返回商户</button>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderDetail, getOrderState } from '../../services/cashierApi'
import {
  OrderState,
  PaymentTypeText,
  OrderStateText,
  type CashierOrder,
  type OrderStateType,
} from '../../types/cashier'

const props = defineProps<{
  orderId: string
}>()

const router = useRouter()

const loading = ref(true)
const error = ref('')
const order = ref<CashierOrder | null>(null)
const expireIn = ref(0)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownTimer = ref<ReturnType<typeof setInterval> | null>(null)

// 计算属性
const isPending = computed(() => order.value?.state === OrderState.PENDING)

const paymentIcon = computed(() => {
  if (!order.value) return ''
  return order.value.type === 'wxpay' ? '' : ''
})

const paymentName = computed(() => {
  if (!order.value) return ''
  return PaymentTypeText[order.value.type] || order.value.type
})

const qrcodeImageUrl = computed(() => {
  if (!order.value?.qrcodeUrl) return ''
  // 如果是相对路径，加上 API 基础 URL
  if (order.value.qrcodeUrl.startsWith('/')) {
    const baseUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
    return baseUrl + order.value.qrcodeUrl
  }
  return order.value.qrcodeUrl
})

const progressPercent = computed(() => {
  if (!order.value) return 0
  // 假设订单有效期 3 分钟 (180秒)
  const totalSeconds = 180
  return Math.max(0, Math.min(100, (expireIn.value / totalSeconds) * 100))
})

const statusClass = computed(() => {
  if (!order.value) return ''
  switch (order.value.state) {
    case OrderState.SUCCESS:
      return 'success'
    case OrderState.FAILED:
      return 'failed'
    case OrderState.EXPIRED:
      return 'expired'
    default:
      return ''
  }
})

const statusIcon = computed(() => {
  if (!order.value) return ''
  switch (order.value.state) {
    case OrderState.SUCCESS:
      return '✓'
    case OrderState.FAILED:
      return '✗'
    case OrderState.EXPIRED:
      return '⏰'
    default:
      return ''
  }
})

const statusText = computed(() => {
  if (!order.value) return ''
  return OrderStateText[order.value.state as OrderStateType] || '未知状态'
})

// 方法
const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const loadOrder = async () => {
  try {
    loading.value = true
    error.value = ''
    order.value = await getOrderDetail(props.orderId)

    // 初始化倒计时
    if (order.value.closeTime) {
      const closeTime = new Date(order.value.closeTime).getTime()
      const now = Date.now()
      expireIn.value = Math.max(0, Math.floor((closeTime - now) / 1000))
    }

    // 如果是待支付状态，启动轮询
    if (isPending.value) {
      startPolling()
      startCountdown()
    }
  } catch (err: unknown) {
    const e = err as { response?: { data?: { message?: string } } }
    error.value = e.response?.data?.message || '订单不存在或已失效'
  } finally {
    loading.value = false
  }
}

const pollOrderState = async () => {
  try {
    const state = await getOrderState(props.orderId)
    expireIn.value = state.expireIn

    // 状态变化时更新订单
    if (order.value && order.value.state !== state.state) {
      order.value = { ...order.value, state: state.state }

      // 非待支付状态，停止轮询
      if (state.state !== OrderState.PENDING) {
        stopPolling()
        stopCountdown()

        // 跳转到结果页
        router.push({ name: 'cashier-result', params: { orderId: props.orderId } })
      }
    }

    // 倒计时结束
    if (state.expireIn <= 0 && order.value?.state === OrderState.PENDING) {
      order.value = { ...order.value, state: OrderState.EXPIRED }
      stopPolling()
      stopCountdown()
    }
  } catch (err) {
    console.error('Poll state error:', err)
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer.value = setInterval(pollOrderState, 3000)
  pollOrderState()
}

const stopPolling = () => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

const startCountdown = () => {
  stopCountdown()
  countdownTimer.value = setInterval(() => {
    if (expireIn.value > 0) {
      expireIn.value--
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
}

const goBack = () => {
  if (order.value?.returnUrl) {
    window.location.href = order.value.returnUrl
  }
}

// 生命周期
onMounted(() => {
  loadOrder()
})

onUnmounted(() => {
  stopPolling()
  stopCountdown()
})

// 监听订单 ID 变化
watch(
  () => props.orderId,
  () => {
    stopPolling()
    stopCountdown()
    loadOrder()
  }
)
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 加载状态 */
.loading {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 错误状态 */
.error-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  text-align: center;
}

.error-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #ff5252;
  color: white;
  font-size: 36px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.error-container h2 {
  color: #333;
  margin: 0 0 8px 0;
}

.error-container p {
  color: #666;
  margin: 0 0 24px 0;
}

.error-container button {
  padding: 12px 32px;
  border: none;
  border-radius: 8px;
  background: #667eea;
  color: white;
  font-size: 16px;
  cursor: pointer;
}

/* 订单容器 */
.order-container {
  max-width: 420px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* 头部 */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 20px;
  color: white;
  text-align: center;
}

.payment-type {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 14px;
  margin-bottom: 16px;
}

.payment-type.wxpay {
  background: rgba(9, 187, 7, 0.8);
}

.payment-type.alipay {
  background: rgba(0, 166, 234, 0.8);
}

.payment-type .icon {
  font-size: 18px;
}

.amount {
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.amount .currency {
  font-size: 24px;
  font-weight: 500;
  margin-right: 4px;
}

.amount .price {
  font-size: 48px;
  font-weight: 600;
  letter-spacing: -1px;
}

/* 订单信息 */
.order-info {
  background: white;
  margin: 16px;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: #999;
  font-size: 14px;
}

.info-row .value {
  color: #333;
  font-size: 14px;
  text-align: right;
  max-width: 60%;
  word-break: break-all;
}

.info-row .order-id {
  font-family: monospace;
  font-size: 13px;
}

.info-row .original-price {
  text-decoration: line-through;
  color: #999;
}

/* 二维码区域 */
.qrcode-section {
  background: white;
  margin: 16px;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.qrcode-wrapper {
  display: inline-block;
  padding: 16px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
}

.qrcode-image {
  display: block;
  width: 200px;
  height: 200px;
  object-fit: contain;
}

.qrcode-placeholder {
  width: 200px;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  border-radius: 8px;
  color: #666;
}

.qrcode-placeholder .sub {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.scan-tip {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}

/* 倒计时 */
.countdown {
  margin: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.countdown-bar {
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
}

.countdown-bar .progress {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 3px;
  transition: width 1s linear;
}

.countdown-text {
  margin-top: 12px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.countdown-text strong {
  color: #ff5252;
  font-size: 16px;
}

/* 状态区域 */
.status-section {
  margin: 16px;
  padding: 40px 24px;
  background: white;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.status-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 40px;
  color: white;
}

.status-icon.success {
  background: #4caf50;
}

.status-icon.failed {
  background: #ff5252;
}

.status-icon.expired {
  background: #ff9800;
}

.status-text {
  margin: 0 0 24px 0;
  color: #333;
  font-size: 20px;
}

.return-btn {
  padding: 12px 40px;
  border: none;
  border-radius: 8px;
  background: #667eea;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.return-btn:hover {
  background: #5a6fd6;
}
</style>
