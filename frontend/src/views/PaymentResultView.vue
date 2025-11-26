<template>
  <div class="payment-result-page">
    <el-card class="result-card">
      <template #header>
        <div class="card-header">
          <span>支付结果</span>
        </div>
      </template>

      <div class="result-content">
        <!-- 支付成功 -->
        <div v-if="isSuccess" class="result-success">
          <el-icon class="result-icon success"><CircleCheck /></el-icon>
          <h2>支付成功</h2>
        </div>

        <!-- 支付失败/未知 -->
        <div v-else class="result-unknown">
          <el-icon class="result-icon warning"><Warning /></el-icon>
          <h2>支付处理中</h2>
          <p class="result-tip">请稍后查询订单状态确认支付结果</p>
        </div>

        <!-- 订单信息 -->
        <el-descriptions :column="1" border class="order-info">
          <el-descriptions-item label="商户订单号">
            {{ orderInfo.outTradeNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="支付宝交易号">
            {{ orderInfo.tradeNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="支付金额">
            {{ orderInfo.totalAmount ? `${orderInfo.totalAmount} 元` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="卖家ID" v-if="orderInfo.sellerId">
            {{ orderInfo.sellerId }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

interface OrderInfo {
  outTradeNo: string
  tradeNo: string
  totalAmount: string
  sellerId: string
}

const orderInfo = reactive<OrderInfo>({
  outTradeNo: '',
  tradeNo: '',
  totalAmount: '',
  sellerId: ''
})

// 判断是否支付成功（有交易号通常表示支付成功）
const isSuccess = computed(() => {
  return !!orderInfo.tradeNo
})

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  // 解析支付宝返回的查询参数
  const query = route.query
  orderInfo.outTradeNo = (query.out_trade_no as string) || ''
  orderInfo.tradeNo = (query.trade_no as string) || ''
  orderInfo.totalAmount = (query.total_amount as string) || ''
  orderInfo.sellerId = (query.seller_id as string) || ''
})
</script>

<style scoped>
.payment-result-page {
  padding: 40px 20px;
  max-width: 600px;
  margin: 0 auto;
}

.result-card {
  text-align: center;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.result-content {
  padding: 20px 0;
}

.result-success,
.result-unknown {
  margin-bottom: 30px;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.result-icon.success {
  color: #67c23a;
}

.result-icon.warning {
  color: #e6a23c;
}

.result-success h2,
.result-unknown h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: 500;
}

.result-tip {
  color: #909399;
  font-size: 14px;
}

.order-info {
  margin-top: 20px;
  text-align: left;
}

.action-buttons {
  margin-top: 30px;
}
</style>
