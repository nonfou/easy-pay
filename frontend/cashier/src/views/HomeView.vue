<template>
  <div class="home">
    <div class="logo">
      <svg viewBox="0 0 100 100" width="64" height="64">
        <circle cx="50" cy="50" r="45" fill="#4CAF50" />
        <text x="50" y="62" text-anchor="middle" fill="white" font-size="32" font-weight="bold">
          $
        </text>
      </svg>
    </div>
    <h1>Easy Pay</h1>
    <p class="desc">安全、便捷的支付收银台</p>

    <div class="search-box">
      <input
        v-model="orderId"
        type="text"
        placeholder="输入订单号查询"
        @keyup.enter="searchOrder"
      />
      <button :disabled="!orderId.trim()" @click="searchOrder">查询订单</button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const orderId = ref('')
const error = ref('')

const searchOrder = () => {
  const id = orderId.value.trim()
  if (!id) return

  error.value = ''
  router.push({ name: 'pay', params: { orderId: id } })
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.logo {
  margin-bottom: 16px;
}

h1 {
  color: white;
  font-size: 32px;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.desc {
  color: rgba(255, 255, 255, 0.85);
  font-size: 16px;
  margin: 0 0 32px 0;
}

.search-box {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 400px;
}

.search-box input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  transition: box-shadow 0.2s;
}

.search-box input:focus {
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3);
}

.search-box button {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  background: #4caf50;
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.1s;
}

.search-box button:hover:not(:disabled) {
  background: #45a049;
}

.search-box button:active:not(:disabled) {
  transform: scale(0.98);
}

.search-box button:disabled {
  background: #9e9e9e;
  cursor: not-allowed;
}

.error {
  margin-top: 16px;
  padding: 12px 20px;
  background: rgba(255, 82, 82, 0.9);
  color: white;
  border-radius: 8px;
  font-size: 14px;
}
</style>
