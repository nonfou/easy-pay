<template>
  <div class="account-list">
    <!-- 操作栏 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增账号
        </el-button>
        <el-button @click="loadAccounts">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 账号列表 -->
    <el-card v-loading="loading" shadow="never">
      <el-collapse v-model="activeAccounts" accordion>
        <el-collapse-item v-for="account in accounts" :key="account.id" :name="account.id">
          <template #title>
            <div class="account-header">
              <div class="account-info">
                <el-tag :type="getPlatformTagType(account.platform)" size="small">
                  {{ getPlatformName(account.platform) }}
                </el-tag>
                <span class="account-name">{{ account.account }}</span>
                <el-tag
                  :type="account.state === 1 ? 'success' : 'danger'"
                  size="small"
                  effect="plain"
                >
                  {{ account.state === 1 ? '启用' : '禁用' }}
                </el-tag>
                <el-tag
                  :type="account.pattern === 1 ? 'primary' : 'info'"
                  size="small"
                  effect="plain"
                >
                  {{ account.pattern === 1 ? '连续监听' : '单次监听' }}
                </el-tag>
              </div>
              <div class="account-actions" @click.stop>
                <el-switch
                  v-model="account.state"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleToggleState(account)"
                />
              </div>
            </div>
          </template>

          <!-- 通道列表 -->
          <div class="channel-section">
            <div class="section-header">
              <span>收款通道</span>
              <el-button type="primary" size="small" @click="handleAddChannel(account)">
                <el-icon><Plus /></el-icon>
                添加通道
              </el-button>
            </div>

            <el-table :data="account.channels" border size="small">
              <el-table-column prop="channel" label="终端编号" width="120" />
              <el-table-column label="二维码" width="100">
                <template #default="{ row }">
                  <el-image
                    v-if="row.qrcode"
                    :src="row.qrcode"
                    :preview-src-list="[row.qrcode]"
                    fit="contain"
                    style="width: 60px; height: 60px"
                  />
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="lastTime" label="最后使用" width="170" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.state === 1 ? 'success' : 'danger'" size="small">
                    {{ row.state === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button type="primary" size="small" link @click="handleUploadQrcode(row)">
                    上传二维码
                  </el-button>
                  <el-button
                    :type="row.state === 1 ? 'danger' : 'success'"
                    size="small"
                    link
                    @click="handleToggleChannelState(row)"
                  >
                    {{ row.state === 1 ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!account.channels?.length" description="暂无通道" />
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-if="!accounts.length" description="暂无账号" />
    </el-card>

    <!-- 新增/编辑账号弹窗 -->
    <el-dialog
      v-model="accountDialogVisible"
      :title="isEdit ? '编辑账号' : '新增账号'"
      width="500px"
    >
      <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="100px">
        <el-form-item label="支付平台" prop="platform">
          <el-select v-model="accountForm.platform" placeholder="请选择" style="width: 100%">
            <el-option label="微信支付" value="wxpay" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银行卡" value="bankcard" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="account">
          <el-input v-model="accountForm.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="监听模式" prop="pattern">
          <el-radio-group v-model="accountForm.pattern">
            <el-radio :value="0">单次监听</el-radio>
            <el-radio :value="1">连续监听</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAccount">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加通道弹窗 -->
    <el-dialog v-model="channelDialogVisible" title="添加通道" width="500px">
      <el-form ref="channelFormRef" :model="channelForm" :rules="channelRules" label-width="100px">
        <el-form-item label="终端编号" prop="channel">
          <el-input v-model="channelForm.channel" placeholder="请输入终端编号" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-input v-model="channelForm.type" placeholder="请输入类型，如: personal, merchant" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveChannel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传二维码弹窗 -->
    <el-dialog v-model="uploadDialogVisible" title="上传二维码" width="400px">
      <el-upload
        ref="uploadRef"
        :action="`${apiBase}/api/channels/${currentChannelId}/qrcode/upload`"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        accept="image/*"
        :show-file-list="false"
      >
        <el-button type="primary">选择图片</el-button>
        <template #tip>
          <div class="upload-tip">支持 JPG、PNG 格式，大小不超过 5MB</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { httpClient } from '../services/httpClient'
import { useAuthStore } from '../stores/auth'
import type { AccountSummary, PayChannel } from '../types'

const authStore = useAuthStore()
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const loading = ref(false)
const accounts = ref<AccountSummary[]>([])
const activeAccounts = ref<number | undefined>()

// 账号弹窗
const accountDialogVisible = ref(false)
const accountFormRef = ref<FormInstance>()
const isEdit = ref(false)
const accountForm = reactive({
  id: 0,
  platform: '',
  account: '',
  pattern: 0
})
const accountRules: FormRules = {
  platform: [{ required: true, message: '请选择支付平台', trigger: 'change' }],
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }]
}

// 通道弹窗
const channelDialogVisible = ref(false)
const channelFormRef = ref<FormInstance>()
const currentAccountId = ref(0)
const channelForm = reactive({
  channel: '',
  type: ''
})
const channelRules: FormRules = {
  channel: [{ required: true, message: '请输入终端编号', trigger: 'blur' }]
}

// 上传弹窗
const uploadDialogVisible = ref(false)
const currentChannelId = ref(0)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.accessToken}`
}))

const getPlatformName = (platform: string) => {
  const map: Record<string, string> = {
    wxpay: '微信支付',
    alipay: '支付宝',
    bankcard: '银行卡'
  }
  return map[platform] || platform
}

const getPlatformTagType = (platform: string) => {
  const map: Record<string, 'success' | 'primary' | 'warning'> = {
    wxpay: 'success',
    alipay: 'primary',
    bankcard: 'warning'
  }
  return map[platform] || 'info'
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const { data } = await httpClient.get('/api/accounts', {
      params: { page: 1, pageSize: 100 }
    })
    accounts.value = data.data?.items || []
  } catch {
    ElMessage.error('加载账号列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  accountForm.id = 0
  accountForm.platform = ''
  accountForm.account = ''
  accountForm.pattern = 0
  accountDialogVisible.value = true
}

const handleSaveAccount = async () => {
  if (!accountFormRef.value) return

  await accountFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (isEdit.value) {
        await httpClient.put(`/api/accounts/${accountForm.id}`, accountForm)
        ElMessage.success('更新成功')
      } else {
        await httpClient.post('/api/accounts', accountForm)
        ElMessage.success('创建成功')
      }
      accountDialogVisible.value = false
      loadAccounts()
    } catch {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  })
}

const handleToggleState = async (account: AccountSummary) => {
  try {
    await httpClient.post(`/api/accounts/${account.id}/state`, null, {
      params: { state: account.state }
    })
    ElMessage.success('状态更新成功')
  } catch {
    // 回滚状态
    account.state = account.state === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleAddChannel = (account: AccountSummary) => {
  currentAccountId.value = account.id
  channelForm.channel = ''
  channelForm.type = ''
  channelDialogVisible.value = true
}

const handleSaveChannel = async () => {
  if (!channelFormRef.value) return

  await channelFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await httpClient.post(`/api/accounts/${currentAccountId.value}/channels`, channelForm)
      ElMessage.success('通道添加成功')
      channelDialogVisible.value = false
      loadAccounts()
    } catch {
      ElMessage.error('通道添加失败')
    }
  })
}

const handleToggleChannelState = async (channel: PayChannel) => {
  try {
    const newState = channel.state === 1 ? 0 : 1
    await httpClient.put(`/api/channels/${channel.id}/state`, { state: newState })
    channel.state = newState
    ElMessage.success('状态更新成功')
  } catch {
    ElMessage.error('状态更新失败')
  }
}

const handleUploadQrcode = (channel: PayChannel) => {
  currentChannelId.value = channel.id
  uploadDialogVisible.value = true
}

const beforeUpload = (file: UploadRawFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
  uploadDialogVisible.value = false
  loadAccounts()
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.account-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.account-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-name {
  font-weight: 500;
}

.channel-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 500;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
</style>
