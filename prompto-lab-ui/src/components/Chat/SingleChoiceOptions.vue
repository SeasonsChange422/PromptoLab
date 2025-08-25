<template>
  <div class="single-choice-options">
    <div class="options-container">
      <!-- 原有选项 -->
      <div 
        v-for="option in options" 
        :key="option.id"
        @click="selectOption(option.id)"
        class="option-item"
        :class="{ 
          selected: selectedValue === option.id,
          disabled: disabled
        }"
      >
        <div class="option-radio" :class="{ small: size === 'small' }">
          <div class="radio-dot"></div>
          <div class="radio-ripple"></div>
        </div>
        <div class="option-content">
          <span class="option-label">{{ option.label }}</span>
        </div>
      </div>
      
      <!-- 自定义选项 -->
      <div 
        v-for="customOption in customOptions" 
        :key="customOption.id"
        @click="selectOption(customOption.id)"
        class="option-item custom-option"
        :class="{ 
          selected: selectedValue === customOption.id,
          disabled: disabled
        }"
      >
        <div class="option-radio" :class="{ small: size === 'small' }">
          <div class="radio-dot"></div>
          <div class="radio-ripple"></div>
        </div>
        <div class="option-content">
          <span class="option-label">{{ customOption.label }}</span>
        </div>
        <button 
          @click.stop="removeCustomOption(customOption.id)"
          class="remove-option-btn"
          :disabled="disabled"
          title="删除此选项"
        >
          ×
        </button>
      </div>
      
      <!-- 添加选项区域 -->
      <div v-if="!disabled" class="add-option-container">
        <!-- 正在添加选项的输入框 -->
        <div v-if="showAddInput" class="add-option-input-container">
          <div class="option-item add-input-item">
            <div class="option-radio" :class="{ small: size === 'small' }">
              <div class="radio-dot"></div>
            </div>
            <div class="option-content">
              <input 
                ref="addInputRef"
                v-model="newOptionText"
                @keydown.enter="confirmAddOption"
                @keydown.escape="cancelAddOption"
                @blur="handleInputBlur"
                placeholder="请输入新选项内容..."
                class="add-option-input"
              />
            </div>
            <div class="add-option-actions">
              <button @click="confirmAddOption" class="confirm-add-btn" :disabled="!newOptionText.trim()">✓</button>
              <button @click="cancelAddOption" class="cancel-add-btn">×</button>
            </div>
          </div>
        </div>
        
        <!-- 添加选项按钮 -->
        <div v-else class="add-option-btn-container">
          <button @click="startAddOption" class="add-option-btn">
            <span class="add-icon">+</span>
            <span class="add-text">添加选项</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'

interface Option {
  id: string
  label: string
}

interface Props {
  options: Option[]
  selectedValue?: string
  disabled?: boolean
  size?: 'normal' | 'small'
}

interface Emits {
  'update:selectedValue': [value: string]
  'change': [value: string]
  'optionsChange': [allOptions: Option[]]
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  size: 'normal'
})

const emit = defineEmits<Emits>()

// 自定义选项相关状态
const customOptions = ref<Option[]>([])
const showAddInput = ref(false)
const newOptionText = ref('')
const addInputRef = ref<HTMLInputElement>()
const customOptionCounter = ref(1)

const selectOption = (optionId: string) => {
  if (props.disabled) return
  
  // 查找选中的选项
  const allOptions = [...props.options, ...customOptions.value]
  const selectedOption = allOptions.find(option => option.id === optionId)
  
  if (selectedOption) {
    // 格式化为 "id:content" 字符串
    const formattedValue = `${selectedOption.id}:${selectedOption.label}`
    emit('update:selectedValue', formattedValue)
    emit('change', formattedValue)
  }
}

const startAddOption = () => {
  showAddInput.value = true
  newOptionText.value = ''
  nextTick(() => {
    addInputRef.value?.focus()
  })
}

const confirmAddOption = () => {
  const text = newOptionText.value.trim()
  if (!text) return
  
  const newOption: Option = {
    id: `other${customOptionCounter.value}`,
    label: text
  }
  
  customOptions.value.push(newOption)
  customOptionCounter.value++
  
  // 通知父组件选项变化
  const allOptions = [...props.options, ...customOptions.value]
  emit('optionsChange', allOptions)
  
  // 重置状态
  showAddInput.value = false
  newOptionText.value = ''
}

const cancelAddOption = () => {
  showAddInput.value = false
  newOptionText.value = ''
}

const handleInputBlur = () => {
  // 延迟取消，避免点击确认按钮时输入框失焦导致取消
  setTimeout(() => {
    if (showAddInput.value && !newOptionText.value.trim()) {
      cancelAddOption()
    }
  }, 150)
}

const removeCustomOption = (optionId: string) => {
  const index = customOptions.value.findIndex(option => option.id === optionId)
  if (index > -1) {
    customOptions.value.splice(index, 1)
    
    // 如果删除的选项正好是当前选中的，清除选择
    if (props.selectedValue === optionId) {
      emit('update:selectedValue', '')
      emit('change', '')
    }
    
    // 通知父组件选项变化
    const allOptions = [...props.options, ...customOptions.value]
    emit('optionsChange', allOptions)
  }
}
</script>

<style scoped>
.single-choice-options {
  width: 100%;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(20px);
}

.option-item:hover:not(.disabled) {
  border-color: rgba(212, 175, 55, 0.4);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(244, 208, 63, 0.05));
  transform: translateX(4px);
}

.option-item.selected {
  border-color: rgba(212, 175, 55, 0.6);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(244, 208, 63, 0.1));
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.2);
}

.option-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.option-radio {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(212, 175, 55, 0.4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.option-radio.small {
  width: 16px;
  height: 16px;
}

.option-item.selected .option-radio {
  border-color: #d4af37;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
}

.radio-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a1a1a;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.option-item.selected .radio-dot {
  opacity: 1;
}

.radio-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.1);
  opacity: 0;
  transition: all 0.3s ease;
  pointer-events: none;
}

.option-item:active:not(.disabled) .radio-ripple {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.2);
}

.option-content {
  flex: 1;
}

.option-label {
  font-size: 16px;
  color: #e8e8e8;
  font-weight: 500;
  line-height: 1.4;
}

.option-item.selected .option-label {
  color: #667eea;
  font-weight: 600;
}

/* 自定义选项样式 */
.custom-option {
  border-style: dashed;
}

.remove-option-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.remove-option-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.3);
  transform: scale(1.1);
}

.remove-option-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 添加选项样式 */
.add-option-container {
  margin-top: 0;
}

.add-option-btn-container {
  display: flex;
}

.add-option-btn {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  width: 100%;
  background: linear-gradient(135deg, rgba(45, 45, 45, 0.9), rgba(35, 35, 35, 0.9));
  border: 1px solid rgba(80, 80, 80, 0.3);
  border-radius: 16px;
  color: rgba(200, 200, 200, 0.8);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 16px;
  font-weight: 500;
  backdrop-filter: blur(20px);
  position: relative;
  overflow: hidden;
}

.add-option-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.1), transparent);
  transition: left 0.6s ease;
}

.add-option-btn:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.08), rgba(244, 208, 63, 0.04));
  color: rgba(212, 175, 55, 0.9);
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(212, 175, 55, 0.1);
}

.add-option-btn:hover::before {
  left: 100%;
}

.add-option-btn:active {
  transform: translateX(4px) scale(0.98);
}

.add-icon {
  width: 20px;
  height: 20px;
  border: 2px solid currentColor;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.add-option-btn:hover .add-icon {
  background: rgba(212, 175, 55, 0.1);
  transform: rotate(90deg);
}

.add-text {
  flex: 1;
  text-align: left;
}

.add-input-item {
  cursor: default;
}

.add-input-item:hover {
  transform: none;
}

.add-option-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 500;
}

.add-option-input::placeholder {
  color: #a0a0a0;
}

.add-option-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.confirm-add-btn,
.cancel-add-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.confirm-add-btn {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.confirm-add-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.3);
  transform: scale(1.1);
}

.confirm-add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cancel-add-btn {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.cancel-add-btn:hover {
  background: rgba(239, 68, 68, 0.3);
  transform: scale(1.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .option-item {
    padding: 14px 16px;
  }
  
  .option-radio {
    width: 18px;
    height: 18px;
    margin-right: 14px;
  }
  
  .option-label {
    font-size: 0.95rem;
  }
  
  .add-option-btn {
    padding: 10px 16px;
    font-size: 13px;
  }
  
  .remove-option-btn {
    width: 20px;
    height: 20px;
    font-size: 14px;
  }
}
</style>