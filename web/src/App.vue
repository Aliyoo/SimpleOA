<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useTheme } from '@/composables/useTheme'

// 初始化主题系统
let themeCleanup = null

onMounted(() => {
  const { initTheme } = useTheme()
  themeCleanup = initTheme()
})

onUnmounted(() => {
  if (themeCleanup) {
    themeCleanup()
    themeCleanup = null
  }
})
</script>

<style>
@import './styles/global.css';
</style>
