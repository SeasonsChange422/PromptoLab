<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { computed, ref, onMounted, watch, onUnmounted } from 'vue'
import { userApi } from '@/services/userApi'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()

// 用户信息
interface UserInfo {
  nickname?: string
  email?: string
  avatar?: string
}

const userInfo = ref<UserInfo | null>(null)
const token = ref<string | null>(null)

// 登录状态判断
const isLoggedIn = computed(() => {
  return !!token.value && !!userInfo.value
})

// 判断是否在工作区页面，如果是则隐藏导航
const isWorkspacePage = computed(() => {
  return route.path.startsWith('/workspace/')
})

// 获取用户信息
const getUserInfo = () => {
  const storedToken = localStorage.getItem('token')
  token.value = storedToken

  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    try {
      userInfo.value = JSON.parse(storedUserInfo)
    } catch {
      userInfo.value = null
      localStorage.removeItem('userInfo')
    }
  } else {
    userInfo.value = null
  }
}

// 监听路由变化，重新检查登录状态
watch(() => route.path, () => {
  getUserInfo()
})

// 监听 localStorage 变化
const handleStorageChange = (e: StorageEvent) => {
  if (e.key === 'token' || e.key === 'userInfo') {
    getUserInfo()
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await userApi.logout()
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    token.value = null
    userInfo.value = null
    toast.success({
      title: '退出成功',
      message: '您已成功退出登录'
    })
    router.push('/auth')
  } catch {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    token.value = null
    userInfo.value = null
    toast.warning({
      title: '退出登录',
      message: '已清除本地登录信息'
    })
    router.push('/auth')
  }
}

// 跳转到登录页
const goToLogin = () => {
  router.push('/auth')
}

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo()
  window.addEventListener('storage', handleStorageChange)
})

// 组件卸载时移除监听器
onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<template>
  <!-- 顶部导航栏 - 重新设计的奢华风格 -->
  <header v-if="!isWorkspacePage" class="app-header">
    <div class="header-glass-effect"></div>
    <div class="header-content">
      <!-- Logo 区域 - 更精致的设计 -->
      <div class="logo-section">
        <RouterLink to="/" class="logo-link">
          <div class="logo">
            <div class="logo-icon-wrapper">
              <div class="logo-inner-glow"></div>
              <span class="logo-icon">◈</span>
            </div>
            <div class="logo-text-wrapper">
              <span class="logo-text">PromptoLab</span>
              <span class="logo-subtitle">AI Engineering</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <!-- 导航菜单 - 更现代的设计 -->
      <nav class="main-nav">
        <div class="nav-background"></div>
        <RouterLink to="/" class="nav-link">
          <div class="nav-content">
            <div class="nav-icon-wrapper">
              <span class="nav-icon">⬢</span>
            </div>
            <span class="nav-text">首页</span>
          </div>
          <div class="nav-glow"></div>
        </RouterLink>
        <RouterLink to="/prompt-builder" class="nav-link">
          <div class="nav-content">
            <div class="nav-icon-wrapper">
              <span class="nav-icon">⬡</span>
            </div>
            <span class="nav-text">构建器</span>
          </div>
          <div class="nav-glow"></div>
        </RouterLink>
      </nav>

      <!-- 右侧操作区域 - 更精致的设计 -->
      <div class="actions-section">
        <!-- 状态指示器 -->
        <div class="status-indicator">
          <div class="status-dot"></div>
          <span class="status-text">在线</span>
        </div>
        
        <!-- 用户信息 -->
        <div v-if="isLoggedIn" class="user-info">
          <div class="user-avatar">
            <img v-if="userInfo?.avatar" :src="userInfo.avatar" :alt="userInfo.nickname" />
            <div v-else class="avatar-placeholder">
              {{ userInfo?.nickname?.charAt(0) || userInfo?.email?.charAt(0) || 'U' }}
            </div>
            <div class="avatar-ring"></div>
          </div>
          <div class="user-details">
            <div class="user-name">{{ userInfo?.nickname || userInfo?.email || '用户' }}</div>
            <div class="user-status">专业版用户</div>
          </div>
          <div class="action-btn-wrapper">
            <button @click="handleLogout" class="action-btn logout-btn" title="退出登录">
              <span class="btn-icon">⏻</span>
              <div class="btn-ripple"></div>
            </button>
          </div>
        </div>

        <!-- 登录按钮 -->
        <button v-else @click="goToLogin" class="login-btn">
          <div class="btn-background"></div>
          <span class="btn-text">登录</span>
          <div class="btn-icon">→</div>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* 顶部导航栏 - 重新设计的奢华风格 */
.app-header {
  /* position: sticky; */
  /* top: 0; */
  /* z-index: 1000; */
  background: rgba(8, 8, 8, 0.85);
  /* backdrop-filter: blur(24px) saturate(180%); */
  border-bottom: 1px solid rgba(212, 175, 55, 0.15);
  box-shadow: 
    0 1px 0 rgba(212, 175, 55, 0.1),
    0 8px 32px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.02);
  position: relative;
  overflow: hidden;
}

.header-glass-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(135deg, 
      rgba(212, 175, 55, 0.03) 0%, 
      transparent 30%, 
      transparent 70%, 
      rgba(212, 175, 55, 0.03) 100%
    );
  animation: header-shimmer 8s ease-in-out infinite;
}

@keyframes header-shimmer {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.header-content {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88px;
  position: relative;
  z-index: 2;
}

/* Logo 区域 - 更精致的设计 */
.logo-section {
  flex-shrink: 0;
}

.logo-link {
  text-decoration: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: block;
}

.logo-link:hover {
  transform: translateY(-2px);
  filter: drop-shadow(0 8px 24px rgba(212, 175, 55, 0.4));
}

.logo {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo-icon-wrapper {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #d4af37 0%, #f4d03f 50%, #d4af37 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 8px 24px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.2);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.logo-icon-wrapper:hover {
  transform: scale(1.05) rotate(5deg);
  box-shadow: 
    0 12px 32px rgba(212, 175, 55, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.logo-inner-glow {
  position: absolute;
  top: 2px;
  left: 2px;
  right: 2px;
  bottom: 2px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2), transparent);
  border-radius: 12px;
  pointer-events: none;
}

.logo-icon {
  font-size: 28px;
  color: #1a1a1a;
  z-index: 2;
  position: relative;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.logo-text-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo-text {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 0%, #d4af37 50%, #f4d03f 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
  line-height: 1;
}

.logo-subtitle {
  font-size: 11px;
  color: #888;
  font-weight: 500;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-left: 2px;
}

/* 导航菜单 - 更现代的设计 */
.main-nav {
  display: flex;
  gap: 24px;
  /* background: rgba(15, 15, 15, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(212, 175, 55, 0.2); */
  /* border-radius: 20px;
  padding: 6px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.08); */
  position: relative;
}

.nav-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 20px;
  pointer-events: none;
}

.nav-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #b8b8b8;
  border-radius: 16px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  font-size: 14px;
  position: relative;
  overflow: hidden;
  min-width: 120px;
  padding: 14px 20px;
  border: 1px solid transparent;
}

.nav-content {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 2;
}

.nav-link:hover {
  background: rgba(212, 175, 55, 0.08);
  border-color: rgba(212, 175, 55, 0.25);
  color: #d4af37;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.15);
}

.nav-link.router-link-active {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.12), rgba(244, 208, 63, 0.08));
  border-color: rgba(212, 175, 55, 0.4);
  color: #d4af37;
  box-shadow: 
    0 8px 24px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.nav-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), transparent);
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.4s ease;
}

.nav-link:hover .nav-glow,
.nav-link.router-link-active .nav-glow {
  opacity: 1;
}

.nav-icon-wrapper {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(212, 175, 55, 0.1);
  border: 1px solid rgba(212, 175, 55, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-link:hover .nav-icon-wrapper,
.nav-link.router-link-active .nav-icon-wrapper {
  background: rgba(212, 175, 55, 0.2);
  border-color: rgba(212, 175, 55, 0.4);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
  transform: scale(1.1);
}

.nav-icon {
  font-size: 14px;
  color: #d4af37;
  font-weight: 600;
}

.nav-text {
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* 右侧操作区域 - 更精致的设计 */
.actions-section {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.6);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 12px;
  color: #22c55e;
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 20px;
  background: rgba(15, 15, 15, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  box-shadow: 
    0 8px 24px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.user-info:hover {
  border-color: rgba(212, 175, 55, 0.4);
  box-shadow: 
    0 12px 32px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.15);
  transform: translateY(-2px);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  border: 2px solid rgba(212, 175, 55, 0.4);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
  position: relative;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: #1a1a1a;
  font-weight: 700;
  font-size: 16px;
}

.avatar-ring {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border: 2px solid transparent;
  border-radius: 50%;
  background: linear-gradient(45deg, #d4af37, transparent, #f4d03f);
  background-clip: border-box;
  animation: rotate 3s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #e8e8e8;
}

.user-status {
  font-size: 11px;
  color: #d4af37;
  font-weight: 500;
}

.action-btn-wrapper {
  position: relative;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(15, 15, 15, 0.9);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #b8b8b8;
  font-size: 18px;
  position: relative;
  overflow: hidden;
}

.action-btn:hover {
  border-color: rgba(212, 175, 55, 0.5);
  color: #d4af37;
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.25);
  transform: scale(1.05);
}

.btn-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(212, 175, 55, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
}

.action-btn:active .btn-ripple {
  width: 100px;
  height: 100px;
}

.logout-btn:hover {
  border-color: rgba(239, 68, 68, 0.5);
  color: #ef4444;
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.25);
}

.login-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 28px;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.4);
  border-radius: 16px;
  color: #d4af37;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.login-btn:hover {
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  color: #1a1a1a;
  border-color: #d4af37;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.4);
}

.btn-background {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.1), transparent);
  transition: left 0.6s ease;
}

.login-btn:hover .btn-background {
  left: 100%;
}

.btn-text {
  position: relative;
  z-index: 2;
}

.btn-icon {
  position: relative;
  z-index: 2;
  font-size: 16px;
  transition: transform 0.3s ease;
}

.login-btn:hover .btn-icon {
  transform: translateX(4px);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .header-content {
    padding: 0 24px;
  }
  
  .user-details {
    display: none;
  }
  
  .status-indicator {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 20px;
    height: 76px;
  }
  
  .logo-text-wrapper {
    display: none;
  }
  
  .main-nav {
    gap: 2px;
    padding: 4px;
  }
  
  .nav-link {
    padding: 12px 16px;
    font-size: 12px;
    min-width: 80px;
  }
  
  .nav-text {
    display: none;
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: 0 16px;
    gap: 12px;
  }
  
  .logo-icon-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .nav-link {
    padding: 10px 12px;
    min-width: 60px;
  }
}
</style>