<template>
  <div class="plugin-list">
    <!-- 操作栏 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleSync" :loading="syncing">
          <el-icon><Refresh /></el-icon>
          同步插件市场
        </el-button>
        <el-button @click="loadPlugins">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 插件列表 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col
        v-for="plugin in plugins"
        :key="plugin.id"
        :xs="24"
        :sm="12"
        :lg="8"
        :xl="6"
      >
        <el-card shadow="hover" class="plugin-card">
          <template #header>
            <div class="plugin-header">
              <div class="plugin-info">
                <el-icon :size="24"><Connection /></el-icon>
                <span class="plugin-name">{{ plugin.name }}</span>
              </div>
              <el-tag
                :type="plugin.state === 1 ? 'success' : 'info'"
                size="small"
              >
                {{ plugin.state === 1 ? '已启用' : '未启用' }}
              </el-tag>
            </div>
          </template>

          <div class="plugin-body">
            <p class="plugin-desc">{{ plugin.description || '暂无描述' }}</p>
            <div class="plugin-meta">
              <span v-if="plugin.version">版本: {{ plugin.version }}</span>
              <span v-if="plugin.author">作者: {{ plugin.author }}</span>
            </div>
          </div>

          <div class="plugin-footer">
            <el-button
              :type="plugin.state === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleState(plugin)"
            >
              {{ plugin.state === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-if="plugin.downloadUrl"
              type="primary"
              size="small"
              link
              @click="handleDownload(plugin)"
            >
              下载
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && !plugins.length" description="暂无插件" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { httpClient } from '../services/httpClient'

interface Plugin {
  id: number
  name: string
  pluginKey: string
  description: string
  version: string
  author: string
  downloadUrl: string
  state: number
}

const loading = ref(false)
const syncing = ref(false)
const plugins = ref<Plugin[]>([])

const loadPlugins = async () => {
  loading.value = true
  try {
    const { data } = await httpClient.get('/api/plugins')
    plugins.value = data.data || []
  } catch {
    ElMessage.error('加载插件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSync = async () => {
  syncing.value = true
  try {
    const { data } = await httpClient.post('/api/plugins/sync')
    const result = data.data
    ElMessage.success(`同步完成：新增 ${result.added} 个，更新 ${result.updated} 个`)
    loadPlugins()
  } catch {
    ElMessage.error('同步失败')
  } finally {
    syncing.value = false
  }
}

const handleToggleState = async (plugin: Plugin) => {
  try {
    const newState = plugin.state === 1 ? 0 : 1
    await httpClient.put(`/api/plugins/${plugin.id}/state`, { state: newState })
    plugin.state = newState
    ElMessage.success('状态更新成功')
  } catch {
    ElMessage.error('状态更新失败')
  }
}

const handleDownload = (plugin: Plugin) => {
  if (plugin.downloadUrl) {
    window.open(plugin.downloadUrl, '_blank')
  }
}

onMounted(() => {
  loadPlugins()
})
</script>

<style scoped>
.plugin-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
}

.plugin-card {
  margin-bottom: 16px;
}

.plugin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plugin-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plugin-name {
  font-weight: 600;
  font-size: 15px;
}

.plugin-body {
  min-height: 80px;
}

.plugin-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 12px 0;
}

.plugin-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.plugin-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
