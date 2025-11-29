<template>
  <div class="refund-demo">
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>退款测试</span>
          <el-button type="text" @click="$router.push('/')">返回支付</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="120px" @submit.prevent="submitRefund">
        <el-form-item label="支付方式">
          <el-radio-group v-model="form.paymentType" @change="handlePaymentTypeChange">
            <el-radio value="alipay">支付宝</el-radio>
            <el-radio value="wxpay">微信支付</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="订单号类型">
          <el-radio-group v-model="form.orderNoType">
            <el-radio value="outTradeNo">商户订单号</el-radio>
            <el-radio value="tradeNo">平台交易号</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="form.orderNoType === 'outTradeNo' ? '商户订单号' : '平台交易号'">
          <el-input
            v-model="form.orderNo"
            :placeholder="form.orderNoType === 'outTradeNo' ? '请输入商户订单号' : '请输入平台交易号'"
            style="width: 280px"
          />
          <el-button type="primary" :loading="queryLoading" @click="queryOrder" style="margin-left: 10px">
            查询订单
          </el-button>
        </el-form-item>

        <!-- 订单信息展示 -->
        <div v-if="orderInfo" class="order-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单状态">
              <el-tag :type="getStatusTagType(orderInfo.tradeStatus)">
                {{ getStatusText(orderInfo.tradeStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              {{ orderInfo.totalAmount }} 元
            </el-descriptions-item>
            <el-descriptions-item label="商户订单号">
              {{ orderInfo.outTradeNo }}
            </el-descriptions-item>
            <el-descriptions-item label="平台交易号">
              {{ orderInfo.tradeNo }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-form-item label="退款金额">
          <el-input-number
            v-model="form.refundAmount"
            :min="0.01"
            :max="orderInfo ? parseFloat(orderInfo.totalAmount) : 100000"
            :precision="2"
            :step="1"
          />
          <span style="margin-left: 8px">元</span>
          <el-button
            v-if="orderInfo"
            type="text"
            @click="setFullRefund"
            style="margin-left: 10px"
          >
            全额退款
          </el-button>
        </el-form-item>

        <el-form-item label="退款原因">
          <el-input
            v-model="form.refundReason"
            type="textarea"
            :rows="2"
            placeholder="请输入退款原因（可选）"
          />
        </el-form-item>

        <el-form-item label="退款请求号" v-if="form.paymentType === 'alipay'">
          <el-input v-model="form.outRequestNo" placeholder="部分退款时必填，留空则自动生成" />
          <el-button type="text" @click="generateOutRequestNo">自动生成</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitRefund">
            发起退款
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 退款结果 -->
      <div v-if="refundResult" class="refund-result">
        <el-divider>退款结果</el-divider>

        <el-result
          :icon="refundResult.success ? 'success' : 'error'"
          :title="refundResult.success ? '退款成功' : '退款失败'"
          :sub-title="refundResult.message"
        >
          <template #extra v-if="refundResult.success">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="交易号">
                {{ refundResult.tradeNo }}
              </el-descriptions-item>
              <el-descriptions-item label="商户订单号">
                {{ refundResult.outTradeNo }}
              </el-descriptions-item>
              <el-descriptions-item label="退款金额">
                {{ refundResult.refundFee }} 元
              </el-descriptions-item>
              <el-descriptions-item label="买家ID" v-if="refundResult.buyerUserId">
                {{ refundResult.buyerUserId }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-result>
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

    <!-- 退款说明 -->
    <el-card class="help-card">
      <template #header>
        <span>退款说明</span>
      </template>
      <el-collapse>
        <el-collapse-item title="支付宝退款" name="alipay">
          <ul>
            <li>支持商户订单号(outTradeNo)或支付宝交易号(tradeNo)进行退款</li>
            <li>支持全额退款和部分退款</li>
            <li>部分退款时，退款请求号(outRequestNo)为必填</li>
            <li>同一订单可多次部分退款，但总退款金额不能超过原订单金额</li>
          </ul>
        </el-collapse-item>
        <el-collapse-item title="微信退款" name="wxpay">
          <ul>
            <li>支持商户订单号(outTradeNo)或微信支付订单号(transactionId)进行退款</li>
            <li>支持全额退款和部分退款</li>
            <li>退款请求号(outRefundNo)会自动生成</li>
            <li>微信退款为异步处理，结果会通过回调通知</li>
          </ul>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import httpClient from '../services/httpClient'

interface OrderInfo {
  outTradeNo: string
  tradeNo: string
  tradeStatus: string
  totalAmount: string
}

interface RefundResult {
  success: boolean
  message: string
  tradeNo?: string
  outTradeNo?: string
  refundFee?: string
  buyerUserId?: string
}

interface PaymentStatus {
  alipayConfigured: boolean
  wxpayConfigured: boolean
}

const loading = ref(false)
const queryLoading = ref(false)
const refundResult = ref<RefundResult | null>(null)
const orderInfo = ref<OrderInfo | null>(null)
const paymentStatus = reactive<PaymentStatus>({
  alipayConfigured: false,
  wxpayConfigured: false
})

const form = reactive({
  paymentType: 'alipay',
  orderNoType: 'outTradeNo',
  orderNo: '',
  refundAmount: 0.01,
  refundReason: '',
  outRequestNo: ''
})

const handlePaymentTypeChange = () => {
  orderInfo.value = null
  refundResult.value = null
}

const generateOutRequestNo = () => {
  const timestamp = Date.now().toString()
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  form.outRequestNo = `REFUND${timestamp}${random}`
}

const resetForm = () => {
  form.paymentType = 'alipay'
  form.orderNoType = 'outTradeNo'
  form.orderNo = ''
  form.refundAmount = 0.01
  form.refundReason = ''
  form.outRequestNo = ''
  refundResult.value = null
  orderInfo.value = null
}

const setFullRefund = () => {
  if (orderInfo.value) {
    form.refundAmount = parseFloat(orderInfo.value.totalAmount)
  }
}

const getStatusTagType = (status: string) => {
  const statusMap: Record<string, string> = {
    TRADE_SUCCESS: 'success',
    SUCCESS: 'success',
    TRADE_FINISHED: 'success',
    WAIT_BUYER_PAY: 'warning',
    NOTPAY: 'warning',
    USERPAYING: 'warning',
    TRADE_CLOSED: 'info',
    CLOSED: 'info',
    REFUND: 'primary',
    PAYERROR: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    // 支付宝状态
    TRADE_SUCCESS: '支付成功',
    TRADE_FINISHED: '交易完成',
    WAIT_BUYER_PAY: '等待付款',
    TRADE_CLOSED: '交易关闭',
    // 微信状态
    SUCCESS: '支付成功',
    NOTPAY: '未支付',
    USERPAYING: '支付中',
    CLOSED: '已关闭',
    REFUND: '已退款',
    PAYERROR: '支付失败',
    REVOKED: '已撤销'
  }
  return statusMap[status] || status
}

const queryOrder = async () => {
  if (!form.orderNo) {
    ElMessage.warning('请输入订单号')
    return
  }

  queryLoading.value = true
  orderInfo.value = null

  try {
    let endpoint = ''
    let requestBody: Record<string, unknown> = {}

    if (form.paymentType === 'alipay') {
      endpoint = '/api/payment/alipay/query'
      if (form.orderNoType === 'outTradeNo') {
        requestBody.outTradeNo = form.orderNo
      } else {
        requestBody.tradeNo = form.orderNo
      }
    } else {
      endpoint = '/api/payment/wxpay/query'
      if (form.orderNoType === 'outTradeNo') {
        requestBody.outTradeNo = form.orderNo
      } else {
        requestBody.transactionId = form.orderNo
      }
    }

    const response = await httpClient.post<{ code: number; msg: string; data: any }>(endpoint, requestBody)

    if (response.data.code === 0) {
      const data = response.data.data

      if (form.paymentType === 'alipay') {
        orderInfo.value = {
          outTradeNo: data.outTradeNo,
          tradeNo: data.tradeNo,
          tradeStatus: data.tradeStatus,
          totalAmount: data.totalAmount || '0'
        }
        // 默认设置为全额退款
        form.refundAmount = parseFloat(data.totalAmount || '0')
      } else {
        // 微信支付金额是分，需要转换为元
        const totalAmount = data.totalFee ? (data.totalFee / 100).toFixed(2) : '0'
        orderInfo.value = {
          outTradeNo: data.outTradeNo,
          tradeNo: data.transactionId,
          tradeStatus: data.tradeState,
          totalAmount: totalAmount
        }
        // 默认设置为全额退款
        form.refundAmount = parseFloat(totalAmount)
      }

      ElMessage.success('订单查询成功')
    } else {
      ElMessage.error(response.data.msg || '订单查询失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '订单查询失败')
  } finally {
    queryLoading.value = false
  }
}

const submitRefund = async () => {
  if (!form.orderNo) {
    ElMessage.warning('请输入订单号')
    return
  }

  // 如果没有查询过订单，先查询订单
  if (!orderInfo.value) {
    await queryOrder()
    if (!orderInfo.value) {
      return
    }
  }

  loading.value = true
  refundResult.value = null

  try {
    let endpoint = ''
    let requestBody: Record<string, unknown> = {}

    if (form.paymentType === 'alipay') {
      endpoint = '/api/payment/alipay/refund'

      // 如果没有填写退款请求号，自动生成
      if (!form.outRequestNo) {
        generateOutRequestNo()
      }

      requestBody = {
        refundAmount: form.refundAmount.toFixed(2),
        refundReason: form.refundReason || '用户申请退款',
        outRequestNo: form.outRequestNo
      }

      if (form.orderNoType === 'outTradeNo') {
        requestBody.outTradeNo = form.orderNo
      } else {
        requestBody.tradeNo = form.orderNo
      }
    } else {
      // 微信退款
      endpoint = '/api/payment/wxpay/refund'
      const refundAmountYuan = form.refundAmount
      const totalAmountYuan = parseFloat(orderInfo.value.totalAmount)

      requestBody = {
        refundFee: refundAmountYuan,
        totalFee: totalAmountYuan,
        refundDesc: form.refundReason || '用户申请退款',
        outRefundNo: `REFUND${Date.now()}`
      }

      if (form.orderNoType === 'outTradeNo') {
        requestBody.outTradeNo = form.orderNo
      } else {
        requestBody.transactionId = form.orderNo
      }
    }

    const response = await httpClient.post<{ code: number; msg: string; data: any }>(endpoint, requestBody)

    if (response.data.code === 0) {
      const data = response.data.data

      if (form.paymentType === 'alipay') {
        // 支付宝退款响应处理
        if (data.code === '10000') {
          refundResult.value = {
            success: true,
            message: '退款申请已受理',
            tradeNo: data.tradeNo || orderInfo.value?.tradeNo || '',
            outTradeNo: data.outTradeNo || orderInfo.value?.outTradeNo || form.orderNo,
            refundFee: data.refundFee || form.refundAmount.toFixed(2),
            buyerUserId: data.buyerUserId
          }
          ElMessage.success('退款成功')
        } else {
          refundResult.value = {
            success: false,
            message: data.subMsg || data.msg || '退款失败'
          }
          ElMessage.error(data.subMsg || data.msg || '退款失败')
        }
      } else {
        // 微信退款响应处理
        if (data.resultCode === 'SUCCESS') {
          refundResult.value = {
            success: true,
            message: '退款申请已提交，等待处理',
            tradeNo: data.transactionId || orderInfo.value?.tradeNo || '',
            outTradeNo: data.outTradeNo || orderInfo.value?.outTradeNo || form.orderNo,
            refundFee: data.refundFee ? data.refundFee.toFixed(2) : form.refundAmount.toFixed(2)
          }
          ElMessage.success('退款申请已提交')
        } else {
          refundResult.value = {
            success: false,
            message: data.errCodeDes || data.returnMsg || '退款失败'
          }
          ElMessage.error(data.errCodeDes || data.returnMsg || '退款失败')
        }
      }
    } else {
      refundResult.value = {
        success: false,
        message: response.data.msg || '退款请求失败'
      }
      ElMessage.error(response.data.msg || '退款请求失败')
    }
  } catch (error: any) {
    refundResult.value = {
      success: false,
      message: error.response?.data?.msg || error.message || '退款请求失败'
    }
    ElMessage.error(error.response?.data?.msg || '退款请求失败')
  } finally {
    loading.value = false
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
  checkPaymentStatus()
})
</script>

<style scoped>
.refund-demo {
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

.order-info {
  margin: 16px 0;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.refund-result {
  margin-top: 20px;
}

.config-status {
  margin-top: 20px;
}

.help-card {
  margin-top: 20px;
}

.help-card ul {
  margin: 0;
  padding-left: 20px;
}

.help-card li {
  line-height: 1.8;
  color: #666;
}
</style>
