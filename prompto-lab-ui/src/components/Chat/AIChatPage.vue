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
    <div class="left-sidebar" :class="{ collapsed: !sidebarExpanded }">
      <ConversationHistory
        :session-list="sessionList"
        :current-session-id="currentSessionId"
        :sidebar-expanded="sidebarExpanded"
        @start-new-chat="startNewChat"
        @switch-to-session="switchToSession"
        @delete-session="deleteSession"
        @toggle-sidebar="toggleSidebar"
      />
    </div>

    <!-- 中间问答主页面 -->
    <div class="main-content" :style="{ width: mainContentWidth + 'px' }">
      <QuestionRenderer ref="questionRendererRef" :current-question="currentQuestion" :is-loading="isLoading"
        :session-id="sessionId" :user-id="userId" :conversation-tree="conversationTree" :current-node-id="currentNodeId"
        @send-message="handleSendMessage" @submit-answer="handleSubmitAnswer" @retry-question="handleRetryQuestion" 
        @generate-prompt="handleGeneratePrompt" @node-selected="handleNodeSelected" />
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
import { ref, computed, onMounted, onUnmounted, readonly, nextTick } from 'vue'
import QuestionRenderer from './QuestionRenderer.vue'
import MindMapTree from './MindMapTree.vue'
import ConversationHistory from './ConversationHistory.vue'
import { startConversation, sendMessage, sendUserMessage, connectSSE, closeSSE, processAnswer, connectUserInteractionSSE, retryQuestion, fetchSessionList, type MessageRequest, type MessageResponse, type ConversationSession, type UnifiedAnswerRequest, type FormAnswerItem, type RetryRequest, type SessionItem } from '@/services/conversationApi'
import { generatePrompt, getConversationHistory, validateFingerprint } from '@/services/userInteractionApi'
import { toast } from '@/utils/toast'
import sseConnectionManager from '@/services/sseConnectionManager'

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

// 指纹和会话列表
const FINGERPRINT_KEY = 'prompto_lab_fingerprint'
const SESSION_LIST_KEY = 'prompto_lab_session_list'
const CURRENT_SESSION_ID_KEY = 'prompto_lab_current_session_id'
const fingerprint = ref<string>(localStorage.getItem(FINGERPRINT_KEY) || '')
const sessionList = ref<SessionItem[]>([])
const currentSessionId = ref<string>('')

// 保存会话列表到localStorage
const saveSessionList = (sessions: SessionItem[]) => {
  try {
    localStorage.setItem(SESSION_LIST_KEY, JSON.stringify(sessions))
  } catch (e) {
    console.error('保存会话列表失败:', e)
  }
}

// 保存当前会话ID到localStorage
const saveCurrentSessionId = (sessionId: string) => {
  try {
    localStorage.setItem(CURRENT_SESSION_ID_KEY, sessionId)
  } catch (e) {
    console.error('保存当前会话ID失败:', e)
  }
}

// 从localStorage加载会话列表
const loadSessionListFromStorage = (): SessionItem[] => {
  try {
    const stored = localStorage.getItem(SESSION_LIST_KEY)
    return stored ? JSON.parse(stored) : []
  } catch (e) {
    console.error('加载会话列表失败:', e)
    return []
  }
}

// 从localStorage加载当前会话ID
const loadCurrentSessionIdFromStorage = (): string => {
  try {
    return localStorage.getItem(CURRENT_SESSION_ID_KEY) || ''
  } catch (e) {
    console.error('加载当前会话ID失败:', e)
    return ''
  }
}

// 侧边栏展开状态
const sidebarExpanded = ref<boolean>(true)

// 保存指纹到localStorage
const saveFingerprint = (fp: string) => {
  fingerprint.value = fp
  localStorage.setItem(FINGERPRINT_KEY, fp)
}

// SSE连接管理
const connectionTimeout = ref<NodeJS.Timeout | null>(null)
const activityTimeout = ref<NodeJS.Timeout | null>(null)
const heartbeatInterval = ref<NodeJS.Timeout | null>(null)
const ACTIVITY_TIMEOUT = 5 * 60 * 1000 // 5分钟不活跃超时
const HEARTBEAT_INTERVAL = 30 * 1000 // 30秒心跳检测间隔
const lastActivityTime = ref<number>(Date.now())
const lastHeartbeatTime = ref<number>(Date.now())

// 对话树存储所有节点
const conversationTree = ref<Map<string, ConversationNode>>(new Map())
const currentNodeId = ref<string>('')
const isLoading = ref(false)

// 设置loading状态的辅助函数
const setLoading = (loading: boolean) => {
  isLoading.value = loading
}

// 加载会话列表 - 从SSE连接返回的数据中获取，不再单独请求
// const loadSessionList = async () => {
//   try {
//     sessionList.value = await fetchSessionList()
//     if (sessionList.value.length > 0) {
//       currentSessionId.value = sessionList.value[0].id
//     }
//   } catch (error) {
//     console.error('Failed to load session list:', error)
//     toast.error('加载会话列表失败')
//   }
// }

// 在组件挂载后不需要立即加载会话列表，而是等待SSE连接返回数据
// onMounted(() => {
//   loadSessionList()
// })

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

  // 停止心跳检测
  stopHeartbeat()
  
  // 重置重连状态
  resetRetryCount()
}

// 更新活跃时间
const updateActivity = () => {
  const now = Date.now()
  lastActivityTime.value = now
  lastHeartbeatTime.value = now

  // 重置活跃超时定时器
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
  }

  activityTimeout.value = setTimeout(() => {
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
  
  // 停止心跳检测
  stopHeartbeat()
  
  // 清理全局连接管理器的连接信息
  sseConnectionManager.clearConnection()
}

// 连接建立锁，防止并发建立连接
// 建立新的SSE连接
const establishNewConnection = async () => {
  // 确保连接唯一性
  ensureUniqueConnection()

  // 建立SSE连接，后端会自动处理指纹生成和会话管理
  eventSource.value = connectUserInteractionSSE(
    handleSSEMessage,
    handleSSEError,
    handleSSEClose
  )

  // 启动活跃监控
  updateActivity()

  console.log('SSE连接已建立，等待后端返回指纹和会话信息...')
}

// 初始化会话
const initializeSession = async () => {
  if (isInitializing.value) return

  // 优先检查全局连接管理器中是否已有活跃连接
  if (sseConnectionManager.hasActiveConnection()) {
    const connectionInfo = sseConnectionManager.getConnectionInfo()
    if (connectionInfo && connectionInfo.eventSource) {
      // 验证连接是否真正可用
      try {
        const testEventSource = connectionInfo.eventSource
        if (testEventSource.readyState === EventSource.OPEN && testEventSource.url) {
          console.log('从全局连接管理器恢复SSE连接:', connectionInfo.connectionId)
          eventSource.value = connectionInfo.eventSource
          fingerprint.value = connectionInfo.fingerprint
          isConnected.value = connectionInfo.isConnected
          
          // 更新活跃时间
          sseConnectionManager.updateActivity()
          
          console.log('SSE连接已从全局管理器恢复，跳过初始化')
          return
        } else {
          console.log('全局管理器中的连接已失效，清理并重新建立')
          sseConnectionManager.clearConnection()
        }
      } catch (error) {
        console.log('验证全局连接时出错，清理并重新建立:', error)
        sseConnectionManager.clearConnection()
      }
    }
  }

  // 如果已有有效的SSE连接，跳过初始化
  if (eventSource.value && eventSource.value.readyState === EventSource.OPEN && isConnected.value) {
    console.log('SSE连接已存在且有效，跳过初始化')
    return
  }

  isInitializing.value = true

  try {
    // 直接建立SSE连接，后端会自动处理指纹生成或获取
    console.log('建立SSE连接，后端将自动处理指纹')
    await establishNewConnection()

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


  // 更新活跃时间
  updateActivity()

  // 检查是否是新的统一消息格式
  let actualData = response
  if (response.success !== undefined && response.code !== undefined && response.data !== undefined) {


    // 如果是错误消息
    if (!response.success) {
      console.error('收到SSE错误消息:', response)
      setLoading(false)
      toast.error({
        title: 'AI服务错误',
        message: response.data || 'AI服务调用失败，请重试',
        duration: 5000
      })
      return
    }

    // 如果是成功消息，提取data字段作为实际数据
    try {
      actualData = typeof response.data === 'string' ? JSON.parse(response.data) : response.data

    } catch (e) {
      console.error('解析SSE数据失败:', e, '原始数据:', response.data)
      setLoading(false)
      return
    }
  }

  // 处理连接建立消息
  if (handleConnectionMessage(actualData)) {
    return
  }

  // 处理生成提示词消息
  if (handleGenPromptMessage(actualData)) {
    return
  }

  // 处理新问题格式消息

  if (handleQuestionMessage(actualData)) {

    return
  }


  // 处理其他类型的消息
  handleOtherMessages(actualData)

  // 兜底逻辑：确保isLoading状态被重置
  // 如果消息处理完成但isLoading仍为true，则重置为false
  if (isLoading.value) {

    setLoading(false)
  }
}

// 处理连接建立消息
const handleConnectionMessage = (response: any): boolean => {
  if (response.type === 'connected' || response.fingerprint || response.fingerprintId) {
    console.log('收到SSE连接建立消息:', response)
    
    // 连接成功，重置重连计数器并启动心跳检测
    resetRetryCount()
    isConnected.value = true
    startHeartbeat()
    
    // 更新全局连接管理器的连接状态
    sseConnectionManager.updateConnectionStatus(true)

    // 处理SSE连接建立时的指纹和sessionList
    const fingerprintValue = response.fingerprintId || response.fingerprint
    if (fingerprintValue) {
      saveFingerprint(fingerprintValue)
      fingerprint.value = fingerprintValue
      console.log('已保存指纹:', fingerprintValue)
      
      // 将连接注册到全局管理器
      if (eventSource.value) {
        const connectionId = sseConnectionManager.registerConnection(
          eventSource.value,
          fingerprintValue,
          true // 连接已建立
        )
        console.log('SSE连接已注册到全局管理器:', connectionId)
      }
    }

    // 处理新的会话详细信息格式
    if (response.sessionList && Array.isArray(response.sessionList)) {
      // 将SessionDetailResponse对象转换为SessionItem对象
      sessionList.value = response.sessionList.map((sessionDetail: any) => ({
        id: sessionDetail.sessionId,
        title: sessionDetail.lastNodeQuestion || '新会话',
        lastMessage: sessionDetail.lastNodeQuestion || '无内容',
        updatedAt: sessionDetail.updateTime || new Date().toISOString(),
        createdAt: sessionDetail.lastNodeCreateTime || new Date().toISOString()
      }))
      console.log('已更新会话列表:', sessionList.value)

      // 保存会话列表到localStorage
      saveSessionList(sessionList.value)

      // 如果有会话列表且当前没有设置currentSessionId，则设置为第一个会话
      if (sessionList.value.length > 0 && !currentSessionId.value) {
        currentSessionId.value = sessionList.value[0].id
        // 保存当前会话ID到localStorage
        saveCurrentSessionId(currentSessionId.value)
      }
    }

    // 这是连接建立时的会话信息
    if (response.sessionId) {
      session.value = {
        sessionId: response.sessionId,
        userId: response.userId || 'demo-user-' + Date.now()
      }
      isConnected.value = true

      console.log('会话已建立:', session.value)

      // 后端总是会返回nodeId，新会话返回'1'，已存在会话返回实际的nodeId
      if (response.currentNodeId) {
        currentNodeId.value = response.currentNodeId
        // console.log('会话节点ID:', response.nodeId)

        // 如果是根节点，初始化根节点
        if (response.currentNodeId === '1') {
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
    } else {
      // 如果没有sessionId但有sessionList，设置连接状态为已连接
      // 但不设置具体的会话，允许后续操作创建新会话
      isConnected.value = true
      console.warn('SSE连接建立消息中缺少sessionId:', response)

      toast.success({
        title: '连接已建立',
        message: '已连接到服务，可以开始对话',
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

    // 从AI响应消息中提取并设置sessionId
    if (response.sessionId && !session.value) {
      console.log('从AI响应消息中设置sessionId:', response.sessionId)
      session.value = {
        sessionId: response.sessionId,
        userId: fingerprint.value || 'unknown'
      }

      toast.success({
        title: '会话已建立',
        message: '已从AI响应中获取会话信息',
        duration: 2000
      })
    }

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

    setLoading(false)
    // console.log('收到新格式问题:', response.question, '当前节点ID:', response.currentNodeId, '父节点ID:', response.parentNodeId)
    return true
  }
  return false
}

// 处理其他类型的消息
const handleOtherMessages = (response: any) => {
  // 从AI响应消息中提取并设置sessionId（如果还没有设置的话）
  if (response.sessionId && !session.value) {
    console.log('从其他消息类型中设置sessionId:', response.sessionId)
    session.value = {
      sessionId: response.sessionId,
      userId: fingerprint.value || 'unknown'
    }

    toast.success({
      title: '会话已建立',
      message: '已从AI响应中获取会话信息',
      duration: 2000
    })
  }

  // 首先检查是否是错误消息
  if (response.error) {
    console.error('收到SSE错误消息:', response)
    setLoading(false)
    toast.error({
      title: 'AI服务错误',
      message: response.message || 'AI服务调用失败，请重试',
      duration: 5000
    })
    return
  }

  const messageResponse = response as MessageResponse
  switch (messageResponse.type) {
    case 'AI_QUESTION':
      // 尝试解析问题内容为问题对象
      try {
        const questionData = JSON.parse(messageResponse.content)
        if (questionData.type && ['input', 'single', 'multi', 'form'].includes(questionData.type)) {
          currentQuestion.value = questionData
          setLoading(false)
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
      // 但需要重置loading状态
      setLoading(false)
      break
    case 'SYSTEM_INFO':
      toast.info({
        title: '系统消息',
        message: messageResponse.content,
        duration: 3000
      })
      setLoading(false)
      break
    case undefined:
      // 处理没有type字段的消息
      // console.log('收到没有type字段的消息，尝试作为普通消息处理:', response)
      if (response.content) {
        addAIMessage(response.nodeId || `ai_${Date.now()}`, response.content)
      } else {
        // 如果没有内容，也要重置loading状态
        setLoading(false)
      }
      break
    default:
      console.warn('未知的消息类型:', messageResponse.type, messageResponse)
      // 未知消息类型也要重置loading状态
      setLoading(false)
      break
  }
}

// SSE重连配置 - 可配置的重连策略
interface ReconnectConfig {
  maxRetryCount: number
  initialRetryDelay: number
  maxRetryDelay: number
  backoffMultiplier: number
  enableHeartbeat: boolean
  heartbeatInterval: number
}

// 默认重连配置
const DEFAULT_RECONNECT_CONFIG: ReconnectConfig = {
  maxRetryCount: 5,
  initialRetryDelay: 1000, // 初始重连延迟1秒
  maxRetryDelay: 30000, // 最大重连延迟30秒
  backoffMultiplier: 2, // 指数退避倍数
  enableHeartbeat: true, // 启用心跳检测
  heartbeatInterval: 30000 // 30秒心跳检测间隔
}

// 当前重连配置（可通过外部配置覆盖）
const reconnectConfig = ref<ReconnectConfig>({ ...DEFAULT_RECONNECT_CONFIG })

// 重连状态
let retryCount = 0
let retryTimeout: NodeJS.Timeout | null = null

// 兼容性常量（保持向后兼容）
const MAX_RETRY_COUNT = reconnectConfig.value.maxRetryCount
const INITIAL_RETRY_DELAY = reconnectConfig.value.initialRetryDelay
const MAX_RETRY_DELAY = reconnectConfig.value.maxRetryDelay

// 计算重连延迟（指数退避策略）
const calculateRetryDelay = (attemptCount: number): number => {
  const delay = reconnectConfig.value.initialRetryDelay * Math.pow(reconnectConfig.value.backoffMultiplier, attemptCount)
  return Math.min(delay, reconnectConfig.value.maxRetryDelay)
}

// 配置重连策略
const configureReconnectStrategy = (config: Partial<ReconnectConfig>) => {
  reconnectConfig.value = { ...reconnectConfig.value, ...config }
  console.log('重连策略已更新:', reconnectConfig.value)
}

// 获取当前重连配置
const getReconnectConfig = (): ReconnectConfig => {
  return { ...reconnectConfig.value }
}

// 重置重连计数器
const resetRetryCount = () => {
  retryCount = 0
  if (retryTimeout) {
    clearTimeout(retryTimeout)
    retryTimeout = null
  }
}

// 启动心跳检测
const startHeartbeat = () => {
  // 检查是否启用心跳检测
  if (!reconnectConfig.value.enableHeartbeat) {
    console.log('心跳检测已禁用')
    return
  }
  
  if (heartbeatInterval.value) {
    clearInterval(heartbeatInterval.value)
  }
  
  heartbeatInterval.value = setInterval(() => {
    const now = Date.now()
    const timeSinceLastActivity = now - lastActivityTime.value
    const heartbeatIntervalMs = reconnectConfig.value.heartbeatInterval
    
    // 如果连接状态为已连接但长时间没有活动，检查连接状态
    if (isConnected.value && timeSinceLastActivity > heartbeatIntervalMs) {
      // 检查EventSource连接状态
      if (eventSource.value && eventSource.value.readyState !== EventSource.OPEN) {
        console.warn('检测到SSE连接异常，触发重连')
        handleSSEError(new Event('heartbeat_check_failed'))
      }
    }
    
    lastHeartbeatTime.value = now
  }, reconnectConfig.value.heartbeatInterval)
}

// 停止心跳检测
const stopHeartbeat = () => {
  if (heartbeatInterval.value) {
    clearInterval(heartbeatInterval.value)
    heartbeatInterval.value = null
  }
}

// 处理SSE错误
const handleSSEError = (error: Event) => {
  console.error('SSE连接错误:', error)
  isConnected.value = false

  // 清理定时器
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
    activityTimeout.value = null
  }

  // 如果正在初始化或已经有连接，不进行重连
  if (isInitializing.value || eventSource.value) {
    console.log('正在初始化或已有连接，跳过重连')
    return
  }

  // 如果已达到最大重连次数，停止重连
  if (retryCount >= reconnectConfig.value.maxRetryCount) {
    console.error(`已达到最大重连次数(${reconnectConfig.value.maxRetryCount})，停止重连`)
    toast.error({
      title: '连接失败',
      message: '无法连接到服务器，请刷新页面重试',
      duration: 5000
    })
    return
  }

  retryCount++
  const retryDelay = calculateRetryDelay(retryCount - 1)
  
  console.log(`第${retryCount}次重连尝试，延迟${retryDelay}ms`)
  
  toast.error({
    title: '连接中断',
    message: `连接已中断，${retryDelay/1000}秒后尝试第${retryCount}次重连...`,
    duration: Math.min(retryDelay, 3000)
  })

  // 使用指数退避策略进行重连
  retryTimeout = setTimeout(() => {
    if (!isConnected.value && !isInitializing.value && !eventSource.value) {
      console.log(`开始第${retryCount}次重连尝试`)
      
      // 重新初始化会话（包含指纹验证）
      initializeSession()
    }
  }, retryDelay)
}

// 处理SSE连接关闭
const handleSSEClose = () => {
  console.log('SSE连接已关闭，清理客户端状态')
  
  // 更新连接状态
  isConnected.value = false
  
  // 清理EventSource引用
  eventSource.value = null
  
  // 清理定时器
  if (activityTimeout.value) {
    clearTimeout(activityTimeout.value)
    activityTimeout.value = null
  }
  if (connectionTimeout.value) {
    clearTimeout(connectionTimeout.value)
    connectionTimeout.value = null
  }
  
  // 停止心跳检测
  stopHeartbeat()
  
  // 重置重连状态
  resetRetryCount()
  
  // 清理全局连接管理器的连接信息
  sseConnectionManager.clearConnection()
  
  // 不清理指纹，保持用户身份标识
  // localStorage.removeItem(FINGERPRINT_KEY)
  // fingerprint.value = null
  
  console.log('SSE连接关闭，保持指纹不变')
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
  setLoading(false)
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

// 侧边栏切换方法
const toggleSidebar = () => {
  sidebarExpanded.value = !sidebarExpanded.value
}

// 会话操作方法
const startNewChat = () => {
  currentSessionId.value = ''
  // 清空当前对话
  conversationTree.value.clear()
  currentNodeId.value = ''
  currentQuestion.value = null
  // 重新初始化会话
  initializeSession()
  toast.success('已开始新对话')
}

const switchToSession = async (sessionId: string) => {
  if (currentSessionId.value === sessionId) return

  try {
    // 显示加载状态
    setLoading(true)
    
    // 确保SSE连接已建立
    if (!isConnected.value || !eventSource.value || eventSource.value.readyState !== EventSource.OPEN) {
      console.log('SSE连接未建立，先建立连接再切换会话')
      toast.info({
        title: '正在连接',
        message: '正在建立连接，请稍候...',
        duration: 2000
      })
      
      // 尝试建立连接
      await initializeSession()
      
      // 等待连接建立（最多等待5秒）
      let waitCount = 0
      while ((!isConnected.value || !eventSource.value || eventSource.value.readyState !== EventSource.OPEN) && waitCount < 50) {
        await new Promise(resolve => setTimeout(resolve, 100))
        waitCount++
      }
      
      // 如果连接仍未建立，提示用户
      if (!isConnected.value || !eventSource.value || eventSource.value.readyState !== EventSource.OPEN) {
        throw new Error('无法建立SSE连接，请刷新页面重试')
      }
      
      console.log('SSE连接已建立，继续切换会话')
    }
    
    // 调用API获取会话历史
    const historyResponse = await getConversationHistory(sessionId)
    
    if (historyResponse && historyResponse.data) {
      const historyData = historyResponse.data
      
      // 更新当前会话ID
      currentSessionId.value = sessionId
      saveCurrentSessionId(sessionId)
      
      // 清空当前对话树和问题状态
      conversationTree.value.clear()
      currentQuestion.value = null // 清空当前问题，显示对话历史而不是快速输入
      
      // 如果有qaTree数据，重建对话树
      if (historyData.qaTree) {
        try {
          // 解析qaTree数据 - 后端返回的是JsonNode数组的JSON字符串
          let qaTreeNodes = historyData.qaTree
          if (typeof qaTreeNodes === 'string') {
            qaTreeNodes = JSON.parse(qaTreeNodes)
          }
          
          // 重建对话树 - qaTreeNodes是JsonNode数组格式
          if (Array.isArray(qaTreeNodes)) {
            qaTreeNodes.forEach((jsonNode: any) => {
              if (jsonNode && jsonNode.nodeId) {
                const conversationNode: ConversationNode = {
                  id: jsonNode.nodeId,
                  content: jsonNode.question || '',
                  type: jsonNode.parentId ? 'user' : 'assistant', // 根节点为assistant，其他为user
                  timestamp: new Date(),
                  parentId: jsonNode.parentId,
                  children: [],
                  isActive: false
                }
                conversationTree.value.set(jsonNode.nodeId, conversationNode)
                
                // 如果有答案，添加对应的回答节点
                if (jsonNode.answer && jsonNode.answer.trim()) {
                  const answerNode: ConversationNode = {
                    id: jsonNode.nodeId + '_answer',
                    content: jsonNode.answer,
                    type: 'assistant',
                    timestamp: new Date(),
                    parentId: jsonNode.nodeId,
                    children: [],
                    isActive: false
                  }
                  conversationTree.value.set(answerNode.id, answerNode)
                  conversationNode.children.push(answerNode.id)
                }
              }
            })
            
            // 设置当前节点ID
            if (historyData.currentNode) {
              currentNodeId.value = historyData.currentNode
            }
            
            console.log('已加载会话历史:', {
              sessionId,
              nodeCount: conversationTree.value.size,
              currentNodeId: currentNodeId.value
            })
            
            toast.success({
              title: '会话已切换',
              message: `已加载会话历史 (${conversationTree.value.size} 个节点)`,
              duration: 2000
            })
          } else {
            console.warn('qaTree数据格式不正确，期望数组格式:', qaTreeNodes)
            toast.warning({
              title: '历史记录为空',
              message: '该会话暂无历史记录',
              duration: 2000
            })
          }
        } catch (parseError) {
          console.error('解析qaTree数据失败:', parseError)
          toast.error({
            title: '数据解析失败',
            message: '无法解析会话历史数据',
            duration: 3000
          })
        }
      } else {
        console.log('该会话没有qaTree数据')
        toast.info({
          title: '会话已切换',
          message: '该会话暂无历史记录',
          duration: 2000
        })
      }
    } else {
      throw new Error('获取会话历史失败：响应数据为空')
    }
  } catch (error) {
    console.error('切换会话失败:', error)
    toast.error({
      title: '切换失败',
      message: '无法加载会话历史，请重试',
      duration: 3000
    })
  } finally {
    setLoading(false)
  }
}

const deleteSession = (sessionId: string) => {
  const index = sessionList.value.findIndex(s => s.id === sessionId)
  if (index > -1) {
    sessionList.value.splice(index, 1)
    toast.success('会话已删除')

    // 如果删除的是当前会话，切换到新对话
    if (currentSessionId.value === sessionId) {
      startNewChat()
    }
  }
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

  // 尝试从localStorage加载会话历史
  const storedSessionList = loadSessionListFromStorage()
  const storedCurrentSessionId = loadCurrentSessionIdFromStorage()

  if (storedSessionList.length > 0) {
    sessionList.value = storedSessionList
    console.log('从localStorage加载会话列表:', sessionList.value)
  }

  if (storedCurrentSessionId) {
    currentSessionId.value = storedCurrentSessionId
    console.log('从localStorage加载当前会话ID:', currentSessionId.value)
  }

  // 页面加载时检查并清理可能失效的连接
  if (sseConnectionManager.hasActiveConnection()) {
    const connectionInfo = sseConnectionManager.getConnectionInfo()
    if (connectionInfo?.eventSource) {
      try {
        // 检查连接是否在页面刷新后仍然有效
        if (connectionInfo.eventSource.readyState !== EventSource.OPEN) {
          console.log('页面加载时发现连接已失效，清理连接信息')
          sseConnectionManager.clearConnection()
        }
      } catch (error) {
        console.log('页面加载时检查连接状态出错，清理连接信息:', error)
        sseConnectionManager.clearConnection()
      }
    }
  }

  // 页面可见性变化处理函数
  const handleVisibilityChange = () => {
    if (!document.hidden) {
      // 页面变为可见时，检查连接状态
      if (sseConnectionManager.hasActiveConnection()) {
        const connectionInfo = sseConnectionManager.getConnectionInfo()
        if (connectionInfo?.eventSource) {
          try {
            if (connectionInfo.eventSource.readyState !== EventSource.OPEN) {
              console.log('页面可见时发现连接已失效，清理连接信息')
              sseConnectionManager.clearConnection()
              // 如果连接失效，尝试重新初始化
              if (!isInitializing.value) {
                initializeSession()
              }
            }
          } catch (error) {
            console.log('页面可见时检查连接状态出错，清理连接信息:', error)
            sseConnectionManager.clearConnection()
          }
        }
      }
    }
  }
  
  // 添加页面可见性变化监听器
  document.addEventListener('visibilitychange', handleVisibilityChange)

  // 添加页面关闭时的连接清理
  const handleBeforeUnload = () => {
    console.log('页面即将关闭，清理SSE连接')
    // 立即关闭SSE连接
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
    }
    // 清理全局连接管理器
    sseConnectionManager.clearConnection()
    // 清理所有定时器
    if (activityTimeout.value) {
      clearTimeout(activityTimeout.value)
      activityTimeout.value = null
    }
    if (connectionTimeout.value) {
      clearTimeout(connectionTimeout.value)
      connectionTimeout.value = null
    }
    stopHeartbeat()
  }
  
  window.addEventListener('beforeunload', handleBeforeUnload)

  // 初始化会话
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
  
  // 清理全局连接管理器的连接信息
  sseConnectionManager.clearConnection()

  window.removeEventListener('resize', updateContainerWidth)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
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

  setLoading(true)

  try {
    // 发送消息到后端，sessionId可选，不带时默认新会话
    const messageRequest: MessageRequest = {
      sessionId: session.value?.sessionId || '', // sessionId可选，空字符串表示新会话
      content,
      type: 'USER_TEXT'
    }

    await sendUserMessage(messageRequest)

    // 消息发送成功，等待SSE返回AI回复
    // console.log('消息已发送，等待AI回复...')

  } catch (error: any) {
    console.error('发送消息失败:', error)
    setLoading(false)

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
  console.log('开始处理重试请求，当前session状态:', {
    session: session.value,
    isConnected: isConnected.value,
    currentNodeId: currentNodeId.value,
    fingerprint: fingerprint.value
  })

  // 验证必需参数：sessionId和指纹
  if (!session.value?.sessionId) {
    console.error('重试失败：缺少sessionId', {
      session: session.value,
      isConnected: isConnected.value,
      eventSource: !!eventSource.value
    })

    // 尝试重新建立连接
    toast.error({
      title: '连接异常',
      message: '会话连接已断开，正在尝试重新连接...',
      duration: 3000
    })

    try {
      // 重新初始化会话
      await initializeSession()

      // 等待一段时间让连接建立
      setTimeout(async () => {
        if (session.value?.sessionId) {
          toast.success({
            title: '连接已恢复',
            message: '请重新尝试重试操作',
            duration: 2000
          })
        } else {
          toast.error({
            title: '连接失败',
            message: '无法重新建立连接，请刷新页面',
            duration: 5000
          })
        }
      }, 2000)
    } catch (error) {
      console.error('重新连接失败:', error)
      toast.error({
        title: '重试失败',
        message: '无法重新建立连接，请刷新页面重试',
        duration: 5000
      })
    }
    return
  }

  if (!fingerprint.value) {
    toast.error({
      title: '重试失败',
      message: '缺少指纹信息，无法重试',
      duration: 3000
    })
    return
  }

  if (!currentQuestion.value) {
    toast.error({
      title: '重试失败',
      message: '没有当前问题',
      duration: 3000
    })
    return
  }

  // 更新活跃时间
  updateActivity()

  setLoading(true)

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
    setLoading(false)

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

  setLoading(true)

  try {
    // 保存当前问题节点ID，用于后端验证
    const questionNodeId = currentNodeId.value

    // 构建统一答案请求，必须包含sessionId
    const request: UnifiedAnswerRequest = {
      sessionId: session.value.sessionId, // 必需的sessionId
      questionType: currentQuestion.value.type,
      answer: answerData
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
      default:
        answerContent = `未知类型：${answerData}`
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
    setLoading(false)

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
  console.log('开始处理生成提示词请求', {
    currentQuestion: currentQuestion.value,
    session: session.value,
    currentNodeId: currentNodeId.value,
    answerData: answerData
  });

  // 检查是否有输入内容
  const hasInputContent = answerData &&
    ((typeof answerData === 'string' && answerData.trim().length > 0) ||
     (Array.isArray(answerData) && answerData.length > 0) ||
     (typeof answerData === 'object' && Object.keys(answerData).length > 0));

  if (!hasInputContent) {
    toast.error('请输入内容后再生成提示词。');
    return;
  }

  // 注意：这里不再检查session.value是否存在，因为后端支持在没有sessionId时创建新会话
  // if (!session.value) {
  //   toast.error({
  //     title: '生成失败',
  //     message: '会话未建立，请先开始对话',
  //     duration: 3000
  //   })
  //   return
  // }

  // 更新活跃时间
  updateActivity()

  try {
    // 调用生成提示词API，触发后端生成提示词
    // 注意：这里只是触发生成，真正的提示词内容会通过SSE消息返回
    const genPromptRequest = {
      // sessionId可选，不提供时后端会创建新会话
      sessionId: session.value?.sessionId || null,
      nodeId: currentNodeId.value,
      answer: answerData
    };

    console.log('发送生成提示词请求:', genPromptRequest);

    await generatePrompt(genPromptRequest)

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

// 导出给父组件使用的方法
defineExpose({
  sendMessage: handleSendMessage,
  // SSE重连策略配置方法
  configureReconnectStrategy,
  getReconnectConfig,
  // 连接状态和控制方法
  isConnected: readonly(isConnected),
  reconnectManually: () => {
    if (!isConnected.value) {
      resetRetryCount()
      initializeSession()
    }
  }
})
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

/* 左侧栏 - 可收缩 */
.left-sidebar {
  width: 250px;
  background: rgba(8, 8, 8, 0.9);
  backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(212, 175, 55, 0.15);
  flex-shrink: 0;
  position: relative;
  z-index: 2;
  box-shadow:
    inset -1px 0 0 rgba(212, 175, 55, 0.1),
    0 0 32px rgba(0, 0, 0, 0.3);
  transition: width 0.3s ease;
}

.left-sidebar.collapsed {
  width: 60px;
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
