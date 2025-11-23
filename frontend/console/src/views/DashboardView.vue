<template>
  <section class="dashboard">
    <h1>Console Dashboard</h1>
    <p>API base: {{ apiBase }}</p>
    <pre>{{ status }}</pre>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { httpClient } from '../services/httpClient'

const status = ref('loading...')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

onMounted(async () => {
  try {
    const { data } = await httpClient.get('/api/_internal/ping')
    status.value = JSON.stringify(data, null, 2)
  } catch (error) {
    status.value = `failed to call gateway: ${error}`
  }
})
</script>

<style scoped>
.dashboard {
  padding: 2rem;
}
</style>
