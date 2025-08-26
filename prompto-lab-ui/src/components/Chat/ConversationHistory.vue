<template>
  <div class="conversation-history">
    <div class="sidebar-content">
      <!-- 可展开/收缩的头部按钮 -->
      <div class="sidebar-toggle" @click="toggleSidebar">
        <div class="toggle-icon">
          <span class="icon">{{ sidebarExpanded ? '◀' : '▶' }}</span>
        </div>
        <div class="toggle-text" v-if="sidebarExpanded">
          <h3>会话列表</h3>
          <p>点击收起</p>
        </div>
      </div>
      
      <!-- 会话列表 -->
      <div class="session-list" v-if="sidebarExpanded">
        <div class="session-list-header">
          <h4>最近对话</h4>
          <button class="new-chat-btn" @click="handleStartNewChat">
            <span class="plus-icon">+</span>
            新对话
          </button>
        </div>
        
        <div class="session-items">
          <div
            v-for="sessionItem in sessionList"
            :key="sessionItem.id"
            class="session-item"
            :class="{ active: sessionItem.id === currentSessionId }"
            @click="handleSwitchToSession(sessionItem.id)"
          >
            <div class="session-content">
              <div class="session-title">{{ sessionItem.title }}</div>
              <div class="session-preview">{{ sessionItem.lastMessage }}</div>
              <div class="session-time">{{ formatTime(sessionItem.updatedAt) }}</div>
            </div>
            <div class="session-actions">
              <button class="delete-btn" @click.stop="handleDeleteSession(sessionItem.id)">
                <span>×</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits } from 'vue'
import type { SessionItem } from '@/services/conversationApi'

// Props
interface Props {
  sessionList: SessionItem[]
  currentSessionId: string
  sidebarExpanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sidebarExpanded: true
})

// Emits
interface Emits {
  startNewChat: []
  switchToSession: [sessionId: string]
  deleteSession: [sessionId: string]
  toggleSidebar: []
}

const emit = defineEmits<Emits>()

// 本地状态
const sidebarExpanded = ref(props.sidebarExpanded)

// 方法
const toggleSidebar = () => {
  sidebarExpanded.value = !sidebarExpanded.value
  emit('toggleSidebar')
}

const handleStartNewChat = () => {
  emit('startNewChat')
}

const handleSwitchToSession = (sessionId: string) => {
  emit('switchToSession', sessionId)
}

const handleDeleteSession = (sessionId: string) => {
  emit('deleteSession', sessionId)
}

const formatTime = (date: Date | string) => {
  // 如果传入的是字符串，尝试解析为Date对象
  let dateObj: Date;
  if (typeof date === 'string') {
    // 处理后端返回的时间格式
    if (date.includes('.')) {
      // 处理带纳秒的时间格式，如 "2025-08-26 02:45:50.591607900"
      const parts = date.split('.');
      if (parts.length > 1) {
        // 只取前3位小数（毫秒）
        const milliseconds = parts[1].substring(0, 3);
        const timeWithMs = parts[0] + '.' + milliseconds;
        dateObj = new Date(timeWithMs);
      } else {
        dateObj = new Date(date);
      }
    } else {
      dateObj = new Date(date);
    }
  } else {
    dateObj = date;
  }

  // 检查日期是否有效
  if (isNaN(dateObj.getTime())) {
    return '无效时间';
  }

  const now = new Date();
  const diff = now.getTime() - dateObj.getTime();
  const minutes = Math.floor(diff / (1000 * 60));
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (minutes < 1) {
    return '刚刚';
  } else if (minutes < 60) {
    return `${minutes}分钟前`;
  } else if (hours < 24) {
    return `${hours}小时前`;
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return dateObj.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric'
    });
  }
}
</script>

<style scoped>
.conversation-history {
  height: 100%;
  background: rgba(8, 8, 8, 0.9);
  backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(212, 175, 55, 0.15);
  position: relative;
  z-index: 3;
  box-shadow:
    inset -1px 0 0 rgba(212, 175, 55, 0.1),
    0 0 32px rgba(0, 0, 0, 0.3);
}

.sidebar-content {
  height: 100%;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
  overflow: hidden;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.8), rgba(25, 25, 25, 0.8));
  border: 1px solid rgba(212, 175, 55, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.sidebar-toggle:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: linear-gradient(135deg, rgba(20, 20, 20, 0.9), rgba(30, 30, 30, 0.9));
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.3),
    0 0 20px rgba(212, 175, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.toggle-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 4px 12px rgba(212, 175, 55, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.sidebar-toggle:hover .toggle-icon {
  transform: scale(1.05);
  box-shadow:
    0 6px 16px rgba(212, 175, 55, 0.4),
    0 0 20px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.icon {
  font-size: 20px;
  font-weight: bold;
  color: #1a1a1a;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.toggle-text {
  flex: 1;
}

.toggle-text h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.toggle-text p {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.session-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.session-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.15);
}

.session-list-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(244, 208, 63, 0.1));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 8px;
  color: #d4af37;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.new-chat-btn:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(244, 208, 63, 0.2));
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.2);
  transform: translateY(-1px);
}

.plus-icon {
  font-size: 16px;
  font-weight: bold;
}

.session-items {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 8px;
}

.session-items::-webkit-scrollbar {
  width: 6px;
}

.session-items::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.session-items::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.3), rgba(212, 175, 55, 0.6));
  border-radius: 3px;
}

.session-items::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.5), rgba(212, 175, 55, 0.8));
}

.session-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.6), rgba(25, 25, 25, 0.6));
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.session-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05), rgba(244, 208, 63, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.session-item:hover::before {
  opacity: 1;
}

.session-item:hover {
  border-color: rgba(212, 175, 55, 0.3);
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.3),
    0 0 20px rgba(212, 175, 55, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.session-item.active {
  border-color: rgba(212, 175, 55, 0.6);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(244, 208, 63, 0.1));
  box-shadow:
    0 4px 16px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(212, 175, 55, 0.2);
}

.session-content {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.session-preview {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 400;
}

.session-actions {
  position: relative;
  z-index: 2;
}

.delete-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(255, 59, 48, 0.1);
  border: 1px solid rgba(255, 59, 48, 0.2);
  color: #ff3b30;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  opacity: 0;
  transform: scale(0.8);
}

.session-item:hover .delete-btn {
  opacity: 1;
  transform: scale(1);
}

.delete-btn:hover {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
  transform: scale(1.05);
}

.delete-btn span {
  font-size: 16px;
  font-weight: bold;
  line-height: 1;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .sidebar-content {
    padding: 24px 16px;
  }
}

@media (max-width: 768px) {
  .conversation-history {
    width: 100% !important;
    height: 200px;
  }

  .sidebar-content {
    padding: 16px 12px;
    gap: 20px;
  }

  .sidebar-toggle {
    padding: 16px;
    gap: 12px;
  }

  .toggle-icon {
    width: 40px;
    height: 40px;
  }

  .icon {
    font-size: 18px;
  }
}
</style>