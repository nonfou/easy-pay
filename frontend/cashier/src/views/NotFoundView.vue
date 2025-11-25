<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>页面不存在</p>
      <button @click="router.push('/')">返回首页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.content {
  text-align: center;
  color: white;
}

h1 {
  font-size: 120px;
  font-weight: 700;
  margin: 0;
  line-height: 1;
  opacity: 0.9;
}

p {
  font-size: 24px;
  margin: 16px 0 32px;
  opacity: 0.85;
}

button {
  padding: 14px 40px;
  border: 2px solid white;
  border-radius: 10px;
  background: transparent;
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

button:hover {
  background: white;
  color: #667eea;
}
</style>
