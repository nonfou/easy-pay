<template>
  <section>
    <h2>Orders</h2>
    <button @click="load">Refresh</button>
    <table v-if="orders.length">
      <thead>
        <tr>
          <th>Order ID</th>
          <th>Type</th>
          <th>Amount</th>
          <th>State</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.orderId">
          <td>{{ order.orderId }}</td>
          <td>{{ order.type }}</td>
          <td>{{ order.money }}</td>
          <td>{{ order.state }}</td>
        </tr>
      </tbody>
    </table>
    <p v-else>No orders loaded.</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { httpClient } from '../services/httpClient'

type Order = {
  orderId: string
  type: string
  money: number
  state: number
}

const orders = ref<Order[]>([])

const load = async () => {
  const { data } = await httpClient.get('/api/Order/getOrders', {
    params: { page: 1, limit: 10 },
  })
  orders.value = data.data ?? []
}

onMounted(load)
</script>
