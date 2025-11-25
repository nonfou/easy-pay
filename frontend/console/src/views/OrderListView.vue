<template>
  <div class="order-list">
    <!-- 搜索筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="订单号">
          <el-input
            v-model="filters.orderId"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="商户订单号">
          <el-input
            v-model="filters.outTradeNo"
            placeholder="请输入商户订单号"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="支付类型">
          <el-select
            v-model="filters.type"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="微信支付" value="wxpay" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银行卡" value="bankcard" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="filters.state"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 快捷筛选 -->
    <el-card shadow="never" class="quick-filter">
      <el-radio-group v-model="quickFilter" @change="handleQuickFilter">
        <el-radio-button value="all">全部订单</el-radio-button>
        <el-radio-button value="active">活跃订单</el-radio-button>
        <el-radio-button value="success">成交订单</el-radio-button>
        <el-radio-button value="expired">超时订单</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 订单表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="orders"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="orderId" label="订单号" width="180" />
        <el-table-column prop="outTradeNo" label="商户订单号" width="160" />
        <el-table-column label="支付类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.type)" size="small">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">
            ¥ {{ formatMoney(row.money) }}
          </template>
        </el-table-column>
        <el-table-column label="实际金额" width="100" align="right">
          <template #default="{ row }">
            ¥ {{ formatMoney(row.reallyPrice) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.state === 1 ? 'success' : 'warning'" size="small">
              {{ row.state === 1 ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="payTime" label="支付时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.state === 0"
              type="primary"
              size="small"
              link
              @click="handleSettle(row)"
            >
              补单
            </el-button>
            <el-button
              v-if="row.state === 1"
              type="success"
              size="small"
              link
              @click="handleRenotify(row)"
            >
              重新通知
            </el-button>
            <el-button
              type="info"
              size="small"
              link
              @click="handleDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOrders"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单号">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ currentOrder.outTradeNo }}</el-descriptions-item>
        <el-descriptions-item label="支付类型">{{ getTypeName(currentOrder.type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentOrder.state === 1 ? 'success' : 'warning'" size="small">
            {{ currentOrder.state === 1 ? '已支付' : '待支付' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单金额">¥ {{ formatMoney(currentOrder.money) }}</el-descriptions-item>
        <el-descriptions-item label="实际金额">¥ {{ formatMoney(currentOrder.reallyPrice) }}</el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ currentOrder.name }}</el-descriptions-item>
        <el-descriptions-item label="回调地址" :span="2">{{ currentOrder.notifyUrl }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端IP">{{ currentOrder.clientip }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ currentOrder.device }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { httpClient } from '../services/httpClient'
import type { Order } from '../types'

const loading = ref(false)
const orders = ref<Order[]>([])
const quickFilter = ref('all')
const detailVisible = ref(false)
const currentOrder = ref<Order | null>(null)

const filters = reactive({
  orderId: '',
  outTradeNo: '',
  type: '',
  state: undefined as number | undefined,
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

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

const loadOrders = async () => {
  loading.value = true
  try {
    let url = '/api/console/orders'

    // 根据快捷筛选选择不同的 API
    if (quickFilter.value === 'active') {
      url = '/api/console/orders/active'
    } else if (quickFilter.value === 'success') {
      url = '/api/console/orders/success'
    } else if (quickFilter.value === 'expired') {
      url = '/api/console/orders/expired'
    }

    const params: Record<string, unknown> = {
      page: pagination.page - 1,
      size: pagination.size,
    }

    // 添加筛选条件
    if (filters.orderId) params.orderId = filters.orderId
    if (filters.outTradeNo) params.outTradeNo = filters.outTradeNo
    if (filters.type) params.type = filters.type
    if (filters.state !== undefined) params.state = filters.state

    const { data } = await httpClient.get(url, { params })
    orders.value = data.data?.content || []
    pagination.total = data.data?.totalElements || 0
  } catch {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadOrders()
}

const handleReset = () => {
  filters.orderId = ''
  filters.outTradeNo = ''
  filters.type = ''
  filters.state = undefined
  quickFilter.value = 'all'
  pagination.page = 1
  loadOrders()
}

const handleQuickFilter = () => {
  pagination.page = 1
  loadOrders()
}

const handleSettle = async (order: Order) => {
  try {
    await ElMessageBox.confirm(
      `确定要对订单 ${order.orderId} 进行手动补单吗？`,
      '补单确认',
      { type: 'warning' }
    )

    await httpClient.post(`/api/admin/orders/${order.id}/settle`, {
      platformOrder: `MANUAL_${Date.now()}`,
    })

    ElMessage.success('补单成功')
    loadOrders()
  } catch (error: unknown) {
    if ((error as { message?: string })?.message !== 'cancel') {
      ElMessage.error('补单失败')
    }
  }
}

const handleRenotify = async (order: Order) => {
  try {
    await ElMessageBox.confirm(
      `确定要重新通知订单 ${order.orderId} 吗？`,
      '重新通知确认',
      { type: 'info' }
    )

    await httpClient.post(`/api/admin/orders/${order.id}/renotify`)
    ElMessage.success('通知已发送')
  } catch (error: unknown) {
    if ((error as { message?: string })?.message !== 'cancel') {
      ElMessage.error('通知发送失败')
    }
  }
}

const handleDetail = (order: Order) => {
  currentOrder.value = order
  detailVisible.value = true
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.quick-filter {
  padding: 8px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
