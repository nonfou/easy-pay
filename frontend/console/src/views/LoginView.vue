<template>
  <section class="login">
    <h1>Console Login</h1>
    <form @submit.prevent="handleLogin">
      <label>
        Username
        <input v-model="form.username" />
      </label>
      <label>
        Password
        <input type="password" v-model="form.password" />
      </label>
      <button type="submit" :disabled="loading">Login</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { httpClient } from '../services/httpClient'

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  loading.value = true
  error.value = ''
  try {
    await httpClient.post('/api/login', form)
    window.location.href = '/'
  } catch (e) {
    error.value = `Login failed: ${e}`
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  max-width: 360px;
  margin: 120px auto;
}
.error {
  color: red;
}
</style>
