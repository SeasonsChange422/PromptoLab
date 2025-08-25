<template>
  <div class="message" :class="message.type">
    <div class="message-avatar">
      <div class="avatar-wrapper" :class="message.type">
        <div class="avatar-glow"></div>
        <span class="avatar-icon">
          {{ message.type === 'user' ? '👤' : '🤖' }}
        </span>
      </div>
    </div>
    <div class="message-content">
      <div class="message-bubble" :class="message.type">
        <div class="bubble-glow"></div>
        <div class="message-text" :class="{ 'streaming': isStreaming }">
          {{ message.content }}
          <span v-if="isStreaming" class="cursor-blink">|</span>
        </div>
        <div class="message-time">{{ formatTime(new Date(message.timestamp)) }}</div>
      </div>
      <!-- <DynamicSelect :fieldConfig="fieldConfig" :modelValue="val" :isDark="true"/> -->

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Message {
  id: string
  content: string
  type: 'user' | 'assistant'
  timestamp: Date
}


interface Props {
  message: Message
  isStreaming?: boolean
}
const handleSelectionChange = (value: any)=>{
  console.log(value)
}
defineProps<Props>()

const formatTime = (timestamp: Date) => {
  // 检查日期是否有效
  if (!timestamp || !(timestamp instanceof Date) || isNaN(timestamp.getTime())) {
    return '未知时间'
  }
  
  return timestamp.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.message {
  margin-bottom: 32px;
  display: flex;
  gap: 16px;
  animation: message-appear 0.5s ease-out;
}
.message .dynamic-select {
  --bg-primary: #0a0a0a;
  --bg-secondary: #141414;
  --bg-selected: #d4af37;
  --text-primary: #ffffff;
  --text-secondary: #e8e8e8;
  --text-muted: #a0a0a0;
  --border-default: rgba(212, 175, 55, 0.2);
  --border-focus: #d4af37;
  --border-disabled: #333333;
  --shadow-hover: rgba(212, 175, 55, 0.3);
}
@keyframes message-appear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s ease;
}

.avatar-wrapper.user {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.3);
}

.avatar-wrapper.assistant {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.avatar-glow {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  opacity: 0.6;
  animation: avatar-glow 3s ease-in-out infinite;
}

.avatar-wrapper.user .avatar-glow {
  background: linear-gradient(45deg, #22c55e, transparent, #16a34a);
}

.avatar-wrapper.assistant .avatar-glow {
  background: linear-gradient(45deg, #3b82f6, transparent, #1d4ed8);
}

@keyframes avatar-glow {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.avatar-icon {
  font-size: 20px;
  z-index: 2;
  position: relative;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-bubble {
  padding: 20px 24px;
  border-radius: 20px;
  position: relative;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.message-bubble.user {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(22, 163, 74, 0.1));
  border-color: rgba(34, 197, 94, 0.3);
  margin-left: auto;
}

.message-bubble.assistant {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(29, 78, 216, 0.1));
  border-color: rgba(59, 130, 246, 0.3);
}

.bubble-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 20px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.message-bubble.user .bubble-glow {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.1), transparent);
}

.message-bubble.assistant .bubble-glow {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), transparent);
}

.message-bubble:hover .bubble-glow {
  opacity: 1;
}

.message-text {
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #e8e8e8;
  font-size: 15px;
  font-weight: 400;
  position: relative;
  z-index: 2;
}

.message-text.streaming {
  position: relative;
}

.cursor-blink {
  display: inline-block;
  animation: blink 1s infinite;
  color: #d4af37;
  font-weight: bold;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.message-text.streaming::after {
  content: '';
  position: absolute;
  right: -20px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(45deg, #d4af37, #f4d03f);
  border-radius: 2px;
  animation: typing-indicator 1.5s ease-in-out infinite;
}

@keyframes typing-indicator {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

.message-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 8px;
  color: #a0a0a0;
  font-weight: 500;
  position: relative;
  z-index: 2;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message {
    margin-bottom: 24px;
  }
  
  .message-content {
    max-width: 85%;
  }
}

@media (max-width: 480px) {
  .avatar-wrapper {
    width: 40px;
    height: 40px;
  }
  
  .avatar-icon {
    font-size: 16px;
  }
  
  .message-bubble {
    padding: 16px 20px;
  }
  
  .message-content {
    max-width: 90%;
  }
}
</style>