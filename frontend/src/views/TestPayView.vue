<template>
  <div class="test-pay-view">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span>测试支付</span>
          <el-tag type="warning" size="small">仅开发环境使用</el-tag>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <!-- 商户信息 -->
        <el-divider content-position="left">商户信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商户ID (pid)" prop="pid">
              <el-input v-model.number="form.pid" placeholder="输入商户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商户密钥" prop="secretKey">
              <el-input
                v-model="form.secretKey"
                type="password"
                show-password
                placeholder="输入商户密钥用于签名"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 订单信息 -->
        <el-divider content-position="left">订单信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支付类型" prop="type">
              <el-select v-model="form.type" placeholder="选择支付类型" style="width: 100%">
                <el-option label="微信支付" value="wxpay" />
                <el-option label="支付宝" value="alipay" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商户订单号" prop="outTradeNo">
              <el-input v-model="form.outTradeNo" placeholder="商户系统订单号">
                <template #append>
                  <el-button @click="generateOrderNo">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付金额" prop="money">
              <el-input-number
                v-model="form.money"
                :min="0.01"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 回调地址 -->
        <el-divider content-position="left">回调地址</el-divider>

        <el-form-item label="异步通知URL" prop="notifyUrl">
          <el-input v-model="form.notifyUrl" placeholder="支付成功后的异步通知地址" />
        </el-form-item>

        <el-form-item label="同步返回URL" prop="returnUrl">
          <el-input v-model="form.returnUrl" placeholder="支付完成后的跳转地址（可选）" />
        </el-form-item>

        <!-- 其他信息 -->
        <el-divider content-position="left">其他信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户端IP">
              <el-input v-model="form.clientIp" placeholder="127.0.0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型">
              <el-select v-model="form.device" style="width: 100%">
                <el-option label="PC端" value="pc" />
                <el-option label="手机端" value="mobile" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 签名预览 -->
        <el-divider content-position="left">签名信息</el-divider>

        <el-form-item label="签名字符串">
          <el-input
            :model-value="signString"
            type="textarea"
            :rows="3"
            readonly
            placeholder="填写表单后自动生成"
          />
        </el-form-item>

        <el-form-item label="签名结果 (MD5)">
          <el-input :model-value="signResult" readonly placeholder="自动计算">
            <template #append>
              <el-button :disabled="!signResult" @click="copySign">复制</el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitOrder">
            提交订单
          </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button type="info" @click="fillDemo">填充示例</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单结果 -->
    <el-card v-if="orderResult" class="result-card">
      <template #header>
        <div class="card-header">
          <span>订单创建结果</span>
          <el-tag type="success">创建成功</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统订单号">
          {{ orderResult.orderId }}
        </el-descriptions-item>
        <el-descriptions-item label="收银台地址">
          <el-link :href="cashierFullUrl" target="_blank" type="primary">
            {{ orderResult.cashierUrl }}
          </el-link>
        </el-descriptions-item>
      </el-descriptions>

      <div class="result-actions">
        <el-button type="primary" @click="openCashier"> 打开收银台 </el-button>
        <el-button @click="copyOrderId">复制订单号</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import MD5 from 'crypto-js/md5'
import { httpClient } from '../services/httpClient'

interface OrderForm {
  pid: number | undefined
  secretKey: string
  type: string
  outTradeNo: string
  name: string
  money: number
  notifyUrl: string
  returnUrl: string
  clientIp: string
  device: string
}

interface OrderResult {
  orderId: string
  cashierUrl: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const orderResult = ref<OrderResult | null>(null)

const form = ref<OrderForm>({
  pid: undefined,
  secretKey: '',
  type: 'wxpay',
  outTradeNo: '',
  name: '',
  money: 1.0,
  notifyUrl: '',
  returnUrl: '',
  clientIp: '127.0.0.1',
  device: 'pc'
})

const rules: FormRules = {
  pid: [{ required: true, message: '请输入商户ID', trigger: 'blur' }],
  secretKey: [{ required: true, message: '请输入商户密钥', trigger: 'blur' }],
  type: [{ required: true, message: '请选择支付类型', trigger: 'change' }],
  outTradeNo: [{ required: true, message: '请输入商户订单号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  money: [{ required: true, message: '请输入支付金额', trigger: 'blur' }],
  notifyUrl: [
    { required: true, message: '请输入异步通知URL', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ]
}

// 生成签名字符串
const signString = computed(() => {
  if (!form.value.pid || !form.value.secretKey) return ''

  const params: Record<string, any> = {
    clientip: form.value.clientIp,
    device: form.value.device,
    money: form.value.money,
    name: form.value.name,
    notify_url: form.value.notifyUrl,
    out_trade_no: form.value.outTradeNo,
    pid: form.value.pid,
    type: form.value.type
  }

  if (form.value.returnUrl) {
    params['return_url'] = form.value.returnUrl
  }

  // 按字典序排列
  const sorted = Object.keys(params).sort()
  const pairs = sorted
    .filter((key) => params[key] != null && params[key] !== '')
    .map((key) => `${key}=${params[key]}`)

  return pairs.join('&') + form.value.secretKey
})

// 计算签名
const signResult = computed(() => {
  if (!signString.value) return ''
  return MD5(signString.value).toString()
})

// 收银台完整地址
const cashierFullUrl = computed(() => {
  if (!orderResult.value) return ''
  const cashierBase = import.meta.env.VITE_CASHIER_BASE || 'http://localhost:5174'
  return `${cashierBase}/pay/${orderResult.value.orderId}`
})

// 生成订单号
const generateOrderNo = () => {
  const timestamp = Date.now().toString()
  const random = Math.random().toString(36).substring(2, 8).toUpperCase()
  form.value.outTradeNo = `TEST${timestamp}${random}`
}

// 复制签名
const copySign = async () => {
  if (signResult.value) {
    await navigator.clipboard.writeText(signResult.value)
    ElMessage.success('签名已复制')
  }
}

// 复制订单号
const copyOrderId = async () => {
  if (orderResult.value) {
    await navigator.clipboard.writeText(orderResult.value.orderId)
    ElMessage.success('订单号已复制')
  }
}

// 打开收银台
const openCashier = () => {
  if (orderResult.value) {
    window.open(cashierFullUrl.value, '_blank')
  }
}

// 提交订单
const submitOrder = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    submitting.value = true

    const requestData = {
      pid: form.value.pid,
      type: form.value.type,
      outTradeNo: form.value.outTradeNo,
      name: form.value.name,
      money: form.value.money,
      notifyUrl: form.value.notifyUrl,
      returnUrl: form.value.returnUrl || undefined,
      clientIp: form.value.clientIp,
      device: form.value.device,
      sign: signResult.value,
      signType: 'MD5'
    }

    const response = await httpClient.post('/api/public/orders', requestData)
    orderResult.value = response.data.data
    ElMessage.success('订单创建成功')
  } catch (error: any) {
    const msg = error.response?.data?.msg || error.message || '订单创建失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  orderResult.value = null
}

// 填充示例数据
const fillDemo = () => {
  form.value.pid = 1001
  form.value.secretKey = 'your_secret_key_here'
  form.value.type = 'wxpay'
  form.value.name = '测试商品'
  form.value.money = 0.01
  form.value.notifyUrl = 'http://localhost:8080/api/test/notify'
  form.value.returnUrl = 'http://localhost:5173/'
  form.value.clientIp = '127.0.0.1'
  form.value.device = 'pc'
  generateOrderNo()
}
</script>

<style scoped>
.test-pay-view {
  max-width: 900px;
  margin: 0 auto;
}

.form-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-card {
  margin-top: 20px;
}

.result-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

:deep(.el-divider__text) {
  font-weight: 500;
  color: #606266;
}
</style>
