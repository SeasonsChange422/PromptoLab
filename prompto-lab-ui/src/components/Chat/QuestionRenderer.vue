<template>
  <div class="question-renderer">
    <!-- 动态背景粒子 -->
    <div class="question-background">
      <div class="background-particles">
        <div class="bg-particle" v-for="i in 12" :key="i"></div>
      </div>
    </div>

    <!-- 对话历史显示 -->
    <div v-if="!currentQuestion && isViewingHistory" class="conversation-history">
      <div class="history-header">
        <h3>对话历史</h3>
        <p v-if="conversationTree && conversationTree.size > 0">点击任意节点继续对话</p>
        <p v-else>该会话暂无历史记录</p>
      </div>
      <div v-if="conversationTree && conversationTree.size > 0" class="chat-messages">
        <div 
          v-for="[nodeId, node] in conversationTree" 
          :key="nodeId"
          @click="emit('nodeSelected', nodeId)"
          class="chat-message"
          :class="{ 
            'message-left': node.type === 'assistant',
            'message-right': node.type === 'user',
            'active-message': nodeId === currentNodeId
          }"
        >
          <div class="message-bubble">
            <!-- AI问题（左侧） -->
            <template v-if="node.type === 'assistant'">
              <!-- 检查是否为结构化问题数据 -->
              <template v-if="isStructuredQuestion(node.content)">
                <!-- 渲染结构化问题 -->
                <div class="structured-question-chat">
                  <div class="question-title-chat">
                    <span class="question-icon">{{ getQuestionIcon(parseStructuredQuestion(node.content).type) }}</span>
                    {{ parseStructuredQuestion(node.content).question }}
                  </div>
                  <div v-if="parseStructuredQuestion(node.content).desc" class="question-desc-chat">
                    {{ parseStructuredQuestion(node.content).desc }}
                  </div>
                  
                  <!-- 根据问题类型渲染不同的预览 -->
                  <div class="question-options-chat">
                    <template v-if="parseStructuredQuestion(node.content).type === 'form'">
                      <div class="form-fields-preview">
                        <div v-for="field in parseStructuredQuestion(node.content).fields" 
                             :key="field.id" 
                             class="field-preview">
                          <div class="field-title">{{ field.question }}</div>
                          <div class="field-options">
                            <span v-for="option in field.options?.slice(0, 3)" 
                                  :key="option.id" 
                                  class="option-chip" 
                                  :class="field.type">{{ option.label }}</span>
                            <span v-if="field.options?.length > 3" class="more-options">+{{ field.options.length - 3 }}个选项</span>
                          </div>
                        </div>
                      </div>
                    </template>
                    
                    <template v-else>
                      <div class="options-list">
                        <span v-for="option in parseStructuredQuestion(node.content).options?.slice(0, 4)" 
                              :key="option.id" 
                              class="option-chip" 
                              :class="parseStructuredQuestion(node.content).type">{{ option.label }}</span>
                        <span v-if="parseStructuredQuestion(node.content).options?.length > 4" class="more-options">+{{ parseStructuredQuestion(node.content).options.length - 4 }}个选项</span>
                      </div>
                    </template>
                  </div>
                </div>
              </template>
              
              <!-- 普通文本问题 -->
              <template v-else>
                <!-- 检查是否包含结构化内容但未被正确识别 -->
                <template v-if="containsStructuredContent(node.content)">
                  <div class="message-text ai-message">
                    <div v-html="formatAIMessage(node.content)"></div>
                    <!-- 如果内容中包含可交互的问题，在这里添加交互组件 -->
                    <div v-if="extractQuestionFromContent(node.content)" class="embedded-question">
                      <div class="question-container">
                        <template v-if="extractQuestionFromContent(node.content).type === 'single'">
                           <SingleChoiceOptions
                             :options="extractQuestionFromContent(node.content).options"
                             :selected-value="embeddedAnswers[nodeId] || ''"
                             :disabled="false"
                             @update:selected-value="handleEmbeddedAnswer(nodeId, $event)"
                           />
                         </template>
                         <template v-else-if="extractQuestionFromContent(node.content).type === 'multi'">
                           <MultipleChoiceOptions
                             :options="extractQuestionFromContent(node.content).options"
                             :selected-values="embeddedAnswers[nodeId] || []"
                             :disabled="false"
                             @update:selected-values="handleEmbeddedAnswer(nodeId, $event)"
                           />
                         </template>
                      </div>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <div class="message-text ai-message" v-html="formatAIMessage(node.content)"></div>
                </template>
              </template>
            </template>
            
            <!-- 用户回答（右侧） -->
            <template v-else>
              <div class="message-text user-message">{{ node.content }}</div>
            </template>
          </div>
          <div class="message-time">{{ formatTimestamp(node.timestamp) }}</div>
        </div>
      </div>
      <div v-else class="empty-history">
        <div class="empty-icon">📝</div>
        <p>这个会话还没有对话记录</p>
        <p class="empty-hint">开始新的对话来创建历史记录</p>
      </div>
    </div>

    <!-- 初始状态：大输入框和快捷按钮 -->
    <div v-else-if="!currentQuestion && !isViewingHistory" class="initial-state">
      <div class="welcome-section">
        <div class="welcome-icon">
          <div class="icon-glow"></div>
          <span class="icon">✨</span>
        </div>
        <h2 class="welcome-title">开始您的AI问答之旅</h2>
        <p class="welcome-subtitle">请输入您的问题，或选择下方快捷选项开始</p>
      </div>

      <div class="main-input-section">
        <div class="input-container">
          <div class="input-wrapper">
            <div class="input-glow"></div>
            <textarea 
              v-model="mainInput"
              @keydown="handleMainInputKeydown"
              placeholder="请输入您想了解的问题或需求..."
              class="main-input"
              rows="3"
              ref="mainInputRef"
            ></textarea>
            <button 
              @click="submitMainInput"
              :disabled="!mainInput.trim() || isLoading"
              class="main-submit-btn"
              :class="{ active: mainInput.trim() && !isLoading }"
            >
              <div class="btn-glow"></div>
              <span class="btn-icon">🚀</span>
              <span class="btn-text">开始问答</span>
            </button>
          </div>
        </div>
      </div>

      <div class="quick-actions">
        <div class="action-group">
          <button 
            @click="toggleQuickInput('introduce')"
            class="quick-btn"
            :class="{ expanded: expandedAction === 'introduce' }"
          >
            <span class="btn-icon">👋</span>
            <span class="btn-text">介绍自己</span>
          </button>
          
          <div v-if="expandedAction === 'introduce'" class="quick-input-container">
            <div class="quick-input-wrapper">
              <input 
                v-model="quickInputs.introduce"
                @keydown.enter="submitQuickInput('introduce')"
                placeholder="请简单介绍一下您自己..."
                class="quick-input"
                ref="introduceInputRef"
              />
              <button 
                @click="submitQuickInput('introduce')"
                :disabled="!quickInputs.introduce.trim()"
                class="quick-submit-btn"
              >
                <span>确认</span>
              </button>
            </div>
          </div>
        </div>



        <div class="action-group">
          <button 
            @click="toggleQuickInput('model')"
            class="quick-btn"
            :class="{ expanded: expandedAction === 'model' }"
          >
            <span class="btn-icon">🤖</span>
            <span class="btn-text">使用模型</span>
          </button>
          
          <div v-if="expandedAction === 'model'" class="quick-input-container">
            <div class="quick-input-wrapper">
              <input 
                v-model="quickInputs.model"
                @keydown.enter="submitQuickInput('model')"
                placeholder="请描述您想使用的AI模型或功能..."
                class="quick-input"
                ref="modelInputRef"
              />
              <button 
                @click="submitQuickInput('model')"
                :disabled="!quickInputs.model.trim()"
                class="quick-submit-btn"
              >
                <span>确认</span>
              </button>
            </div>
          </div>
        </div>

        <div class="action-group">
          <button 
          @click="generatePrompt"
           class="quick-btn"
          :disabled="isLoading"
        >
          <span class="btn-icon">✨</span>
          <span class="btn-text">生成提示词</span>
        </button>
        </div>
      </div>
    </div>

    <!-- 问答状态：根据问题类型渲染不同界面 -->
    <div v-else class="question-state">
      <!-- 问题标题和描述 -->
      <div class="question-header">
        <div class="question-title">
          <div class="title-icon">
            <span>{{ getQuestionIcon(currentQuestion.type) }}</span>
          </div>
          <h3>{{ currentQuestion.question }}</h3>
        </div>
        <p v-if="currentQuestion.desc" class="question-desc">{{ currentQuestion.desc }}</p>
      </div>

      <!-- 输入框问题 -->
      <div v-if="currentQuestion.type === 'input'" class="input-question">
        <div class="answer-input-container">
          <div class="answer-input-wrapper">
            <div class="input-glow"></div>
            <textarea 
              v-model="answers.input"
              @keydown="handleAnswerKeydown"
              placeholder="请输入您的答案..."
              class="answer-input"
              rows="4"
            ></textarea>
          </div>
        </div>
      </div>

      <!-- 单选问题 -->
      <div v-else-if="currentQuestion.type === 'single'" class="single-choice-question">
        <SingleChoiceOptions
          :options="currentQuestion.options"
          v-model:selected-value="answers.single"
          :disabled="isLoading"
        />
      </div>

      <!-- 多选问题 -->
      <div v-else-if="currentQuestion.type === 'multi'" class="multiple-choice-question">
        <MultipleChoiceOptions
          :options="currentQuestion.options"
          v-model:selected-values="answers.multiple"
          :disabled="isLoading"
        />
      </div>

      <!-- 表单问题 -->
      <div v-else-if="currentQuestion.type === 'form'" class="form-question">
        <div class="form-fields">
          <FormField
            v-for="field in currentQuestion.fields"
            :key="field.id"
            :field="field"
            v-model:value="answers.form[field.id]"
            :disabled="isLoading"
            :required="true"
            @change="handleFormFieldChange"
          />
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="question-actions">
        <button 
          @click="submitAnswer"
          :disabled="!isAnswerValid() || isLoading"
          class="submit-answer-btn"
          :class="{ active: isAnswerValid() && !isLoading }"
        >
          <div class="btn-glow"></div>
          <span class="btn-icon">📝</span>
          <span class="btn-text">提交答案</span>
        </button>
        
        <button 
          @click="retryQuestion"
          :disabled="isLoading"
          class="retry-btn"
          title="重新生成这个问题"
        >
          <span class="btn-icon">🔄</span>
          <span class="btn-text">重试问题</span>
        </button>
        
        <button 
          @click="generatePrompt"
          class="reset-btn"
          :disabled="isLoading || isGeneratingPrompt"
        >
          <span class="btn-icon">✨</span>
          <span class="btn-text">生成提示词</span>
        </button>
      </div>
      
    </div>

    <!-- 提示词结果展示 -->
    <div v-if="showPromptResult && promptResult" class="prompt-result">
      <div class="prompt-result-container">
        <div class="prompt-result-header">
          <h3 class="result-title">生成的提示词</h3>
          <div class="header-actions">
            <button @click="copyPrompt" class="copy-btn" :class="{ copied: copySuccess }">
              <span class="btn-icon">{{ copySuccess ? '✅' : '📋' }}</span>
              <span class="btn-text">{{ copySuccess ? '已复制' : '复制' }}</span>
            </button>
            <button @click="closePromptResult" class="close-btn" title="关闭">
              <span class="btn-icon">✕</span>
            </button>
          </div>
        </div>
        
        <div class="prompt-content">
          <pre class="prompt-text">{{ promptResult }}</pre>
        </div>
        
        <div class="prompt-actions">
          <button @click="regeneratePrompt" class="regenerate-btn" :disabled="isLoading">
            <span class="btn-icon">🔄</span>
            <span class="btn-text">重新生成</span>
          </button>
          
          <button @click="continueChat" class="continue-btn">
            <span class="btn-icon">💬</span>
            <span class="btn-text">继续问答</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <LoadingAnimation 
      v-if="isLoading" 
      :type="loadingType"
      :duration="3000"
    />
    
    <!-- 重试原因输入对话框 -->
    <div v-if="showRetryDialog" class="retry-dialog-overlay">
      <div class="retry-dialog">
        <div class="retry-dialog-header">
          <h3>重试原因</h3>
          <button @click="cancelRetry" class="close-btn">×</button>
        </div>
        <div class="retry-dialog-content">
          <p>请说明为什么要重新生成这个问题：</p>
          <textarea 
            v-model="retryReason" 
            placeholder="例如：问题不够具体、需要更详细的选项、问题与我的需求不符等..."
            class="retry-reason-input"
            rows="4"
          ></textarea>
        </div>
        <div class="retry-dialog-actions">
          <button @click="cancelRetry" class="cancel-btn">取消</button>
          <button @click="confirmRetry" class="confirm-btn">确认重试</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, computed, watch, getCurrentInstance } from 'vue'
import LoadingAnimation from './LoadingAnimation.vue'
import SingleChoiceOptions from './SingleChoiceOptions.vue'
import MultipleChoiceOptions from './MultipleChoiceOptions.vue'
import FormField from './FormField.vue'
import { generatePrompt as apiGeneratePrompt, setUserProfile } from '@/services/userInteractionApi'
import { toast } from '@/utils/toast'

// 定义问题类型接口
interface Option {
  id: string
  label: string
}

interface FormFieldData {
  id: string
  question: string
  type: 'input' | 'single' | 'multi'
  options?: Option[]
  desc?: string
  weight?: string
}

interface BaseQuestion {
  type: 'input' | 'single' | 'multi' | 'form'
  question: string
  desc?: string
  questionId?: string
}

interface InputQuestion extends BaseQuestion {
  type: 'input'
  answer?: string
}

interface SingleChoiceQuestion extends BaseQuestion {
  type: 'single'
  options: Option[]
  answer?: string[]
}

interface MultipleChoiceQuestion extends BaseQuestion {
  type: 'multi'
  options: Option[]
  answer?: string[]
}

interface FormQuestion extends BaseQuestion {
  type: 'form'
  fields: FormFieldData[]
  answer?: Array<{ id: string; value: string[] }>
}

type Question = InputQuestion | SingleChoiceQuestion | MultipleChoiceQuestion | FormQuestion

interface ConversationNode {
  id: string
  content: string
  type: 'user' | 'assistant'
  timestamp: Date
  parentId?: string
  children: string[]
  isActive: boolean
}

interface Props {
  currentQuestion?: Question | null
  isLoading?: boolean
  sessionId?: string | null
  userId?: string
  conversationTree?: Map<string, ConversationNode>
  currentNodeId?: string
  isViewingHistory?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  sendMessage: [content: string]
  submitAnswer: [answer: any]
  retryQuestion: [reason: string]
  generatePrompt: [answer: any]
  nodeSelected: [nodeId: string]
}>()

// 响应式数据
const mainInput = ref('')
const expandedAction = ref<string | null>(null)
const quickInputs = reactive({
  introduce: '',
  model: ''
})

// 答案数据
const answers = reactive({
  input: '',
  single: '',
  multiple: [] as string[],
  form: {} as Record<string, any>
})

// 引用
const mainInputRef = ref<HTMLTextAreaElement>()
const introduceInputRef = ref<HTMLInputElement>()
const modelInputRef = ref<HTMLInputElement>()

// 计算属性
const loadingType = computed(() => {
  if (!props.currentQuestion) {
    return 'thinking'
  }
  
  switch (props.currentQuestion.type) {
    case 'input':
      return 'analyzing'
    case 'single':
    case 'multi':
      return 'processing'
    case 'form':
      return 'generating'
    default:
      return 'thinking'
  }
})

// 监听问题变化，重置答案
watch(() => props.currentQuestion, (newQuestion, oldQuestion) => {
  if (newQuestion && newQuestion !== oldQuestion) {
    
    resetAnswers()
  }
}, { deep: true })

// 方法
const resetAnswers = () => {
  answers.input = ''
  answers.single = ''
  answers.multiple = []
  answers.form = {}
}

const handleMainInputKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submitMainInput()
  }
}

const submitMainInput = () => {
  if (!mainInput.value.trim() || props.isLoading) return
  
  emit('sendMessage', mainInput.value.trim())
  mainInput.value = ''
}

const toggleQuickInput = (action: string) => {
  if (expandedAction.value === action) {
    expandedAction.value = null
  } else {
    expandedAction.value = action
    nextTick(() => {
      if (action === 'introduce' && introduceInputRef.value) {
        introduceInputRef.value.focus()
      } else if (action === 'model' && modelInputRef.value) {
        modelInputRef.value.focus()
      }
    })
  }
}

const submitQuickInput = async (action: string) => {
  const content = quickInputs[action as keyof typeof quickInputs]
  if (!content.trim()) return
  
  if (action === 'introduce') {
    // 介绍自己调用setUserProfile接口
    try {
      if (!props.sessionId || !props.userId) {
        console.error('缺少sessionId或userId，无法设置用户画像')
        return
      }
      
      await setUserProfile({
         sessionId: props.sessionId,
         userId: props.userId,
         userProfile: content.trim()
       })
       
       console.log('用户画像设置成功')
       toast.success({
         title: '设置成功',
         message: '您的个人信息已保存，AI将为您提供更个性化的服务',
         duration: 3000
       })
     } catch (error) {
       console.error('设置用户画像失败:', error)
       toast.error({
         title: '设置失败',
         message: '保存个人信息时出现错误，请稍后重试',
         duration: 3000
       })
     }
  } else {
    // 其他快捷输入继续使用原有逻辑
    const prefix = action === 'model' ? '使用模型：' : ''
    emit('sendMessage', prefix + content.trim())
  }
  
  quickInputs[action as keyof typeof quickInputs] = ''
  expandedAction.value = null
}

const getQuestionIcon = (type: string) => {
  const icons = {
    input: '✏️',
    single: '🔘',
    multi: '☑️',
    form: '📋'
  }
  return icons[type as keyof typeof icons] || '❓'
}

const formatTimestamp = (timestamp: Date) => {
  const now = new Date()
  const diff = now.getTime() - timestamp.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return timestamp.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化AI消息内容
const formatAIMessage = (content: string) => {
  if (!content) return ''
  
  let formatted = content
    // 处理Markdown标题
    .replace(/^### (.+)$/gm, '<h3 class="ai-h3">$1</h3>')
    .replace(/^## (.+)$/gm, '<h2 class="ai-h2">$1</h2>')
    .replace(/^# (.+)$/gm, '<h1 class="ai-h1">$1</h1>')
    // 处理【标题】格式
    .replace(/【([^】]+)】/g, '<div class="ai-section-title">$1</div>')
    // 处理粗体
    .replace(/\*\*([^*]+)\*\*/g, '<strong class="ai-bold">$1</strong>')
    // 处理代码块
    .replace(/```([\s\S]*?)```/g, '<pre class="ai-code-block"><code>$1</code></pre>')
    // 处理行内代码
    .replace(/`([^`]+)`/g, '<code class="ai-inline-code">$1</code>')
    // 处理有序列表
    .replace(/^(\d+\.)\s+(.+)$/gm, '<div class="ai-list-item"><span class="ai-list-number">$1</span><span class="ai-list-content">$2</span></div>')
    // 处理无序列表
    .replace(/^[-•*]\s+(.+)$/gm, '<div class="ai-bullet-item"><span class="ai-bullet">•</span><span class="ai-list-content">$1</span></div>')
    // 处理段落分隔
    .replace(/\n\n/g, '</div><div class="ai-paragraph">')
    .replace(/\n/g, '<br>')
  
  // 包装在段落容器中
  return `<div class="ai-paragraph">${formatted}</div>`
}

// 检查内容是否包含结构化内容
const containsStructuredContent = (content: string) => {
  // 检查是否包含选择题格式
  return /[A-D]\)\s+/.test(content) || /\d+\.\s+/.test(content) || /请选择|请回复/.test(content)
}

// 从内容中提取问题结构
const extractQuestionFromContent = (content: string) => {
  // 简单的选择题提取逻辑
  const optionMatches = content.match(/([A-D])\)\s+([^\n]+)/g)
  if (optionMatches && optionMatches.length >= 2) {
    const options = optionMatches.map((match, index) => {
      const [, letter, text] = match.match(/([A-D])\)\s+(.+)/) || []
      return {
        id: letter || String.fromCharCode(65 + index),
        label: text || match,
        value: letter || String.fromCharCode(65 + index)
      }
    })
    
    return {
      type: 'single',
      options
    }
  }
  
  return null
}

// 嵌入式答案状态
const embeddedAnswers = ref<Record<string, any>>({})

// 处理嵌入式答案
const handleEmbeddedAnswer = (nodeId: string, answer: any) => {
  embeddedAnswers.value[nodeId] = answer
  console.log('嵌入式答案更新:', nodeId, answer)
  // 这里可以添加自动提交逻辑或其他处理
}

const handleFormFieldChange = (fieldId: string, value: any) => {
  // 表单字段变化处理，已通过v-model自动处理
  
}

const handleAnswerKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && event.ctrlKey) {
    event.preventDefault()
    submitAnswer()
  }
}

const isAnswerValid = () => {
  if (!props.currentQuestion) return false
  
  switch (props.currentQuestion.type) {
    case 'input':
      return answers.input.trim().length > 0
    case 'single':
      return answers.single.length > 0
    case 'multi':
      return answers.multiple.length > 0
    case 'form':
      return props.currentQuestion.fields.every(field => {
        const answer = answers.form[field.id]
        if (field.type === 'input') {
          return answer && answer.trim().length > 0
        } else if (field.type === 'single') {
          return answer && answer.length > 0
        } else if (field.type === 'multi') {
          return answer && Array.isArray(answer) && answer.length > 0
        }
        return false
      })
    default:
      return false
  }
}

const submitAnswer = () => {
  if (!isAnswerValid() || props.isLoading) return
  
  let answerData: any
  
  switch (props.currentQuestion!.type) {
    case 'input':
      answerData = answers.input.trim()
      break
    case 'single':
      answerData = [answers.single]
      break
    case 'multi':
      answerData = [...answers.multiple]
      break
    case 'form':
      answerData = props.currentQuestion!.fields.map(field => ({
        id: field.id,
        value: field.type === 'input' 
          ? [answers.form[field.id]] 
          : field.type === 'single'
          ? [answers.form[field.id]]
          : answers.form[field.id] || []
      }))
      break
  }
  
  emit('submitAnswer', answerData)
}

// 重试相关状态
const showRetryDialog = ref(false)
const retryReason = ref('')

// 提示词相关状态
const promptResult = ref<string>('')
const copySuccess = ref(false)
const showPromptResult = ref(false)

// 监听promptResult变化
watch(() => promptResult.value, (newValue, oldValue) => {
  // 可以在这里添加必要的响应式逻辑
})

const retryQuestion = () => {
  // 显示重试原因输入对话框
  showRetryDialog.value = true
  retryReason.value = ''
}

const confirmRetry = () => {
  // 发送重试事件，包含用户输入的原因
  emit('retryQuestion', retryReason.value || '用户要求重新生成问题')
  showRetryDialog.value = false
  retryReason.value = ''
}

const cancelRetry = () => {
  showRetryDialog.value = false
  retryReason.value = ''
}

const resetQuestion = () => {
  // 重置所有答案
  answers.input = ''
  answers.single = ''
  answers.multiple = []
  answers.form = {}
  
  // 发送重置信号
  emit('sendMessage', '重新开始')
}

// 防抖相关
let generatePromptTimeout: NodeJS.Timeout | null = null
const isGeneratingPrompt = ref(false)

// 提示词相关方法
const generatePrompt = async () => {
  // 防抖：如果正在生成或者有待处理的生成请求，则忽略
  if (isGeneratingPrompt.value || generatePromptTimeout) {
    console.log('QuestionRenderer: 生成提示词请求被防抖机制拦截');
    return;
  }
  
  // 设置防抖标志
  isGeneratingPrompt.value = true;
  
  console.log('QuestionRenderer: 开始生成提示词', {
    currentQuestion: props.currentQuestion,
    isLoading: props.isLoading,
    isAnswerValid: isAnswerValid(),
    inputAnswer: answers.input,
    quickInputs: quickInputs,
    mainInput: mainInput.value
  });
  
  try {
    // 检查是否有输入内容
    let answerData: any = null;
    
    if (props.currentQuestion) {
      // 如果有当前问题，检查是否可以基于历史数据生成
      // 在历史查看模式下，即使sessionId为null也应该允许生成（因为已有qaTree数据）
      const canGenerateFromHistory = props.isViewingHistory;
      console.log('QuestionRenderer: 历史模式检测', {
        isViewingHistory: props.isViewingHistory,
        sessionId: props.sessionId,
        canGenerateFromHistory: canGenerateFromHistory,
        isAnswerValid: isAnswerValid()
      });
      
      if (!canGenerateFromHistory && (!isAnswerValid() || props.isLoading)) {
        console.log('QuestionRenderer: 答案无效或正在加载中，取消生成提示词');
        return;
      }
      
      // 如果可以基于历史数据生成且用户没有回答，则不传递answer
      if (canGenerateFromHistory && !isAnswerValid()) {
        answerData = null; // 不传递answer，让后端基于现有qaTree生成
        console.log('QuestionRenderer: 基于历史数据生成提示词（无用户回答）');
      } else {
      
      switch (props.currentQuestion.type) {
        case 'input':
          answerData = answers.input.trim()
          break
        case 'single':
          answerData = [answers.single]
          break
        case 'multi':
          answerData = [...answers.multiple]
          break
        case 'form':
          answerData = props.currentQuestion.fields.map(field => ({
            id: field.id,
            value: field.type === 'input' 
              ? [answers.form[field.id]] 
              : field.type === 'single'
              ? [answers.form[field.id]]
              : answers.form[field.id] || []
          }))
          break
      }
      } // 结束 else 分支
    } else {
      // 如果没有当前问题，检查是否在历史查看模式
      if (props.isViewingHistory && props.sessionId) {
        // 在历史查看模式且有sessionId，说明有qaTree数据，可以生成提示词
        answerData = 'HISTORY_MODE'; // 特殊标识，表示基于历史数据生成
        console.log('QuestionRenderer: 基于历史会话数据生成提示词', { sessionId: props.sessionId });
      } else {
        // 如果不在历史模式，检查快速输入或主输入框
        if (quickInputs.introduce && quickInputs.introduce.trim().length > 0) {
          answerData = '自我介绍：' + quickInputs.introduce.trim();
        } else if (quickInputs.model && quickInputs.model.trim().length > 0) {
          answerData = '使用模型：' + quickInputs.model.trim();
        } else if (mainInput.value && mainInput.value.trim().length > 0) {
          answerData = mainInput.value.trim();
        } else {
          console.log('QuestionRenderer: 没有输入内容，取消生成提示词');
          toast.error('请输入内容后再生成提示词。');
          return;
        }
      }
    }
    
    if (answerData === null) {
      // answerData为null是有效的，表示基于历史数据生成
      console.log('QuestionRenderer: 基于历史数据生成提示词，answerData为null是正常的');
    } else if (!answerData) {
      console.log('QuestionRenderer: 没有有效的输入内容，取消生成提示词');
      toast.error('请输入内容后再生成提示词。');
      return;
    }
    
    console.log('QuestionRenderer: 准备发送生成提示词事件', { answerData });
    // 同时触发事件给父组件
    emit('generatePrompt', answerData)
    
    // 设置防抖超时，防止短时间内重复调用
    generatePromptTimeout = setTimeout(() => {
      generatePromptTimeout = null;
    }, 1000); // 1秒防抖
    
  } catch (error) {
    console.error('生成提示词失败:', error)
    toast.error('生成提示词失败，请重试。');
  } finally {
    // 重置防抖标志
    isGeneratingPrompt.value = false;
  }
}

const regeneratePrompt = async () => {
  promptResult.value = ''
  await generatePrompt()
}

const continueChat = () => {
  promptResult.value = ''
  showPromptResult.value = false
  // 继续问答功能暂时不实现
  
}

const copyPrompt = async () => {
  try {
    await navigator.clipboard.writeText(promptResult.value)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (error) {
    console.error('复制失败:', error)
    // 降级方案：使用传统方法复制
    const textArea = document.createElement('textarea')
    textArea.value = promptResult.value
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      copySuccess.value = true
      setTimeout(() => {
        copySuccess.value = false
      }, 2000)
    } catch (fallbackError) {
      console.error('降级复制也失败:', fallbackError)
    }
    document.body.removeChild(textArea)
  }
}

// 关闭提示词结果
const closePromptResult = () => {
  promptResult.value = ''
  showPromptResult.value = false
  copySuccess.value = false
}

// 检查内容是否为结构化问题数据
const isStructuredQuestion = (content: string): boolean => {
  try {
    const parsed = JSON.parse(content)
    // 检查是否为单个问题对象
    if (parsed && typeof parsed === 'object' && parsed.type && parsed.question) {
      return true
    }
    // 检查是否为问题数组（表单类型）
    if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].type && parsed[0].question) {
      return true
    }
    return false
  } catch (e) {
    // 如果直接解析失败，尝试检查"描述文本:[JSON数组]"格式
    try {
      const colonIndex = content.indexOf(':')
      if (colonIndex > 0) {
        const jsonPart = content.substring(colonIndex + 1).trim()
        const parsed = JSON.parse(jsonPart)
        if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].type && parsed[0].question) {
          return true
        }
      }
      return false
    } catch (e2) {
      return false
    }
  }
}

// 解析结构化问题数据
const parseStructuredQuestion = (content: string): Question | null => {
  try {
    // 首先尝试直接解析JSON
    const parsed = JSON.parse(content)
    // 如果是单个问题对象
    if (parsed && typeof parsed === 'object' && parsed.type && parsed.question) {
      return parsed as Question
    }
    // 如果是问题数组，转换为表单问题格式
    if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].type && parsed[0].question) {
      return {
        type: 'form',
        question: '为了帮你定制最合适的提示词，请补充以下关键信息：',
        desc: '',
        fields: parsed.map((item: any) => ({
          id: item.id,
          question: item.question,
          type: item.type,
          options: item.options || []
        }))
      } as Question
    }
    return null
  } catch (e) {
    // 如果直接解析失败，尝试解析"描述文本:[JSON数组]"格式
    try {
      const colonIndex = content.indexOf(':')
      if (colonIndex > 0) {
        const descriptionText = content.substring(0, colonIndex).trim()
        const jsonPart = content.substring(colonIndex + 1).trim()
        
        const parsed = JSON.parse(jsonPart)
        if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].type && parsed[0].question) {
          return {
            type: 'form',
            question: descriptionText,
            desc: '',
            fields: parsed.map((item: any) => ({
              id: item.id,
              question: item.question,
              type: item.type,
              options: item.options || []
            }))
          } as Question
        }
      }
      return null
    } catch (e2) {
      return null
    }
  }
}

// 获取当前组件实例
const instance = getCurrentInstance()

// 暴露设置提示词结果的方法
const setPromptResult = (result: string) => {
  // 设置提示词内容和显示状态
  promptResult.value = result
  showPromptResult.value = true
  
  // 强制更新组件以确保响应式更新
  if (instance) {
    instance.proxy?.$forceUpdate()
  }
  
  // 使用nextTick确保DOM更新完成
  nextTick(() => {
    // DOM更新完成后的回调
  })
}

// 暴露方法给父组件
defineExpose({
  setPromptResult
})
</script>

<style scoped>
/* 主容器 */
.question-renderer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, rgba(10, 10, 10, 0.95) 0%, rgba(20, 20, 20, 0.95) 100%);
  backdrop-filter: blur(20px);
  position: relative;
  overflow: hidden;
}

/* 动态背景 */
.question-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.background-particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.bg-particle {
  position: absolute;
  background: linear-gradient(45deg, #d4af37, #ffd700);
  border-radius: 50%;
  opacity: 0.1;
  animation: bg-float 15s ease-in-out infinite;
}

.bg-particle:nth-child(1) { width: 4px; height: 4px; top: 10%; left: 10%; animation-delay: 0s; }
.bg-particle:nth-child(2) { width: 6px; height: 6px; top: 20%; left: 80%; animation-delay: -2s; }
.bg-particle:nth-child(3) { width: 3px; height: 3px; top: 70%; right: 15%; animation-delay: -4s; }
.bg-particle:nth-child(4) { width: 5px; height: 5px; bottom: 30%; left: 30%; animation-delay: -6s; }
.bg-particle:nth-child(5) { width: 4px; height: 4px; top: 50%; right: 25%; animation-delay: -8s; }
.bg-particle:nth-child(6) { width: 7px; height: 7px; top: 30%; right: 60%; animation-delay: -10s; }
.bg-particle:nth-child(7) { width: 3px; height: 3px; bottom: 60%; right: 10%; animation-delay: -12s; }
.bg-particle:nth-child(8) { width: 5px; height: 5px; bottom: 20%; left: 60%; animation-delay: -14s; }
.bg-particle:nth-child(9) { width: 4px; height: 4px; top: 80%; left: 20%; animation-delay: -16s; }
.bg-particle:nth-child(10) { width: 6px; height: 6px; top: 40%; left: 70%; animation-delay: -18s; }
.bg-particle:nth-child(11) { width: 3px; height: 3px; bottom: 80%; right: 40%; animation-delay: -20s; }
.bg-particle:nth-child(12) { width: 5px; height: 5px; top: 60%; left: 50%; animation-delay: -22s; }

@keyframes bg-float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.1;
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
    opacity: 0.3;
  }
}

/* 初始状态样式 */
.initial-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  position: relative;
  z-index: 2;
  gap: 48px;
}

.welcome-section {
  text-align: center;
  margin-bottom: 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #d4af37 0%, #f4d03f 50%, #d4af37 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  position: relative;
  box-shadow: 
    0 12px 32px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.icon-glow {
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  background: linear-gradient(45deg, #d4af37, transparent, #f4d03f);
  border-radius: 50%;
  opacity: 0.6;
  animation: icon-pulse 3s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.05); }
}

.icon {
  font-size: 36px;
  z-index: 2;
  position: relative;
}

.welcome-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #ffffff 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 16px;
  color: #a0a0a0;
  margin: 0;
  font-weight: 400;
}

/* 主输入区域 */
.main-input-section {
  width: 100%;
  max-width: 800px;
}

.input-container {
  width: 100%;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 24px;
  padding: 32px;
  position: relative;
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.input-wrapper:focus-within {
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 
    0 16px 48px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.input-glow {
  position: absolute;
  top: -1px;
  left: -1px;
  right: -1px;
  bottom: -1px;
  background: linear-gradient(45deg, rgba(212, 175, 55, 0.3), transparent, rgba(212, 175, 55, 0.3));
  border-radius: 24px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.input-wrapper:focus-within .input-glow {
  opacity: 1;
}

.main-input {
  width: 100%;
  min-height: 120px;
  background: transparent;
  border: none;
  outline: none;
  resize: vertical;
  font-family: inherit;
  font-size: 18px;
  line-height: 1.6;
  color: #e8e8e8;
  font-weight: 400;
}

.main-input::placeholder {
  color: #666;
  font-weight: 400;
}

.main-submit-btn {
  align-self: flex-end;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 32px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  color: #888;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.main-submit-btn.active {
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  border-color: #d4af37;
  color: #1a1a1a;
  box-shadow: 
    0 8px 24px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.main-submit-btn.active:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 12px 32px rgba(212, 175, 55, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

/* 快捷操作区域 */
.quick-actions {
  display: flex;
  gap: 24px;
  width: 100%;
  max-width: 600px;
}

/* 生成提示词区域样式 */
.generate-prompt-section {
  margin-top: 32px;
  padding: 24px;
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.prompt-divider {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.3), transparent);
}

.divider-text {
  color: #d4af37;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 6px 12px;
  background: rgba(10, 10, 10, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.generate-prompt-btn {
  position: relative;
  width: 100%;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.generate-prompt-btn:hover:not(:disabled) {
  border-color: rgba(212, 175, 55, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.15);
}

.generate-prompt-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-glow {
  position: absolute;
  top: -1px;
  left: -1px;
  right: -1px;
  bottom: -1px;
  background: linear-gradient(45deg, #d4af37, #f4d03f, #d4af37, #f4d03f);
  background-size: 400% 400%;
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  animation: btn-glow-rotate 3s linear infinite;
  z-index: -1;
}

.generate-prompt-btn:hover:not(:disabled) .btn-glow {
  opacity: 0.2;
}

@keyframes btn-glow-rotate {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.btn-content {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.btn-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(212, 175, 55, 0.1);
  border-radius: 10px;
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.btn-icon {
  font-size: 20px;
  filter: drop-shadow(0 0 4px rgba(212, 175, 55, 0.6));
}

.btn-text-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.btn-title {
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.btn-desc {
  color: #b8b8b8;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.3;
}

.btn-arrow {
  color: #d4af37;
  font-size: 18px;
  font-weight: bold;
  transition: transform 0.3s ease;
}

.generate-prompt-btn:hover:not(:disabled) .btn-arrow {
  transform: translateX(4px);
}

.action-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.quick-btn:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(244, 208, 63, 0.05));
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(212, 175, 55, 0.2);
}

.quick-btn.expanded {
  border-color: rgba(212, 175, 55, 0.5);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(244, 208, 63, 0.1));
}

.quick-input-container {
  animation: expand-down 0.3s ease-out;
}

@keyframes expand-down {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.quick-input-wrapper {
  display: flex;
  gap: 12px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.quick-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #e8e8e8;
  font-size: 14px;
  font-weight: 400;
}

.quick-input::placeholder {
  color: #666;
}

.quick-submit-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  border: none;
  border-radius: 8px;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-submit-btn:hover {
  transform: scale(1.05);
}

.quick-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 问答状态样式 */
.question-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 40px;
  position: relative;
  z-index: 2;
  gap: 32px;
  overflow-y: auto;
}

.question-header {
  text-align: center;
  margin-bottom: 20px;
}

.question-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.title-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.3);
}

.question-title h3 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #e8e8e8;
}

.question-desc {
  font-size: 16px;
  color: #a0a0a0;
  margin: 0;
  line-height: 1.6;
}

/* 输入框问题样式 */
.input-question {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.answer-input-container {
  width: 100%;
}

.answer-input-wrapper {
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 20px;
  padding: 24px;
  position: relative;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
}

.answer-input-wrapper:focus-within {
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 0 0 0 1px rgba(212, 175, 55, 0.3);
}

.answer-input {
  width: 100%;
  min-height: 120px;
  background: transparent;
  border: none;
  outline: none;
  resize: vertical;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.6;
  color: #e8e8e8;
  font-weight: 400;
}

.answer-input::placeholder {
  color: #666;
}

/* 选择题样式 */
.single-choice-question,
.multiple-choice-question {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.option-item:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(244, 208, 63, 0.05));
  transform: translateX(4px);
}

.option-item.selected {
  border-color: rgba(212, 175, 55, 0.6);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(244, 208, 63, 0.1));
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.2);
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

.option-checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(212, 175, 55, 0.4);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.option-item.selected .option-checkbox {
  border-color: #d4af37;
  background: linear-gradient(135deg, #d4af37, #f4d03f);
}

.checkbox-check {
  font-size: 12px;
  color: #1a1a1a;
  font-weight: 700;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.option-item.selected .checkbox-check {
  opacity: 1;
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

/* 表单问题样式 */
.form-question {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.form-fields {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
}

/* 根据字段数量调整布局 */
.form-fields:has(.form-field:nth-child(1):nth-last-child(1)) {
  grid-template-columns: 1fr;
  max-width: 600px;
  margin: 0 auto;
}

.form-fields:has(.form-field:nth-child(2):nth-last-child(1)) {
  grid-template-columns: repeat(2, 1fr);
}

.form-fields:has(.form-field:nth-child(3):nth-last-child(1)) {
  grid-template-columns: repeat(3, 1fr);
}

.form-fields:has(.form-field:nth-child(4):nth-last-child(1)) {
  grid-template-columns: repeat(2, 1fr);
}

.form-fields:has(.form-field:nth-child(5):nth-last-child(1)) {
  grid-template-columns: repeat(3, 1fr);
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .form-fields {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  }
  
  .form-fields:has(.form-field:nth-child(3):nth-last-child(1)),
  .form-fields:has(.form-field:nth-child(4):nth-last-child(1)),
  .form-fields:has(.form-field:nth-child(5):nth-last-child(1)) {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .form-fields {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

.form-field {
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 20px;
  padding: 20px;
  backdrop-filter: blur(20px);
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.field-header {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.field-label {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #e8e8e8;
  margin-bottom: 6px;
  line-height: 1.3;
}

.field-desc {
  font-size: 13px;
  color: #a0a0a0;
  margin: 0;
  line-height: 1.4;
}

.field-input-wrapper {
  background: rgba(10, 10, 10, 0.5);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.field-input-wrapper:focus-within {
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 0 0 0 1px rgba(212, 175, 55, 0.3);
}

.field-input-control {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 400;
}

.field-input-control::placeholder {
  color: #666;
}

.field-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(10, 10, 10, 0.5);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.field-option:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: rgba(212, 175, 55, 0.05);
}

.field-option.selected {
  border-color: rgba(212, 175, 55, 0.6);
  background: rgba(212, 175, 55, 0.1);
}

.option-radio.small {
  width: 16px;
  height: 16px;
}

.option-radio.small .radio-dot {
  width: 6px;
  height: 6px;
}

.option-checkbox.small {
  width: 16px;
  height: 16px;
}

.option-checkbox.small .checkbox-check {
  font-size: 10px;
}

/* 操作按钮 */
.question-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.submit-answer-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 32px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  color: #888;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.submit-answer-btn.active {
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  border-color: #d4af37;
  color: #1a1a1a;
  box-shadow: 
    0 8px 24px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.submit-answer-btn.active:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 12px 32px rgba(212, 175, 55, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(255, 165, 0, 0.2);
  border-radius: 16px;
  color: #ffb366;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.retry-btn:hover {
  border-color: rgba(255, 165, 0, 0.4);
  color: #ffffff;
  transform: translateY(-1px);
  background: rgba(255, 165, 0, 0.1);
}

.retry-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  color: #a0a0a0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.reset-btn:hover {
  border-color: rgba(255, 255, 255, 0.2);
  color: #e8e8e8;
  transform: translateY(-1px);
}

.btn-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(212, 175, 55, 0.2), transparent, rgba(212, 175, 55, 0.2));
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.submit-answer-btn.active .btn-glow {
  opacity: 1;
  animation: btn-shimmer 2s ease-in-out infinite;
}

@keyframes btn-shimmer {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.btn-icon {
  font-size: 16px;
  position: relative;
  z-index: 2;
}

.btn-text {
  position: relative;
  z-index: 2;
}

/* 加载状态已移至LoadingAnimation组件 */

/* 滚动条样式 */
.question-state::-webkit-scrollbar {
  width: 6px;
}

.question-state::-webkit-scrollbar-track {
  background: rgba(15, 15, 15, 0.3);
  border-radius: 3px;
}

.question-state::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.3), rgba(212, 175, 55, 0.6));
  border-radius: 3px;
  transition: all 0.3s ease;
}

.question-state::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.6), rgba(212, 175, 55, 0.8));
}

/* 响应式设计 */
@media (max-width: 768px) {
  .initial-state {
    padding: 24px;
    gap: 32px;
  }
  
  .welcome-icon {
    width: 64px;
    height: 64px;
  }
  
  .icon {
    font-size: 28px;
  }
  
  .welcome-title {
    font-size: 24px;
  }
  
  .input-wrapper {
    padding: 24px;
  }
  
  .main-input {
    font-size: 16px;
    min-height: 100px;
  }
  
  .quick-actions {
    flex-direction: column;
    gap: 16px;
  }
  
  .question-state {
    padding: 24px;
    gap: 24px;
  }
  
  .question-title {
    flex-direction: column;
    gap: 12px;
  }
  
  .title-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .question-title h3 {
    font-size: 20px;
  }
  
  .form-fields {
    gap: 24px;
  }
  
  .form-field {
    padding: 20px;
  }
}

@media (max-width: 480px) {
  .initial-state {
    padding: 16px;
  }
  
  .input-wrapper {
    padding: 20px;
  }
  
  .main-submit-btn {
    padding: 12px 24px;
    font-size: 14px;
  }
  
  .quick-btn {
    padding: 16px 20px;
    font-size: 14px;
  }
  
  .question-state {
    padding: 16px;
  }
  
  .option-item {
    padding: 16px 20px;
  }
  
  .form-field {
    padding: 16px;
  }
  
  .question-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .submit-answer-btn,
  .reset-btn {
    width: 100%;
    justify-content: center;
  }
}

/* 提示词结果展示样式 */
.prompt-result {
  margin-top: 32px;
  padding: 0;
  animation: slideInUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.prompt-result-container {
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.95), rgba(25, 25, 25, 0.95));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 24px;
  padding: 32px;
  width: 100%;
  backdrop-filter: blur(20px);
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;
}

.prompt-result-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #d4af37, transparent);
  animation: shimmer 2s infinite;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.prompt-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 50%;
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.close-btn:hover {
  border-color: rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  transform: scale(1.05);
}

.close-btn .btn-icon {
  font-size: 18px;
  line-height: 1;
}

.result-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(15, 15, 15, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  color: #e8e8e8;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.copy-btn:hover {
  border-color: rgba(212, 175, 55, 0.4);
  background: rgba(25, 25, 25, 0.8);
  transform: translateY(-1px);
}

.copy-btn.copied {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  border-color: #22c55e;
  color: white;
}

.prompt-content {
  margin-bottom: 32px;
}

.prompt-text {
  background: rgba(10, 10, 10, 0.8);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  padding: 24px;
  color: #e8e8e8;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  max-height: 400px;
  overflow-y: auto;
  backdrop-filter: blur(10px);
}

.prompt-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.regenerate-btn, .continue-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  backdrop-filter: blur(10px);
}

.regenerate-btn {
  background: rgba(15, 15, 15, 0.8);
  border-color: rgba(212, 175, 55, 0.2);
  color: #e8e8e8;
}

.regenerate-btn:hover:not(:disabled) {
  border-color: rgba(212, 175, 55, 0.4);
  background: rgba(25, 25, 25, 0.8);
  transform: translateY(-2px);
}

.regenerate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.continue-btn {
  background: linear-gradient(135deg, #d4af37, #f4d03f);
  color: #1a1a1a;
  border-color: #d4af37;
}

.continue-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.4);
}

/* 重试对话框样式 */
.retry-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.retry-dialog {
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.95), rgba(25, 25, 25, 0.95));
  border: 1px solid rgba(255, 165, 0, 0.3);
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.retry-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 165, 0, 0.2);
}

.retry-dialog-header h3 {
  margin: 0;
  color: #ffb366;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: #888;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 165, 0, 0.1);
  color: #ffb366;
}

.retry-dialog-content {
  padding: 24px;
}

.retry-dialog-content p {
  margin: 0 0 16px 0;
  color: #e8e8e8;
  font-size: 14px;
  line-height: 1.5;
}

.retry-reason-input {
  width: 100%;
  background: rgba(10, 10, 10, 0.8);
  border: 1px solid rgba(255, 165, 0, 0.3);
  border-radius: 12px;
  padding: 12px 16px;
  color: #e8e8e8;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  min-height: 100px;
  transition: all 0.3s ease;
}

.retry-reason-input:focus {
  outline: none;
  border-color: rgba(255, 165, 0, 0.6);
  box-shadow: 0 0 0 2px rgba(255, 165, 0, 0.1);
}

.retry-reason-input::placeholder {
  color: #666;
}

.retry-dialog-actions {
  display: flex;
  gap: 12px;
  padding: 0 24px 24px 24px;
  justify-content: flex-end;
}

.cancel-btn, .confirm-btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid;
}

.cancel-btn {
  background: rgba(128, 128, 128, 0.1);
  border-color: rgba(128, 128, 128, 0.3);
  color: #cccccc;
}

/* 对话历史样式 */
.conversation-history {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.history-header {
  text-align: center;
  margin-bottom: 32px;
}

.history-header h3 {
  color: #e8e8e8;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.history-header p {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.conversation-nodes {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.conversation-node {
  background: rgba(20, 20, 20, 0.8);
  border: 1px solid rgba(255, 165, 0, 0.2);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.conversation-node:hover {
  background: rgba(30, 30, 30, 0.9);
  border-color: rgba(255, 165, 0, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 165, 0, 0.1);
}

.conversation-node.active-node {
  background: rgba(255, 165, 0, 0.1);
  border-color: rgba(255, 165, 0, 0.6);
  box-shadow: 0 0 0 2px rgba(255, 165, 0, 0.2);
}

.conversation-node.user-node {
  border-left: 4px solid #4CAF50;
}

.conversation-node.assistant-node {
  border-left: 4px solid #2196F3;
}

.node-content {
  color: #e8e8e8;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
  word-wrap: break-word;
}

.node-timestamp {
  color: #666;
  font-size: 12px;
  text-align: right;
}

.cancel-btn:hover {
  background: rgba(128, 128, 128, 0.2);
  border-color: rgba(128, 128, 128, 0.5);
  color: #ffffff;
}

.confirm-btn {
  background: linear-gradient(135deg, rgba(255, 165, 0, 0.2), rgba(255, 140, 0, 0.2));
  border-color: rgba(255, 165, 0, 0.5);
  color: #ffb366;
}

.confirm-btn:hover {
  background: linear-gradient(135deg, rgba(255, 165, 0, 0.3), rgba(255, 140, 0, 0.3));
  border-color: rgba(255, 165, 0, 0.7);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(255, 165, 0, 0.3);
}

/* 空历史状态样式 */
.empty-history {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
  opacity: 0.6;
}

.empty-history p {
  margin: 8px 0;
  font-size: 16px;
}

.empty-hint {
  font-size: 14px !important;
  color: #666 !important;
}

/* 聊天消息样式 */
.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.chat-message {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chat-message:hover {
  transform: translateY(-1px);
}

.chat-message.active-message .message-bubble {
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.5);
}

/* AI消息（左侧） */
.message-left {
  align-items: flex-start;
}

.message-left .message-bubble {
  background: transparent; /* AI消息的背景由ai-message类控制 */
  border: none;
  border-radius: 18px 18px 18px 4px;
  max-width: 85%; /* 增加最大宽度以适应长文本 */
  margin-right: auto;
  padding: 0; /* 移除padding，由ai-message控制 */
}

/* 用户消息（右侧） */
.message-right {
  align-items: flex-end;
}

.message-right .message-bubble {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(184, 134, 11, 0.2));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 18px 18px 4px 18px;
  max-width: 80%;
  margin-left: auto;
}

.message-bubble {
  padding: 12px 16px;
  backdrop-filter: blur(10px);
  transition: all 0.2s ease;
}

.message-text {
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
}

.ai-message {
  color: #f0f0f0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.7;
  letter-spacing: 0.2px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(30, 30, 30, 0.95), rgba(20, 20, 20, 0.98));
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  max-width: 100%;
  overflow-wrap: break-word;
  word-break: break-word;
  position: relative;
}

.ai-message::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.3), transparent);
  border-radius: 16px 16px 0 0;
}

/* AI消息格式化样式 */
.ai-h1 {
  font-size: 20px;
  font-weight: 800;
  color: #f4d03f;
  margin: 20px 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid rgba(212, 175, 55, 0.3);
}

.ai-h2 {
  font-size: 18px;
  font-weight: 700;
  color: #f4d03f;
  margin: 18px 0 14px 0;
  padding-left: 12px;
  border-left: 3px solid #d4af37;
}

.ai-h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e8e8e8;
  margin: 16px 0 12px 0;
}

.ai-section-title {
  font-size: 16px;
  font-weight: 700;
  color: #f4d03f;
  margin: 16px 0 12px 0;
  padding: 8px 12px;
  background: rgba(212, 175, 55, 0.1);
  border-left: 3px solid #d4af37;
  border-radius: 4px;
}

.ai-paragraph {
  margin: 12px 0;
  line-height: 1.7;
  color: #e8e8e8;
}

.ai-bold {
  font-weight: 600;
  color: #f4d03f;
}

.ai-code-block {
  background: rgba(20, 20, 20, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.ai-code-block code {
  color: #e8e8e8;
  background: none;
  padding: 0;
  border: none;
}

.ai-inline-code {
  background: rgba(212, 175, 55, 0.15);
  color: #f4d03f;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.ai-list-item {
  display: flex;
  align-items: flex-start;
  margin: 8px 0;
  padding-left: 8px;
}

.ai-list-number {
  color: #d4af37;
  font-weight: 600;
  margin-right: 12px;
  min-width: 24px;
  flex-shrink: 0;
}

.ai-list-content {
  flex: 1;
  line-height: 1.6;
}

.ai-bullet-item {
  display: flex;
  align-items: flex-start;
  margin: 8px 0;
  padding-left: 8px;
}

.ai-bullet {
  color: #d4af37;
  font-weight: 600;
  margin-right: 12px;
  min-width: 16px;
  flex-shrink: 0;
}

.ai-bullet-content {
  flex: 1;
  line-height: 1.6;
}

/* Markdown标题样式 */
.ai-h1 {
  font-size: 24px;
  font-weight: 800;
  color: #f4d03f;
  margin: 24px 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid rgba(212, 175, 55, 0.3);
}

.ai-h2 {
  font-size: 20px;
  font-weight: 700;
  color: #f4d03f;
  margin: 20px 0 12px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
}

.ai-h3 {
  font-size: 18px;
  font-weight: 600;
  color: #f4d03f;
  margin: 16px 0 10px 0;
}

/* 文本格式样式 */
.ai-bold {
  font-weight: 700;
  color: #ffffff;
}

/* 代码样式 */
.ai-code-block {
  background: rgba(20, 20, 20, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.5;
}

.ai-code-block code {
  color: #e8e8e8;
  background: none;
  padding: 0;
  border: none;
}

.ai-inline-code {
  background: rgba(212, 175, 55, 0.15);
  color: #f4d03f;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  border: 1px solid rgba(212, 175, 55, 0.2);
}

/* 列表内容样式 */
.ai-list-content {
  flex: 1;
  line-height: 1.6;
}

/* 嵌入式问题样式 */
.embedded-question {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.embedded-question .question-container {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 确保嵌入式选项组件的样式正确 */
.embedded-question .choice-options {
  margin: 0;
}

.embedded-question .choice-option {
  margin: 6px 0;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s ease;
}

.embedded-question .choice-option:hover {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.3);
}

.embedded-question .choice-option.selected {
  background: rgba(212, 175, 55, 0.2);
  border-color: rgba(212, 175, 55, 0.5);
  color: #f4d03f;
}

.user-message {
  color: #f0f0f0;
}

.message-time {
  font-size: 11px;
  color: #888;
  margin-top: 4px;
  opacity: 0.7;
}

.message-left .message-time {
  text-align: left;
}

.message-right .message-time {
  text-align: right;
}

/* 结构化问题聊天样式 */
.structured-question-chat {
  width: 100%;
}

.question-title-chat {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #e8e8e8;
  margin-bottom: 8px;
}

.question-icon {
  font-size: 18px;
  opacity: 0.9;
}

.question-desc-chat {
  font-size: 13px;
  color: #bbb;
  margin-bottom: 12px;
  line-height: 1.4;
}

.question-options-chat {
  margin-top: 12px;
}

.form-fields-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-preview {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.field-title {
  font-size: 13px;
  font-weight: 500;
  color: #ddd;
  margin-bottom: 6px;
}

.field-options, .options-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.option-chip {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.option-chip.single {
  background: rgba(212, 175, 55, 0.15);
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
}

.option-chip.multi {
  background: rgba(76, 175, 80, 0.15);
  border: 1px solid rgba(76, 175, 80, 0.4);
  color: #4caf50;
}

.more-options {
  font-size: 11px;
  color: #888;
  font-style: italic;
  padding: 4px 8px;
  background: rgba(128, 128, 128, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(128, 128, 128, 0.2);
}
</style>