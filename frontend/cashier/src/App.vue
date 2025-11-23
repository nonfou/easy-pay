<template>
  <main class="cashier">
    <h1>Cashier Preview</h1>
    <form @submit.prevent="loadOrder">
      <label>
        Order ID
        <input v-model="orderId" placeholder="H2024..." />
      </label>
      <button type="submit">Load</button>
    </form>
    <section v-if="orderData">
      <p>Type: {{ orderData.type }}</p>
      <p>Amount: {{ orderData.money }}</p>
      <p>State: {{ orderState?.state ?? 'pending' }}</p>
      <p>Expire in: {{ orderState?.expireIn ?? 0 }}s</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { httpClient } from './services/httpClient'

const orderId = ref('')
const orderData = ref<any>(null)
const orderState = ref<any>(null)
let timer: ReturnType<typeof setInterval> | null = null

const loadOrder = async () => {
  if (!orderId.value) return
  const { data } = await httpClient.get(`/api/public/orders/${orderId.value}`)
  orderData.value = data
  startPolling()
}

const pollState = async () => {
  if (!orderId.value) return
  const { data } = await httpClient.get(`/api/public/orders/${orderId.value}/state`)
  orderState.value = data
  if (data.state === 1 || data.state === 2) {
    stopPolling()
  }
}

const startPolling = () => {
  stopPolling()
  timer = setInterval(pollState, 3000)
  pollState()
}

const stopPolling = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onUnmounted(stopPolling)
</script>

<style scoped>
.cashier {
  padding: 2rem;
}
</style>
