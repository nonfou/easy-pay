<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">今日收入</div>
              <div class="stat-value">¥ {{ formatMoney(revenue.todayRevenue) }}</div>
            </div>
            <div class="stat-icon" style="background: #1890ff">
              <el-icon :size="28"><Wallet /></el-icon>
            </div>
          </div>
          <div class="stat-footer">
            总收入: ¥ {{ formatMoney(revenue.totalRevenue) }}
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">今日订单</div>
              <div class="stat-value">{{ revenue.todayOrderCount }}</div>
            </div>
            <div class="stat-icon" style="background: #52c41a">
              <el-icon :size="28"><Document /></el-icon>
            </div>
          </div>
          <div class="stat-footer">
            总订单: {{ revenue.orderCount }}
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">成功率</div>
              <div class="stat-value">{{ revenue.successRate }}%</div>
            </div>
            <div class="stat-icon" style="background: #722ed1">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
          </div>
          <div class="stat-footer">
            <el-progress :percentage="revenue.successRate" :show-text="false" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">活跃账号</div>
              <div class="stat-value">{{ activeAccounts }}</div>
            </div>
            <div class="stat-icon" style="background: #fa8c16">
              <el-icon :size="28"><CreditCard /></el-icon>
            </div>
          </div>
          <div class="stat-footer">
            收款账号数量
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单趋势 (近7天)</span>
              <el-button text @click="loadTrend">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="trend-chart">
            <el-table :data="trend" stripe>
              <el-table-column prop="date" label="日期" />
              <el-table-column prop="count" label="订单数" />
              <el-table-column label="金额">
                <template #default="{ row }">
                  ¥ {{ formatMoney(row.amount) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>支付类型分布</span>
            </div>
          </template>
          <div class="type-list">
            <div
              v-for="item in paymentTypes"
              :key="item.type"
              class="type-item"
            >
              <div class="type-info">
                <el-tag :type="getTagType(item.type)" size="small">
                  {{ getTypeName(item.type) }}
                </el-tag>
                <span class="type-count">{{ item.count }} 笔</span>
              </div>
              <el-progress
                :percentage="item.percentage"
                :stroke-width="8"
                :show-text="false"
              />
            </div>
            <el-empty v-if="paymentTypes.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { httpClient } from '../services/httpClient'
import type { RevenueStatistics, PaymentTypeStatistics, OrderTrend } from '../types'

const revenue = ref<RevenueStatistics>({
  totalRevenue: 0,
  todayRevenue: 0,
  orderCount: 0,
  todayOrderCount: 0,
  successRate: 0,
})

const paymentTypes = ref<PaymentTypeStatistics[]>([])
const trend = ref<OrderTrend[]>([])
const activeAccounts = ref(0)

const formatMoney = (value: number) => {
  return (value / 100).toFixed(2)
}

const getTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    wxpay: '微信支付',
    alipay: '支付宝',
    bankcard: '银行卡',
  }
  return typeMap[type] || type
}

const getTagType = (type: string) => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
    wxpay: 'success',
    alipay: 'primary',
    bankcard: 'warning',
  }
  return typeMap[type] || 'info'
}

const loadRevenue = async () => {
  try {
    const { data } = await httpClient.get('/api/console/statistics/revenue')
    revenue.value = data.data
  } catch {
    ElMessage.error('加载收入统计失败')
  }
}

const loadPaymentTypes = async () => {
  try {
    const { data } = await httpClient.get('/api/console/statistics/payment-types')
    paymentTypes.value = data.data || []
  } catch {
    ElMessage.error('加载支付类型统计失败')
  }
}

const loadTrend = async () => {
  try {
    const { data } = await httpClient.get('/api/console/statistics/trend', {
      params: { days: 7 },
    })
    trend.value = data.data || []
  } catch {
    ElMessage.error('加载订单趋势失败')
  }
}

const loadActiveAccounts = async () => {
  try {
    const { data } = await httpClient.get('/api/accounts', {
      params: { state: 1, page: 0, size: 1 },
    })
    activeAccounts.value = data.data?.totalElements || 0
  } catch {
    // 忽略错误
  }
}

onMounted(() => {
  loadRevenue()
  loadPaymentTypes()
  loadTrend()
  loadActiveAccounts()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-info {
  flex: 1;
}

.stat-title {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  color: #999;
  font-size: 13px;
}

.chart-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-chart {
  min-height: 300px;
}

.type-list {
  min-height: 300px;
}

.type-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.type-item:last-child {
  border-bottom: none;
}

.type-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.type-count {
  color: #666;
  font-size: 13px;
}
</style>
