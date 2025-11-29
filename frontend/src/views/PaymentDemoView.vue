<template>
  <div class="payment-demo">
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>支付网关演示</span>
          <el-button type="text" @click="$router.push('/refund')">退款测试</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="100px" @submit.prevent="createPayment">
        <el-form-item label="支付方式">
          <el-radio-group v-model="form.type">
            <el-radio value="alipay_qrcode">支付宝扫码</el-radio>
            <el-radio value="alipay_pc">支付宝PC</el-radio>
            <el-radio value="alipay_h5">支付宝H5</el-radio>
            <el-radio value="wxpay_qrcode">微信扫码</el-radio>
            <el-radio value="wxpay_h5">微信H5</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="商品名称">
          <el-input v-model="form.subject" placeholder="请输入商品名称" />
        </el-form-item>

        <el-form-item label="支付金额">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="10000"
            :precision="2"
            :step="1"
          />
          <span style="margin-left: 8px">元</span>
        </el-form-item>

        <el-form-item label="商户订单号">
          <el-input v-model="form.outTradeNo" placeholder="留空则自动生成" />
          <el-button type="text" @click="generateOutTradeNo">自动生成</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="createPayment">
            发起支付
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 支付结果 -->
      <div v-if="paymentResult" class="payment-result">
        <el-divider>支付信息</el-divider>

        <!-- 二维码展示 -->
        <div v-if="paymentResult.qrCode" class="qrcode-section">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单号">{{ form.outTradeNo }}</el-descriptions-item>
            <el-descriptions-item label="二维码链接">
              <el-link type="primary" :href="paymentResult.qrCode" target="_blank">
                {{ paymentResult.qrCode }}
              </el-link>
            </el-descriptions-item>
          </el-descriptions>
          <div class="qrcode-image" v-if="qrCodeDataUrl">
            <img :src="qrCodeDataUrl" alt="支付二维码" />
            <p>请使用{{ form.type.startsWith('alipay') ? '支付宝' : '微信' }}扫码支付</p>
          </div>
        </div>

        <!-- H5/PC 表单展示 -->
        <div v-else-if="paymentResult.htmlForm" class="form-section">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单号">{{ form.outTradeNo }}</el-descriptions-item>
            <el-descriptions-item label="支付表单">已生成</el-descriptions-item>
          </el-descriptions>
          <div class="action-buttons">
            <el-button type="primary" @click="submitPayForm">跳转支付</el-button>
          </div>
          <div ref="payFormContainer" v-html="paymentResult.htmlForm" style="display: none;"></div>
        </div>
      </div>
    </el-card>

    <!-- 支付配置状态 -->
    <el-card class="config-status">
      <template #header>
        <span>支付配置状态</span>
      </template>
      <el-descriptions :column="2">
        <el-descriptions-item label="支付宝">
          <el-tag :type="paymentStatus.alipayConfigured ? 'success' : 'info'">
            {{ paymentStatus.alipayConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="微信支付">
          <el-tag :type="paymentStatus.wxpayConfigured ? 'success' : 'info'">
            {{ paymentStatus.wxpayConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="!paymentStatus.alipayConfigured && !paymentStatus.wxpayConfigured"
        type="warning"
        title="支付未配置"
        description="请在 application.yml 中配置支付宝或微信支付参数"
        :closable="false"
        style="margin-top: 16px"
      />
    </el-card>

    <!-- WebSocket 连接状态 -->
    <el-card class="ws-status" v-if="wsConnected">
      <template #header>
        <span>实时通知</span>
      </template>
      <el-tag type="success">WebSocket 已连接</el-tag>
      <p v-if="wsMessage" style="margin-top: 10px;">
        最新消息: {{ wsMessage }}
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import httpClient from '../services/httpClient'
import QRCode from 'qrcode'

interface PaymentResult {
  qrCode?: string
  htmlForm?: string
}

interface PaymentStatus {
  alipayConfigured: boolean
  wxpayConfigured: boolean
}

const loading = ref(false)
const paymentResult = ref<PaymentResult | null>(null)
const qrCodeDataUrl = ref<string | null>(null)
const payFormContainer = ref<HTMLElement | null>(null)
const paymentStatus = reactive<PaymentStatus>({
  alipayConfigured: false,
  wxpayConfigured: false
})

// WebSocket 相关
const wsConnected = ref(false)
const wsMessage = ref<string | null>(null)
let ws: WebSocket | null = null

const form = reactive({
  type: 'alipay_qrcode',
  subject: '测试商品',
  amount: 0.01,
  outTradeNo: ''
})

const generateOutTradeNo = () => {
  const timestamp = Date.now().toString()
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  form.outTradeNo = `TEST${timestamp}${random}`
}

const resetForm = () => {
  form.type = 'alipay_qrcode'
  form.subject = '测试商品'
  form.amount = 0.01
  form.outTradeNo = ''
  paymentResult.value = null
  qrCodeDataUrl.value = null
  disconnectWebSocket()
}

const createPayment = async () => {
  if (!form.outTradeNo) {
    generateOutTradeNo()
  }

  loading.value = true
  paymentResult.value = null
  qrCodeDataUrl.value = null

  try {
    let endpoint = ''
    let requestBody: Record<string, unknown> = {}
    const totalAmountCents = Math.round(form.amount * 100)

    switch (form.type) {
      case 'alipay_qrcode':
        endpoint = '/api/payment/alipay/qrcode'
        requestBody = {
          outTradeNo: form.outTradeNo,
          subject: form.subject,
          totalAmount: form.amount.toFixed(2)
        }
        break
      case 'alipay_pc':
        endpoint = '/api/payment/alipay/pc'
        requestBody = {
          outTradeNo: form.outTradeNo,
          subject: form.subject,
          totalAmount: form.amount.toFixed(2),
          returnUrl: window.location.origin + '/result'
        }
        break
      case 'alipay_h5':
        endpoint = '/api/payment/alipay/h5'
        requestBody = {
          outTradeNo: form.outTradeNo,
          subject: form.subject,
          totalAmount: form.amount.toFixed(2),
          returnUrl: window.location.origin + '/result',
          quitUrl: window.location.origin
        }
        break
      case 'wxpay_qrcode':
        endpoint = '/api/payment/wxpay/qrcode'
        requestBody = {
          outTradeNo: form.outTradeNo,
          body: form.subject,
          totalFee: totalAmountCents
        }
        break
      case 'wxpay_h5':
        endpoint = '/api/payment/wxpay/h5'
        requestBody = {
          outTradeNo: form.outTradeNo,
          body: form.subject,
          totalFee: totalAmountCents
        }
        break
    }

    const response = await httpClient.post<{ code: number; msg: string; data: any }>(endpoint, requestBody)

    if (response.data.code === 0) {
      const data = response.data.data

      if (form.type === 'alipay_qrcode' || form.type === 'wxpay_qrcode') {
        const qrCodeUrl = data.qrCode || data.codeUrl
        paymentResult.value = { qrCode: qrCodeUrl }
        // 生成二维码图片
        if (qrCodeUrl) {
          qrCodeDataUrl.value = await QRCode.toDataURL(qrCodeUrl, { width: 200 })
        }
        // 连接 WebSocket 监听支付结果
        connectWebSocket(form.outTradeNo)
      } else {
        // H5 或 PC 支付返回 HTML 表单
        paymentResult.value = { htmlForm: data }
      }

      ElMessage.success('支付创建成功')
    } else {
      ElMessage.error(response.data.msg || '支付创建失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '支付创建失败')
  } finally {
    loading.value = false
  }
}

const submitPayForm = () => {
  nextTick(() => {
    if (payFormContainer.value) {
      const form = payFormContainer.value.querySelector('form')
      if (form) {
        form.submit()
      }
    }
  })
}

const connectWebSocket = (orderId: string) => {
  disconnectWebSocket()

  const wsUrl = `ws://${window.location.host}/ws/payment/${orderId}`
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    wsConnected.value = true
    console.log('WebSocket 已连接')
  }

  ws.onmessage = (event) => {
    wsMessage.value = event.data
    const data = JSON.parse(event.data)
    if (data.type === 'PAYMENT_SUCCESS') {
      ElMessage.success(`支付成功！交易号: ${data.tradeNo}`)
    } else if (data.type === 'PAYMENT_FAILED') {
      ElMessage.error(`支付失败: ${data.reason}`)
    }
  }

  ws.onclose = () => {
    wsConnected.value = false
    console.log('WebSocket 已断开')
  }

  ws.onerror = (error) => {
    console.error('WebSocket 错误:', error)
  }
}

const disconnectWebSocket = () => {
  if (ws) {
    ws.close()
    ws = null
    wsConnected.value = false
    wsMessage.value = null
  }
}

const checkPaymentStatus = async () => {
  try {
    const response = await httpClient.get<{ code: number; data: PaymentStatus }>('/api/payment/status')
    if (response.data.code === 0) {
      Object.assign(paymentStatus, response.data.data)
    }
  } catch {
    paymentStatus.alipayConfigured = false
    paymentStatus.wxpayConfigured = false
  }
}

onMounted(() => {
  generateOutTradeNo()
  checkPaymentStatus()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.payment-demo {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.demo-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 500;
}

.payment-result {
  margin-top: 20px;
}

.qrcode-section {
  text-align: center;
}

.qrcode-image {
  margin-top: 20px;
}

.qrcode-image img {
  border: 1px solid #eee;
  padding: 10px;
  border-radius: 8px;
}

.action-buttons {
  margin-top: 16px;
  text-align: center;
}

.config-status {
  margin-top: 20px;
}

.ws-status {
  margin-top: 20px;
}
</style>
