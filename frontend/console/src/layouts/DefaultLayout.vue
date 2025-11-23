<template>
  <div class="layout">
    <aside>
      <nav>
        <router-link to="/">Dashboard</router-link>
        <router-link to="/orders">Orders</router-link>
      </nav>
    </aside>
    <main>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}
aside {
  width: 200px;
  background: #20232a;
  color: #fff;
  padding: 1rem;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
main {
  flex: 1;
  padding: 1.5rem;
}
</style>
