<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import { ref,onMounted,onUnmounted,computed } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { throttle } from './utils/common'

const route = useRoute()
const appRef = ref<HTMLDivElement>()
// 判断是否在工作区页面，如果是则隐藏导航
const isWorkspacePage = computed(() => {
  return route.path.startsWith('/workspace/')
})
const isScrolled = ref(false)
const headerHeight = 88 // AppHeader的高度

// 滚动监听函数
const handleScroll = throttle(() => {
  const scrollTop = appRef.value?.scrollTop || 0
  isScrolled.value = scrollTop > headerHeight
},100)

// 组件挂载时添加滚动监听
onMounted(() => {
  appRef.value?.addEventListener('scroll', handleScroll, { passive: true })
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  appRef.value?.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="app" ref="appRef">
    <!-- 顶部导航栏组件 -->
    <AppHeader v-if="isScrolled && !isWorkspacePage" 
      class="scroll-header"/>

    <AppHeader />
    <!-- 主要内容区域 -->
    <main class="app-main" :class="{ 'fullscreen': isWorkspacePage }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app {
  height: 100vh;
  overflow-y: scroll;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
  color: #e8e8e8;
  font-family: 'Inter', 'SF Pro Display', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 主要内容区域 */
.app-main {
  flex: 1;
  /* background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); */
  margin: 0;
  border-radius: 0;
  min-height: 0; /* 重要：允许flex子项收缩 */
}

.app-main.fullscreen {
  /* background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%); */
  margin: 0;
  border-radius: 0;
}
.scroll-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1001;
  background: rgba(8, 8, 8, 0.8);
  /* backdrop-filter: blur(20px) saturate(180%); */
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
  box-shadow: 
    0 2px 20px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.scroll-header-content {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
</style>
