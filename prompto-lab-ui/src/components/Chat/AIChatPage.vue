<template>
  <div class="ai-chat-page">
    <!-- 动态背景效果 -->
    <div class="dynamic-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="floating-particles">
        <div class="particle" v-for="i in 15" :key="i"></div>
      </div>
    </div>

    <!-- 左侧栏 -->
    <div class="left-sidebar">
      <div class="sidebar-content">
        <div class="sidebar-header">
          <div class="header-icon">
            <div class="icon-wrapper">
              <span class="icon">◈</span>
            </div>
          </div>
          <div class="header-text">
            <h3>AI助手</h3>
            <p>智能对话引导</p>
          </div>
        </div>
        <div class="sidebar-placeholder">
          <div class="placeholder-content">
            <div class="placeholder-icon">⚡</div>
            <p>功能面板</p>
            <span>即将推出</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间问答主页面 -->
    <div class="main-content" :style="{ width: mainContentWidth + 'px' }">
      <QuestionRenderer ref="questionRendererRef" :current-question="currentQuestion" :is-loading="isLoading" 
        :session-id="sessionId" :user-id="userId" @send-message="handleSendMessage"
        @submit-answer="handleSubmitAnswer" @retry-question="handleRetryQuestion" @generate-prompt="handleGeneratePrompt" />
    </div>

    <!-- 可拖拽的分隔条 -->
    <div v-if="false" class="resizer" @mousedown="startResize" @touchstart="startResize">
      <div class="resizer-line"></div>
      <div class="resizer-handle">
        <div class="resizer-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>

    <!-- 右侧思维导图 -->
    <div v-if="false" class="right-sidebar" :style="{ width: rightSidebarWidth + 'px' }">
      <MindMapTree :conversation-tree="conversationTree" :current-node-id="currentNodeId"
        @node-selected="handleNodeSelected" @branch-deleted="handleBranchDeleted" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import QuestionRenderer from './QuestionRenderer.vue'
import MindMapTree from './MindMapTree.vue'
import { startConversation, sendMessage, sendUserMessage, connectSSE, closeSSE, processAnswer, connectUserInteractionSSE, retryQuestion, type MessageRequest, type MessageResponse, type ConversationSession, type UnifiedAnswerRequest, type FormAnswerItem, type RetryRequest } from '@/services/conversationApi'
import { generatePrompt } from '@/services/userInteractionApi'
import { toast } from '@/utils/toast'

interface Message {
  id: string
  content: string
  type: 'user' | 'assistant'
  timestamp: Date
}

interface ConversationNode {
  id: string
  content: string
  type: 'user' | 'assistant'
  timestamp: Date
  parentId?: string
  children: string[]
  isActive: boolean
}

// 布局相关的响应式变量
const containerWidth = ref(1500) // 容器总宽度
const leftSidebarWidth = 250 // 左侧栏恢复到原始宽度 250px
const rightSidebarWidth = ref(600) // 右侧栏进一步增加到600px
const minRightSidebarWidth = 500 // 右侧栏最小宽度增加到500px
const maxRightSidebarWidth = 1000 // 右侧栏最大宽度增加到1000px
const resizerWidth = 6 // 分隔条宽度
// 计算中间内容区域宽度
const mainContentWidth = computed(() => {
  return containerWidth.value - leftSidebarWidth - rightSidebarWidth.value - resizerWidth
})

// 拖拽相关变量
const isResizing = ref(false)
const startX = ref(0)
const startRightWidth = ref(0)

// 会话状态
const session = ref<ConversationSession | null>(null)
const eventSource = ref<EventSource | null>(null)
const isConnected = ref(false)
const isInitializing = ref(false)

// SSE连接管理
const connectionTimeout = ref<NodeJS.Timeout | null>(null)
const activityTimeout = ref<NodeJS.Timeout | null>(null)
const ACTIVITY_TIMEOUT = 5 * 60 * 1000 // 5分钟不活跃超时
const lastActivityTime = ref<number>(Date.now())

// 对话树存储所有节点
const conversationTree = ref<Map<string, ConversationNode>>(new Map())
const currentNodeId = ref<string>('')
const isLoading = ref(false)

// 问题状态管理
const currentQuestion = ref<any>(null)

// 会话信息
const sessionId = ref<string | null>(null)
const userId = ref<string>('')

// 子组件引用
const questionRendererRef = ref<any>(null)

// 确保SSE连接唯一性
const ensureUniqueConnection = () => {
  if (eventSource.value) {
    // console.log('关闭现有SSE连接以确保唯一性')
    closeSSE(eventSource.value)
    eventSource.value = null
    isConnected.value = false
  }

  // 清理定时器
  if (connectionTimeout.value) {
    clearTimeout(connectionTimeout.value)
    connectionTimeout.value = null
  }
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
    activityTimeout.value = null
  }
}

// 更新活跃时间
const updateActivity = () => {
  console.log(conversationTree.value)
  lastActivityTime.value = Date.now()

  // 重置活跃超时定时器
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
  }

  activityTimeout.value = setTimeout(() => {
    console.log('SSE连接因不活跃超时，自动关闭')
    closeConnection()
    toast.info({
      title: '连接已关闭',
      message: '由于长时间无活动，连接已自动关闭',
      duration: 3000
    })
  }, ACTIVITY_TIMEOUT)
}

// 关闭连接
const closeConnection = () => {
  if (eventSource.value) {
    closeSSE(eventSource.value)
    eventSource.value = null
  }
  isConnected.value = false

  if (connectionTimeout.value) {
    clearTimeout(connectionTimeout.value)
    connectionTimeout.value = null
  }
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
    activityTimeout.value = null
  }
}

// 初始化会话
const initializeSession = async () => {
  if (isInitializing.value) return

  isInitializing.value = true

  try {
    // 确保连接唯一性
    ensureUniqueConnection()

    // 生成用户ID（如果没有的话）
    const userIdValue = 'demo-user-' + Date.now() // 临时用户ID
    userId.value = userIdValue

    // 建立SSE连接（不传sessionId，让后端创建新会话）
    eventSource.value = connectUserInteractionSSE(
      null, // sessionId为null，后端会创建新会话
      userIdValue,
      handleSSEMessage,
      handleSSEError
    )

    // 启动活跃监控
    updateActivity()

    // console.log('SSE连接已建立，等待后端返回会话信息...')

  } catch (error: any) {
    console.error('初始化会话失败:', error)
    isConnected.value = false
    toast.error({
      title: '连接失败',
      message: '无法连接到AI服务，请刷新页面重试',
      duration: 5000
    })
  } finally {
    isInitializing.value = false
  }
}

// 处理SSE消息
const handleSSEMessage = (response: any) => {
  console.log('收到SSE消息:', response)

  // 更新活跃时间
  updateActivity()

  // 处理连接建立消息
  if (handleConnectionMessage(response)) {
    return
  }

  // 处理生成提示词消息
  if (handleGenPromptMessage(response)) {
    return
  }

  // 处理新问题格式消息
  if (handleQuestionMessage(response)) {
    return
  }

  // 处理其他类型的消息
  handleOtherMessages(response)
}

// 处理连接建立消息
const handleConnectionMessage = (response: any): boolean => {
  if (response.type === 'connected' || response.sessionId) {
    // 这是连接建立时的会话信息
    if (response.sessionId) {
      sessionId.value = response.sessionId
      session.value = {
        sessionId: response.sessionId,
        userId: response.userId || 'demo-user-' + Date.now()
      }
      isConnected.value = true

      // console.log('会话已建立:', session.value)

      // 后端总是会返回nodeId，新会话返回'1'，已存在会话返回实际的nodeId
      if (response.nodeId) {
        currentNodeId.value = response.nodeId
        // console.log('会话节点ID:', response.nodeId)

        // 如果是根节点，初始化根节点
        if (response.nodeId === '1') {
          const rootNode: ConversationNode = {
            id: '1',
            content: '您好！我是AI助手，有什么可以帮助您的吗？',
            type: 'assistant',
            timestamp: new Date(),
            children: [],
            isActive: true
          }
          conversationTree.value.set('1', rootNode)
        }
      }

      // 如果有现有的qaTree，恢复对话树
      if (response.qaTree) {
        try {
          // 这里需要根据实际的qaTree格式来解析和恢复对话树
          // console.log('恢复现有对话树:', response.qaTree)
          // TODO: 实现qaTree的解析和恢复逻辑
        } catch (error) {
          console.error('恢复对话树失败:', error)
        }
      }

      toast.success({
        title: '会话已建立',
        message: response.isNewSession ? '已创建新会话' : '已连接到现有会话',
        duration: 2000
      })
    }
    return true
  }
  return false
}

// 处理生成提示词消息
const handleGenPromptMessage = (response: any): boolean => {
  if (response.genPrompt) {
    try {
      // 通过ref调用子组件的setPromptResult方法显示提示词结果
      if (questionRendererRef.value && questionRendererRef.value.setPromptResult) {
        questionRendererRef.value.setPromptResult(response.genPrompt)
        
        toast.success({
          title: '提示词生成成功',
          message: '已为您生成优化的提示词',
          duration: 3000
        })
      } else {
        console.warn('QuestionRenderer组件引用不可用，无法显示提示词结果')
        toast.error({
          title: '显示失败',
          message: '无法显示提示词结果，请重试',
          duration: 3000
        })
      }
    } catch (error) {
      console.error('处理生成提示词消息失败:', error)
      toast.error({
        title: '处理失败',
        message: '处理提示词结果时发生错误',
        duration: 3000
      })
    }
    return true
  }
  return false
}

// 处理新问题格式消息
const handleQuestionMessage = (response: any): boolean => {
  if (response.question && response.question.type) {
    // 这是新的问题格式
    currentQuestion.value = response.question

    // 更新当前节点ID为新创建的问题节点ID
    if (response.currentNodeId) {
      // 创建问题节点并添加到对话树
      const questionContent = `${response.question.question}${response.question.desc ? '\n' + response.question.desc : ''}`
      const questionNode: ConversationNode = {
        id: response.currentNodeId,
        content: questionContent,
        type: 'assistant',
        timestamp: new Date(),
        parentId: response.parentNodeId,
        children: [],
        isActive: true
      }

      // 更新父节点的children数组
      if (response.parentNodeId) {
        const parentNode = conversationTree.value.get(response.parentNodeId)
        if (parentNode) {
          // 将父节点的其他子节点设为非活跃状态
          parentNode.children.forEach(childId => {
            const childNode = conversationTree.value.get(childId)
            if (childNode) {
              setNodeAndDescendantsInactive(childId)
            }
          })
          parentNode.children.push(response.currentNodeId)
        }
      }

      // 添加新问题节点到对话树
      conversationTree.value.set(response.currentNodeId, questionNode)
      currentNodeId.value = response.currentNodeId
      // console.log('更新当前节点ID为:', response.currentNodeId)

      // 在聊天界面显示问题内容
      addAIMessage(response.currentNodeId, questionContent)
    }

    // 记录父节点ID，用于后续构建树形关系图
    if (response.parentNodeId) {
      // console.log('父节点ID:', response.parentNodeId)
    }

    isLoading.value = false
    // console.log('收到新格式问题:', response.question, '当前节点ID:', response.currentNodeId, '父节点ID:', response.parentNodeId)
    return true
  }
  return false
}

// 处理其他类型的消息
const handleOtherMessages = (response: any) => {
  const messageResponse = response as MessageResponse
  switch (messageResponse.type) {
    case 'AI_QUESTION':
      // 尝试解析问题内容为问题对象
      try {
        const questionData = JSON.parse(messageResponse.content)
        if (questionData.type && ['input', 'single', 'multi', 'form'].includes(questionData.type)) {
          currentQuestion.value = questionData
          isLoading.value = false
          break
        }
      } catch (e) {
        // console.log('非JSON格式的问题，作为普通消息处理')
      }
      // 如果不是问题格式，作为普通消息处理
      addAIMessage(messageResponse.nodeId, messageResponse.content)
      break
    case 'AI_ANSWER':  // 添加对AI_ANSWER类型的处理
      addAIMessage(messageResponse.nodeId, messageResponse.content)
      break
    case 'AI_SELECTION_QUESTION':
      addAISelectionMessage(messageResponse.nodeId, messageResponse.content, messageResponse.options || [])
      break
    case 'USER_ANSWER':
      // 用户消息确认，通常不需要特殊处理
      break
    case 'SYSTEM_INFO':
      toast.info({
        title: '系统消息',
        message: messageResponse.content,
        duration: 3000
      })
      break
    case undefined:
      // 处理没有type字段的消息
      // console.log('收到没有type字段的消息，尝试作为普通消息处理:', response)
      if (response.content) {
        addAIMessage(response.nodeId || `ai_${Date.now()}`, response.content)
      }
      break
    default:
      console.warn('未知的消息类型:', messageResponse.type, messageResponse)
      break
  }
}

var retryCount = 1
// 处理SSE错误
const handleSSEError = (error: Event) => {

  if (retryCount <= 0) return
  retryCount--
  console.error('SSE连接错误:', error)
  isConnected.value = false

  // 清理定时器
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
    activityTimeout.value = null
  }

  toast.error({
    title: '连接中断',
    message: '与AI助手的连接已中断，正在尝试重连...',
    duration: 3000
  })

  // 尝试重连（如果有会话信息）
  if (session.value && !isInitializing.value) {
    setTimeout(() => {
      if (!isConnected.value && !isInitializing.value) {
        // console.log('尝试重连到现有会话:', session.value?.sessionId)
        ensureUniqueConnection() // 确保连接唯一性

        eventSource.value = connectUserInteractionSSE(
          session.value?.sessionId || null,
          session.value?.userId || 'demo-user-' + Date.now(),
          handleSSEMessage,
          handleSSEError
        )

        // 重新启动活跃监控
        updateActivity()
      }
    }, 3000)
  }
}

// 添加AI消息到对话树
const addAIMessage = (nodeId: string, content: string) => {
  const aiNode: ConversationNode = {
    id: nodeId,
    content,
    type: 'assistant',
    timestamp: new Date(),
    parentId: currentNodeId.value,
    children: [],
    isActive: true
  }

  const currentNode = conversationTree.value.get(currentNodeId.value)
  if (currentNode) {
    currentNode.children.push(nodeId)
  }

  conversationTree.value.set(nodeId, aiNode)
  currentNodeId.value = nodeId
  isLoading.value = false
}

const addAISelectionMessage = (nodeId: string, content: string, options: string[]) => {
  // 暂时作为普通消息处理，后续可以扩展选择题UI
  const fullContent = content + '\n\n选项：\n' + options.map((opt, idx) => `${idx + 1}. ${opt}`).join('\n')
  addAIMessage(nodeId, fullContent)
}

// 计算当前分支的消息列表
const currentBranchMessages = computed(() => {
  const messages: Message[] = []
  const visited = new Set<string>()

  const buildPath = (nodeId: string): string[] => {
    const path: string[] = []
    let current = nodeId

    while (current && !visited.has(current)) {
      visited.add(current)
      path.unshift(current)
      const node = conversationTree.value.get(current)
      current = node?.parentId || ''
    }

    return path
  }

  const path = buildPath(currentNodeId.value)

  path.forEach(nodeId => {
    const node = conversationTree.value.get(nodeId)
    if (node) {
      messages.push({
        id: node.id,
        content: node.content,
        type: node.type,
        timestamp: node.timestamp
      })
    }
  })

  return messages
})

// 拖拽开始
const startResize = (event: MouseEvent | TouchEvent) => {
  isResizing.value = true

  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  startX.value = clientX
  startRightWidth.value = rightSidebarWidth.value

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.addEventListener('touchmove', handleResize)
  document.addEventListener('touchend', stopResize)

  // 防止文本选择
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'

  event.preventDefault()
}

// 拖拽过程中
const handleResize = (event: MouseEvent | TouchEvent) => {
  if (!isResizing.value) return

  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const deltaX = startX.value - clientX // 注意方向：向左拖拽为正值
  const newWidth = startRightWidth.value + deltaX

  // 限制在最小和最大宽度之间
  rightSidebarWidth.value = Math.max(
    minRightSidebarWidth,
    Math.min(maxRightSidebarWidth, newWidth)
  )

  event.preventDefault()
}

// 拖拽结束
const stopResize = () => {
  isResizing.value = false

  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)

  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}

// 更新容器宽度
const updateContainerWidth = () => {
  containerWidth.value = window.innerWidth
}

// 生命周期
onMounted(() => {
  currentNodeId.value = '1'
  const rootNode: ConversationNode = {
    id: '1',
    content: '您好！我是AI助手，有什么可以帮助您的吗？',
    type: 'assistant',
    timestamp: new Date(),
    children: [],
    isActive: true
  }
  conversationTree.value.set('1', rootNode)
  initializeSession()
  updateContainerWidth()
  window.addEventListener('resize', updateContainerWidth)
})

onUnmounted(() => {
  // 清理SSE连接
  if (eventSource.value) {
    closeSSE(eventSource.value)
    eventSource.value = null
  }

  window.removeEventListener('resize', updateContainerWidth)
  stopResize() // 确保清理事件监听器
})

// 发送消息
const handleSendMessage = async (content: string) => {
  // 重置当前问题状态，进入新的对话
  currentQuestion.value = null

  // 后端总是返回nodeId，前端也总是传递nodeId
  const nodeIdToSend = currentNodeId.value

  const userNodeId = `user_${Date.now()}`
  const userNode: ConversationNode = {
    id: userNodeId,
    content,
    type: 'user',
    timestamp: new Date(),
    parentId: currentNodeId.value,
    children: [],
    isActive: true
  }

  const currentNode = conversationTree.value.get(currentNodeId.value)
  if (currentNode) {
    // 将当前节点的其他子节点设为非活跃状态
    currentNode.children.forEach(childId => {
      const childNode = conversationTree.value.get(childId)
      if (childNode) {
        setNodeAndDescendantsInactive(childId)
      }
    })

    currentNode.children.push(userNodeId)
  }

  conversationTree.value.set(userNodeId, userNode)
  currentNodeId.value = userNodeId

  isLoading.value = true

  try {
    // 发送消息到后端，必须包含sessionId
    const messageRequest: MessageRequest = {
      sessionId: session.value.sessionId, // 必需的sessionId
      content,
      type: 'USER_TEXT'
    }

    await sendUserMessage(messageRequest, session.value.userId, nodeIdToSend)

    // 消息发送成功，等待SSE返回AI回复
    // console.log('消息已发送，等待AI回复...')

  } catch (error: any) {
    console.error('发送消息失败:', error)
    isLoading.value = false

    // 检查是否是会话相关错误
    if (error.message && (error.message.includes('sessionId') || error.message.includes('会话'))) {
      toast.error({
        title: '会话异常',
        message: '会话已失效，请刷新页面重新建立连接',
        duration: 5000
      })
      // 清理当前会话状态
      session.value = null
      closeConnection()
    } else {
      toast.error({
        title: '发送失败',
        message: error.message || '消息发送失败，请重试',
        duration: 4000
      })
    }

    // 发送失败时移除用户消息节点
    conversationTree.value.delete(userNodeId)
    if (currentNode) {
      const index = currentNode.children.indexOf(userNodeId)
      if (index > -1) {
        currentNode.children.splice(index, 1)
      }
    }
  }
}

// 处理重试问题
const handleRetryQuestion = async (reason: string = '用户要求重新生成问题') => {
  if (!session.value || !currentQuestion.value) {
    toast.error({
      title: '重试失败',
      message: '会话未建立或没有当前问题',
      duration: 3000
    })
    return
  }

  // 更新活跃时间
  updateActivity()

  isLoading.value = true

  try {
    // 构建重试请求
    const retryRequest: RetryRequest = {
      sessionId: session.value.sessionId,
      nodeId: currentNodeId.value, // 当前问题节点ID
      whyretry: reason
    }

    // 调用重试接口
    await retryQuestion(retryRequest)

    toast.success({
      title: '重试成功',
      message: '正在重新生成问题，请稍候',
      duration: 2000
    })

    // console.log('重试请求已发送，等待AI重新生成问题...')

  } catch (error: any) {
    console.error('重试失败:', error)
    isLoading.value = false

    // 检查是否是会话相关错误
    if (error.message && (error.message.includes('sessionId') || error.message.includes('nodeId') || error.message.includes('会话') || error.message.includes('节点'))) {
      toast.error({
        title: '会话异常',
        message: '会话或节点状态异常，请刷新页面重新建立连接',
        duration: 5000
      })
      // 清理当前会话状态
      session.value = null
      closeConnection()
    } else {
      toast.error({
        title: '重试失败',
        message: error.message || '重试请求失败，请重试',
        duration: 4000
      })
    }
  }
}

// 处理答案提交
const handleSubmitAnswer = async (answerData: any) => {
  if (!session.value || !currentQuestion.value) {
    toast.error({
      title: '提交失败',
      message: '会话未建立或没有当前问题',
      duration: 3000
    })
    return
  }

  // 更新活跃时间
  updateActivity()

  isLoading.value = true

  try {
    // 保存当前问题节点ID，用于后端验证
    const questionNodeId = currentNodeId.value

    // 构建统一答案请求，必须包含sessionId和正确的nodeId
    const request: UnifiedAnswerRequest = {
      sessionId: session.value.sessionId, // 必需的sessionId
      nodeId: questionNodeId, // 问题节点ID，用于后端验证
      questionType: currentQuestion.value.type,
      answer: answerData,
      userId: session.value.userId // 必需的userId
    }

    // 调用新的processAnswer接口
    await processAnswer(request)

    // 添加用户答案到对话树
    const userNodeId = `user_${Date.now()}`
    let answerContent = ''

    // 根据问题类型格式化答案内容
    switch (currentQuestion.value.type) {
      case 'input':
        answerContent = `回答：${answerData}`
        break
      case 'single':
        const selectedOption = currentQuestion.value.options.find((opt: any) => opt.id === answerData[0])
        answerContent = `选择：${selectedOption ? selectedOption.label : answerData[0]}`
        break
      case 'multi':
        const selectedOptions = currentQuestion.value.options.filter((opt: any) => answerData.includes(opt.id))
        answerContent = `选择：${selectedOptions.map((opt: any) => opt.label).join('、')}`
        break
      case 'form':
        const formAnswers = answerData.map((item: any) => {
          const field = currentQuestion.value.fields.find((f: any) => f.id === item.id)
          if (field) {
            if (field.type === 'input') {
              return `${field.question}：${item.value[0]}`
            } else {
              const selectedOpts = field.options?.filter((opt: any) => item.value.includes(opt.id))
              return `${field.question}：${selectedOpts?.map((opt: any) => opt.label).join('、') || item.value.join('、')}`
            }
          }
          return `${item.id}：${item.value.join('、')}`
        })
        answerContent = `表单回答：\n${formAnswers.join('\n')}`
        break
    }

    const userNode: ConversationNode = {
      id: userNodeId,
      content: answerContent,
      type: 'user',
      timestamp: new Date(),
      parentId: currentNodeId.value,
      children: [],
      isActive: true
    }

    const currentNode = conversationTree.value.get(currentNodeId.value)
    if (currentNode) {
      // 将当前节点的其他子节点设为非活跃状态
      currentNode.children.forEach(childId => {
        const childNode = conversationTree.value.get(childId)
        if (childNode) {
          setNodeAndDescendantsInactive(childId)
        }
      })
      currentNode.children.push(userNodeId)
    }

    conversationTree.value.set(userNodeId, userNode)
    // 不更新currentNodeId为用户节点ID，保持为问题节点ID直到收到新问题
    // currentNodeId.value = userNodeId

    // 不清除当前问题状态，保持显示直到收到新问题
    // currentQuestion.value = null

    toast.success({
      title: '提交成功',
      message: '答案已提交，等待AI回复',
      duration: 2000
    })

  } catch (error: any) {
    console.error('提交答案失败:', error)
    isLoading.value = false

    // 检查是否是会话或节点相关错误
    if (error.message && (error.message.includes('sessionId') || error.message.includes('nodeId') || error.message.includes('会话') || error.message.includes('节点'))) {
      toast.error({
        title: '会话异常',
        message: '会话或节点状态异常，请刷新页面重新建立连接',
        duration: 5000
      })
      // 清理当前会话状态
      session.value = null
      closeConnection()
    } else {
      toast.error({
        title: '提交失败',
        message: error.message || '答案提交失败，请重试',
        duration: 4000
      })
    }
  }
}

// 处理生成提示词
const handleGeneratePrompt = async (answerData: any) => {
  if (!session.value) {
    toast.error({
      title: '生成失败',
      message: '会话未建立，请先开始对话',
      duration: 3000
    })
    return
  }

  // 更新活跃时间
  updateActivity()

  try {
    // 调用生成提示词API，触发后端生成提示词
    // 注意：这里只是触发生成，真正的提示词内容会通过SSE消息返回
    await generatePrompt({
      sessionId: session.value.sessionId,
      answer: answerData
    })

    // 不在这里设置提示词结果，等待SSE消息中的handleGenPromptMessage处理
    // 真正的提示词内容会通过SSE消息在handleGenPromptMessage中处理

    toast.info({
      title: '生成中',
      message: '正在生成提示词，请稍候...',
      duration: 2000
    })

  } catch (error: any) {
    console.error('生成提示词失败:', error)
    
    // 检查是否是会话相关错误
    if (error.message && (error.message.includes('sessionId') || error.message.includes('会话'))) {
      toast.error({
        title: '会话异常',
        message: '会话已失效，请刷新页面重新建立连接',
        duration: 5000
      })
      // 清理当前会话状态
      session.value = null
      closeConnection()
    } else {
      toast.error({
        title: '生成失败',
        message: error.message || '提示词生成失败，请重试',
        duration: 4000
      })
    }
  }
}

const setNodeAndDescendantsInactive = (nodeId: string) => {
  const node = conversationTree.value.get(nodeId)
  if (node) {
    node.isActive = false
    node.children.forEach(childId => {
      setNodeAndDescendantsInactive(childId)
    })
  }
}

// generateAIResponse方法已移除，现在使用真实的AI服务

const handleNodeSelected = (nodeId: string) => {
  const targetNode = conversationTree.value.get(nodeId)
  if (targetNode) {
    // 首先将所有节点设为非活跃
    conversationTree.value.forEach(node => {
      node.isActive = false
    })

    // 激活从根节点到目标节点的完整路径
    const activatePath = (currentNodeId: string) => {
      const node = conversationTree.value.get(currentNodeId)
      if (node) {
        node.isActive = true
        if (node.parentId) {
          activatePath(node.parentId)
        }
      }
    }

    activatePath(nodeId)
    currentNodeId.value = nodeId

    // 根据节点类型设置currentQuestion
    if (targetNode.type === 'assistant') {
      // 尝试恢复问题状态
      try {
        // 方法1: 尝试解析content中的JSON格式问题数据
        const questionData = JSON.parse(targetNode.content)
        if (questionData.type && ['input', 'single', 'multi', 'form'].includes(questionData.type)) {
          currentQuestion.value = questionData
          // console.log('恢复问题状态:', questionData)
          return
        }
      } catch (e) {
        // 如果不是JSON格式，检查是否是问题文本格式
        // console.log('非JSON格式，检查是否为问题文本')
      }

      // 方法2: 如果是问题文本但不是JSON格式，清除问题状态
      // 这种情况下显示为普通对话
      currentQuestion.value = null
    } else if (targetNode.type === 'user') {
      // 如果点击的是用户节点，查找对应的问题节点
      if (targetNode.parentId) {
        const questionNode = conversationTree.value.get(targetNode.parentId)
        if (questionNode && questionNode.type === 'assistant') {
          try {
            // 尝试解析父节点（问题节点）的问题数据
            const questionData = JSON.parse(questionNode.content)
            if (questionData.type && ['input', 'single', 'multi', 'form'].includes(questionData.type)) {
              currentQuestion.value = questionData
              // console.log('从用户回答节点恢复问题状态:', questionData)
              return
            }
          } catch (e) {
            // console.log('用户节点对应的问题节点不是JSON格式')
          }
        }
      }
      // 如果无法找到对应的问题，清除问题状态
      currentQuestion.value = null
    }
  }
}

const handleBranchDeleted = (nodeId: string) => {
  const deleteNodeAndDescendants = (id: string) => {
    const node = conversationTree.value.get(id)
    if (node) {
      node.children.forEach(childId => {
        deleteNodeAndDescendants(childId)
      })

      if (node.parentId) {
        const parentNode = conversationTree.value.get(node.parentId)
        if (parentNode) {
          parentNode.children = parentNode.children.filter(childId => childId !== id)
        }
      }

      conversationTree.value.delete(id)
    }
  }

  deleteNodeAndDescendants(nodeId)

  if (!conversationTree.value.has(currentNodeId.value)) {
    // 动态查找根节点（没有parentId的节点）
    let rootNodeId = ''
    let newCurrentId = ''
    conversationTree.value.forEach((node, id) => {
      if (!node.parentId) {
        rootNodeId = id
      }
      if (node.isActive) {
        newCurrentId = id
      }
    })
    // 优先使用活跃节点，否则回退到根节点
    currentNodeId.value = newCurrentId || rootNodeId
  }
}
</script>

<style scoped>
/* 主容器 - 黑金风格 */
.ai-chat-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
  color: #e8e8e8;
  font-family: 'Inter', 'SF Pro Display', -apple-system, BlinkMacSystemFont, sans-serif;
  overflow: hidden;
  position: relative;
}

/* 动态背景效果 */
.dynamic-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.2;
  animation: float 20s ease-in-out infinite;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(45deg, #d4af37, #ffd700);
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(45deg, #c0c0c0, #ffffff);
  top: 60%;
  right: 15%;
  animation-delay: -10s;
}

.orb-3 {
  width: 150px;
  height: 150px;
  background: linear-gradient(45deg, #d4af37, #b8860b);
  bottom: 30%;
  left: 60%;
  animation-delay: -5s;
}

@keyframes float {

  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }

  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }

  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

.floating-particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  background: linear-gradient(45deg, #d4af37, #ffd700);
  border-radius: 50%;
  opacity: 0.4;
  animation: particle-float 8s ease-in-out infinite;
  width: 2px;
  height: 2px;
}

.particle:nth-child(odd) {
  animation-duration: 6s;
}

.particle:nth-child(3n) {
  animation-duration: 10s;
}

@keyframes particle-float {

  0%,
  100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }

  10%,
  90% {
    opacity: 0.4;
  }

  50% {
    transform: translateY(-100px) rotate(180deg);
    opacity: 0.8;
  }
}

/* 左侧栏 - 恢复原始宽度 */
.left-sidebar {
  width: 250px;
  /* 恢复到原始的250px宽度 */
  background: rgba(8, 8, 8, 0.9);
  backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(212, 175, 55, 0.15);
  flex-shrink: 0;
  position: relative;
  z-index: 2;
  box-shadow:
    inset -1px 0 0 rgba(212, 175, 55, 0.1),
    0 0 32px rgba(0, 0, 0, 0.3);
}

.sidebar-content {
  padding: 24px 20px;
  /* 恢复原始内边距 */
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  /* 恢复原始间距 */
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 16px;
  /* 恢复原始间距 */
  padding: 16px;
  /* 恢复原始内边距 */
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.08), rgba(244, 208, 63, 0.04));
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.icon-wrapper {
  width: 48px;
  /* 恢复到原始的48px */
  height: 48px;
  background: linear-gradient(135deg, #d4af37 0%, #f4d03f 50%, #d4af37 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 8px 24px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.icon {
  font-size: 22px;
  /* 恢复到原始的22px */
  color: #1a1a1a;
  font-weight: 700;
}

.header-text h3 {
  margin: 0;
  font-size: 18px;
  /* 恢复到原始的18px */
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.header-text p {
  margin: 4px 0 0 0;
  font-size: 12px;
  /* 恢复到原始的12px */
  color: #888;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.placeholder-content {
  text-align: center;
  padding: 32px 20px;
  /* 恢复原始内边距 */
  background: rgba(15, 15, 15, 0.6);
  border: 1px solid rgba(212, 175, 55, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.placeholder-icon {
  font-size: 32px;
  /* 恢复到原始的32px */
  margin-bottom: 16px;
  opacity: 0.6;
}

.placeholder-content p {
  margin: 0 0 8px 0;
  font-size: 16px;
  /* 恢复到原始的16px */
  font-weight: 600;
  color: #e8e8e8;
}

.placeholder-content span {
  font-size: 12px;
  /* 恢复到原始的12px */
  color: #888;
  font-weight: 500;
}

/* 右侧栏 - 进一步增加宽度 */
.right-sidebar {
  background: rgba(8, 8, 8, 0.9);
  backdrop-filter: blur(24px) saturate(180%);
  min-width: 600px;
  /* 从500px进一步增加到600px */
  max-width: 1200px;
  /* 从900px增加到1200px */
  border-left: 1px solid rgba(212, 175, 55, 0.15);
  position: relative;
  z-index: 2;
  box-shadow:
    inset 1px 0 0 rgba(212, 175, 55, 0.1),
    0 0 32px rgba(0, 0, 0, 0.3);
}

/* 主内容区域 - 恢复合适的最小宽度 */
.main-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  /* min-width: 400px; */
  /* 恢复到400px */
  background: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 2;
}

/* 响应式设计调整 */
@media (max-width: 1200px) {
  .left-sidebar {
    width: 220px;
    /* 在较小屏幕上适当减少 */
  }

  .right-sidebar {
    min-width: 450px;
  }

  .main-content {
    min-width: 350px;
  }
}

@media (max-width: 1024px) {
  .left-sidebar {
    width: 200px;
    /* 更小的屏幕进一步调整 */
  }

  .right-sidebar {
    min-width: 400px;
  }
}

.sidebar-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  padding: 32px 20px;
  background: rgba(15, 15, 15, 0.6);
  border: 1px solid rgba(212, 175, 55, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.placeholder-content:hover {
  border-color: rgba(212, 175, 55, 0.3);
  background: rgba(15, 15, 15, 0.8);
}

.placeholder-icon {
  font-size: 32px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.placeholder-content p {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #e8e8e8;
}

.placeholder-content span {
  font-size: 12px;
  color: #888;
  font-weight: 500;
}

/* 主内容区域 */
.main-content {
  display: flex;
  flex-direction: column;
  min-width: 400px;
  background: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 2;
}

/* 分隔条 - 精致设计 */
.resizer {
  width: 6px;
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.1), rgba(212, 175, 55, 0.3), rgba(212, 175, 55, 0.1));
  cursor: col-resize;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  z-index: 3;
  border-left: 1px solid rgba(212, 175, 55, 0.2);
  border-right: 1px solid rgba(212, 175, 55, 0.2);
}

.resizer:hover {
  background: linear-gradient(180deg, rgba(212, 175, 55, 0.3), rgba(212, 175, 55, 0.6), rgba(212, 175, 55, 0.3));
  box-shadow:
    0 0 20px rgba(212, 175, 55, 0.4),
    inset 0 0 20px rgba(212, 175, 55, 0.2);
  border-color: rgba(212, 175, 55, 0.5);
}

.resizer-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(212, 175, 55, 0.6), transparent);
  transform: translateX(-50%);
}

.resizer:hover .resizer-line {
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  box-shadow: 0 0 10px rgba(212, 175, 55, 0.6);
}

.resizer-handle {
  width: 24px;
  height: 48px;
  background: linear-gradient(135deg, rgba(15, 15, 15, 0.9), rgba(25, 25, 25, 0.9));
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.resizer:hover .resizer-handle {
  opacity: 1;
  transform: scale(1.05);
  border-color: rgba(212, 175, 55, 0.6);
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.4),
    0 0 20px rgba(212, 175, 55, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.resizer-dots {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.resizer-dots span {
  width: 4px;
  height: 4px;
  background: linear-gradient(45deg, #d4af37, #f4d03f);
  border-radius: 50%;
  box-shadow: 0 0 4px rgba(212, 175, 55, 0.6);
  transition: all 0.3s ease;
}

.resizer:hover .resizer-dots span {
  background: linear-gradient(45deg, #ffffff, #d4af37);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
  transform: scale(1.2);
}

/* 右侧栏 */
.right-sidebar {
  background: rgba(8, 8, 8, 0.9);
  backdrop-filter: blur(24px) saturate(180%);
  min-width: 300px;
  max-width: 600px;
  flex-shrink: 0;
  border-left: 1px solid rgba(212, 175, 55, 0.15);
  position: relative;
  z-index: 2;
  box-shadow:
    inset 1px 0 0 rgba(212, 175, 55, 0.1),
    0 0 32px rgba(0, 0, 0, 0.3);
}

/* 拖拽时的全局样式 */
body.resizing {
  cursor: col-resize !important;
  user-select: none !important;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .left-sidebar {
    width: 200px;
  }

  .sidebar-content {
    padding: 24px 16px;
  }
}

@media (max-width: 768px) {
  .ai-chat-page {
    flex-direction: column;
  }

  .left-sidebar,
  .right-sidebar {
    width: 100% !important;
    height: 200px;
  }

  .main-content {
    width: 100% !important;
    flex: 1;
    min-height: 400px;
  }

  .resizer {
    display: none;
  }

  .dynamic-background {
    display: none;
  }
}

@media (max-width: 480px) {
  .sidebar-content {
    padding: 16px 12px;
    gap: 20px;
  }

  .sidebar-header {
    padding: 16px;
    gap: 12px;
  }

  .icon-wrapper {
    width: 40px;
    height: 40px;
  }

  .icon {
    font-size: 20px;
  }
}
</style>
