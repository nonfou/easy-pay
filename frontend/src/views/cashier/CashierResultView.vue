<template>
  <div class="result-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="result-container error">
      <div class="icon error-icon">!</div>
      <h2>加载失败</h2>
      <p class="message">{{ error }}</p>
      <button class="btn primary" @click="router.push('/cashier')">返回首页</button>
    </div>

    <!-- 支付成功 -->
    <div v-else-if="isSuccess" class="result-container success">
      <div class="icon success-icon">✓</div>
      <h2>支付成功</h2>
      <p class="amount">¥{{ order?.reallyPrice.toFixed(2) }}</p>
      <div class="info">
        <p>
          <span>商品</span><span>{{ order?.name }}</span>
        </p>
        <p>
          <span>订单号</span><span class="mono">{{ order?.orderId }}</span>
        </p>
      </div>
      <div class="actions">
        <button v-if="order?.returnUrl" class="btn primary" @click="goBack">返回商户</button>
        <button class="btn secondary" @click="router.push('/cashier')">返回首页</button>
      </div>
    </div>

    <!-- 支付失败 -->
    <div v-else-if="isFailed" class="result-container failed">
      <div class="icon failed-icon">✗</div>
      <h2>支付失败</h2>
      <p class="message">订单支付未完成，请重试</p>
      <div class="info">
        <p>
          <span>订单号</span><span class="mono">{{ order?.orderId }}</span>
        </p>
      </div>
      <div class="actions">
        <button class="btn primary" @click="retry">重新支付</button>
        <button v-if="order?.returnUrl" class="btn secondary" @click="goBack">返回商户</button>
      </div>
    </div>

    <!-- 订单超时 -->
    <div v-else-if="isExpired" class="result-container expired">
      <div class="icon expired-icon">⏰</div>
      <h2>订单已过期</h2>
      <p class="message">订单超时未支付，请重新下单</p>
      <div class="info">
        <p>
          <span>订单号</span><span class="mono">{{ order?.orderId }}</span>
        </p>
      </div>
      <div class="actions">
        <button v-if="order?.returnUrl" class="btn primary" @click="goBack">返回商户</button>
        <button class="btn secondary" @click="router.push('/cashier')">返回首页</button>
      </div>
    </div>

    <!-- 待支付（跳转到支付页） -->
    <div v-else-if="isPending" class="result-container pending">
      <div class="icon pending-icon">💳</div>
      <h2>等待支付</h2>
      <p class="message">订单尚未支付</p>
      <div class="actions">
        <button class="btn primary" @click="goToPay">继续支付</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderDetail } from '../../services/cashierApi'
import { OrderState, type CashierOrder } from '../../types/cashier'

const props = defineProps<{
  orderId: string
}>()

const router = useRouter()

const loading = ref(true)
const error = ref('')
const order = ref<CashierOrder | null>(null)

// 计算属性
const isSuccess = computed(() => order.value?.state === OrderState.SUCCESS)
const isFailed = computed(() => order.value?.state === OrderState.FAILED)
const isExpired = computed(() => order.value?.state === OrderState.EXPIRED)
const isPending = computed(() => order.value?.state === OrderState.PENDING)

// 方法
const loadOrder = async () => {
  try {
    loading.value = true
    error.value = ''
    order.value = await getOrderDetail(props.orderId)
  } catch (err: unknown) {
    const e = err as { response?: { data?: { message?: string } } }
    error.value = e.response?.data?.message || '订单不存在'
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (order.value?.returnUrl) {
    window.location.href = order.value.returnUrl
  }
}

const goToPay = () => {
  router.push({ name: 'cashier-pay', params: { orderId: props.orderId } })
}

const retry = () => {
  goToPay()
}

onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* 加载状态 */
.loading {
  text-align: center;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 结果容器 */
.result-container {
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  text-align: center;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 图标 */
.icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 40px;
  color: white;
}

.success-icon {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
}

.failed-icon {
  background: linear-gradient(135deg, #ff5252 0%, #e04848 100%);
}

.expired-icon {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
}

.pending-icon {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
}

.error-icon {
  background: linear-gradient(135deg, #ff5252 0%, #e04848 100%);
}

/* 标题 */
h2 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

/* 金额 */
.amount {
  font-size: 36px;
  font-weight: 600;
  color: #4caf50;
  margin: 0 0 24px 0;
}

/* 消息 */
.message {
  color: #666;
  font-size: 14px;
  margin: 0 0 24px 0;
}

/* 信息列表 */
.info {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.info p {
  display: flex;
  justify-content: space-between;
  margin: 0;
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

.info p:not(:last-child) {
  border-bottom: 1px solid #eee;
}

.info p span:last-child {
  color: #333;
  text-align: right;
  max-width: 60%;
  word-break: break-all;
}

.info .mono {
  font-family: monospace;
  font-size: 13px;
}

/* 按钮 */
.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn {
  width: 100%;
  padding: 14px 24px;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn.secondary {
  background: #f5f5f5;
  color: #666;
}

.btn.secondary:hover {
  background: #eee;
}

/* 不同状态的容器样式 */
.result-container.success h2 {
  color: #4caf50;
}

.result-container.failed h2 {
  color: #ff5252;
}

.result-container.expired h2 {
  color: #ff9800;
}

.result-container.pending h2 {
  color: #2196f3;
}
</style>
