<template>
  <div class="user-list">
    <!-- 操作栏 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :model="filters" inline>
          <el-form-item label="角色">
            <el-select v-model="filters.role" placeholder="全部" clearable style="width: 120px">
              <el-option label="普通用户" :value="0" />
              <el-option label="管理员" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filters.state" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
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
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="users" stripe border>
        <el-table-column prop="pid" label="商户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.state === 1 ? 'success' : 'danger'" size="small">
              {{ row.state === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEditRole(row)">
              修改角色
            </el-button>
            <el-button
              :type="row.state === 1 ? 'danger' : 'success'"
              size="small"
              link
              @click="handleToggleState(row)"
            >
              {{ row.state === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 修改角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="修改角色" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }} (PID: {{ currentUser?.pid }})</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="newRole">
            <el-radio :value="0">普通用户</el-radio>
            <el-radio :value="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { httpClient } from '../services/httpClient'

interface User {
  pid: number
  username: string
  email: string
  role: number
  state: number
}

const loading = ref(false)
const users = ref<User[]>([])

const filters = reactive({
  role: undefined as number | undefined,
  state: undefined as number | undefined
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const roleDialogVisible = ref(false)
const currentUser = ref<User | null>(null)
const newRole = ref(0)

const loadUsers = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      pageSize: pagination.size
    }

    if (filters.role !== undefined) params.role = filters.role
    if (filters.state !== undefined) params.state = filters.state

    const { data } = await httpClient.get('/api/users', { params })
    users.value = data.data?.items || []
    pagination.total = data.data?.total || 0
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadUsers()
}

const handleReset = () => {
  filters.role = undefined
  filters.state = undefined
  pagination.page = 1
  loadUsers()
}

const handleEditRole = (user: User) => {
  currentUser.value = user
  newRole.value = user.role
  roleDialogVisible.value = true
}

const handleSaveRole = async () => {
  if (!currentUser.value) return

  try {
    await httpClient.put(`/api/users/${currentUser.value.pid}/role`, {
      role: newRole.value
    })
    currentUser.value.role = newRole.value
    ElMessage.success('角色更新成功')
    roleDialogVisible.value = false
  } catch {
    ElMessage.error('角色更新失败')
  }
}

const handleToggleState = async (user: User) => {
  const action = user.state === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${user.username} 吗？`, '确认', {
      type: 'warning'
    })

    const newState = user.state === 1 ? 0 : 1
    await httpClient.put(`/api/users/${user.pid}/state`, null, {
      params: { state: newState }
    })
    user.state = newState
    ElMessage.success(`用户已${action}`)
  } catch (error: unknown) {
    if ((error as { message?: string })?.message !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar :deep(.el-form-item) {
  margin-bottom: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
