<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-layout">
    <header class="app-header">
      <img alt="Vue logo" class="logo" src="@/assets/logo.svg" width="36" height="36" />
      <span class="brand">Drive2Stars</span>
      <nav>
        <RouterLink to="/">Simulation</RouterLink>
        <RouterLink to="/scenario">Scenarios</RouterLink>
      </nav>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 1.5rem;
  background: var(--color-background-mute);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.logo {
  display: block;
}

.brand {
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-heading);
  margin-right: 0.5rem;
}

nav {
  display: flex;
  gap: 0.25rem;
}

nav a {
  font-size: 0.88rem;
  color: var(--color-text);
  text-decoration: none;
  padding: 0.22rem 0.6rem;
  border-radius: 4px;
  opacity: 0.7;
  transition: opacity 0.15s;
}

nav a:hover {
  opacity: 1;
}

main {
  flex: 1;
}
</style>
