import { API_CONFIG } from './apiConfig'
 import { apiRequest } from './apiUtils'

// 类型定义
export interface MessageRequest {
  sessionId?: string // sessionId可选，不带时默认新会话
  content: string
  type: 'USER_TEXT' | 'USER_SELECTION'
  questionId?: string
  selectedOption?: number
}

export interface MessageResponse {
  nodeId: string
  content: string
  type: 'USER_ANSWER' | 'AI_QUESTION' | 'AI_SELECTION_QUESTION' | 'SYSTEM_INFO' | 'AI_ANSWER'
  options?: string[]
  timestamp: number
}

export interface ConversationSession {
  sessionId: string
  userId: string
  qaTree: any
  currentNodeId: string
  createdAt: string
}

export interface SessionItem {
  id: string
  title: string
  lastMessage: string
  updatedAt: string
  createdAt: string
}


// 统一答案请求接口
export interface UnifiedAnswerRequest {
  sessionId?: string // sessionId可选，不带时默认新会话
  questionType: 'single' | 'multi' | 'input' | 'form'
  answer: any // 根据questionType不同，类型不同
  context?: Record<string, any>
}

// 表单答案项
export interface FormAnswerItem {
  id: string
  value: string[]
}

// API基础URL
const API_BASE = `${API_CONFIG.BASE_URL}/api/demo`
const USER_INTERACTION_BASE = `${API_CONFIG.BASE_URL}/api/user-interaction`
const CONVERSATION_BASE = `${API_CONFIG.BASE_URL}/api/conversation`

/**
 * 创建新的会话（真实版本 - 对接后端会话管理）
 */
export const startConversation = async (userId: string): Promise<ConversationSession> => {
  // 生成真实的会话ID
  
  // 返回会话信息
  return {
    sessionId: "",
    userId: userId,
    qaTree: null,
    currentNodeId: 'root',
    createdAt: new Date().toISOString()
  }
}

/**
 * 创建新的会话（Demo版本 - 直接返回模拟数据）
 */
export const startConversationDemo = async (userId: string): Promise<ConversationSession> => {
  // Demo版本：直接返回模拟的会话数据
  return {
    sessionId: userId,
    userId: userId,
    qaTree: null,
    currentNodeId: 'root',
    createdAt: new Date().toISOString()
  }
}

/**
 * 发送消息（Demo版本 - 使用SSE Demo的发送接口）
 */
export const sendMessage = async (request: MessageRequest): Promise<void> => {
  const url = `${API_BASE}/send/${request.sessionId}?message=${encodeURIComponent(request.content)}`
  await apiRequest(url, {
    method: 'POST',
    requireAuth: false
  })
}

/**
 * 获取会话信息（Demo版本）
 */
export const getSession = async (sessionId: string): Promise<ConversationSession> => {
  return {
    sessionId: sessionId,
    userId: sessionId,
    qaTree: null,
    currentNodeId: 'root',
    createdAt: new Date().toISOString()
  }
}

/**
 * 获取会话列表
 */
export const fetchSessionList = async (): Promise<SessionItem[]> => {
  const url = `${CONVERSATION_BASE}/sessions`
  const response = await apiRequest(url, {
    method: 'GET',
    requireAuth: false
  })
  return response.data
}

/**
 * 建立SSE连接（Demo版本 - 使用SSE Demo接口）
 */
export const connectSSE = (sessionId: string, onMessage: (response: MessageResponse) => void, onError?: (error: Event) => void): EventSource => {
  const url = `${API_BASE}/sse/${sessionId}`
  const eventSource = new EventSource(url)

  // 监听连接建立事件
  eventSource.addEventListener('connected', (event: MessageEvent) => {
  })

  // 监听普通消息
  eventSource.addEventListener('message', (event: MessageEvent) => {
    try {
      // Demo版本：将SSE消息转换为MessageResponse格式
      const response: MessageResponse = {
        nodeId: `ai_${Date.now()}`,
        content: event.data,
        type: 'AI_QUESTION',
        timestamp: Date.now()
      }
      onMessage(response)
    } catch (error) {
      console.error('解析SSE消息失败:', error)
    }
  })

  // 监听流式数据
  eventSource.addEventListener('stream', (event: MessageEvent) => {
    try {
      const response: MessageResponse = {
        nodeId: `stream_${Date.now()}`,
        content: event.data,
        type: 'AI_QUESTION',
        timestamp: Date.now()
      }
      onMessage(response)
    } catch (error) {
      console.error('解析流式消息失败:', error)
    }
  })

  // 监听广播消息
  eventSource.addEventListener('broadcast', (event: MessageEvent) => {
    try {
      const response: MessageResponse = {
        nodeId: `broadcast_${Date.now()}`,
        content: `[广播] ${event.data}`,
        type: 'SYSTEM_INFO',
        timestamp: Date.now()
      }
      onMessage(response)
    } catch (error) {
      console.error('解析广播消息失败:', error)
    }
  })

  eventSource.onerror = (error: Event) => {
    console.error('SSE连接错误:', error)
    if (onError) {
      onError(error)
    }
  }

  return eventSource
}

/**
 * 发送用户消息到用户交互接口
 * 对接后端的用户交互消息接口
 */
export const sendUserMessage = async (request: MessageRequest): Promise<void> => {
  // 构建统一答案请求格式
  const unifiedRequest: UnifiedAnswerRequest = {
    sessionId: request.sessionId || '', // sessionId可选，空字符串表示新会话
    questionType: 'input', // 普通文本消息作为input类型
    answer: request.content
  }
  
  const url = `${USER_INTERACTION_BASE}/message`
  await apiRequest(url, {
    method: 'POST',
    body: JSON.stringify(unifiedRequest),
    headers: {
      'Content-Type': 'application/json'
    },
    requireAuth: false
  })
}

/**
 * 处理统一答案请求
 * 对接后端的processAnswer接口
 */
export const processAnswer = async (request: UnifiedAnswerRequest): Promise<void> => {
  const url = `${USER_INTERACTION_BASE}/message`
  await apiRequest(url, {
    method: 'POST',
    body: JSON.stringify(request),
    headers: {
      'Content-Type': 'application/json'
    },
    requireAuth: false
  })
}

/**
 * 建立用户交互SSE连接
 * 对接后端的用户交互SSE接口（基于用户指纹）
 */
export const connectUserInteractionSSE = (onMessage: (response: any) => void, onError?: (error: Event) => void): EventSource => {
  // 后端SSE接口不需要参数，会自动通过HttpServletRequest生成或获取用户指纹
  const url = `${USER_INTERACTION_BASE}/sse`
  const eventSource = new EventSource(url)



  // 监听连接建立事件
  eventSource.addEventListener('connected', (event: MessageEvent) => {
    try {
      // 解析连接数据并传递给onMessage回调
      const connectionData = JSON.parse(event.data)
      onMessage(connectionData)
    } catch (error) {
      console.error('解析连接数据失败:', error)
      // 如果解析失败，传递原始数据
      onMessage({ type: 'connected', data: event.data })
    }
  })

  // 监听消息事件
  eventSource.addEventListener('message', (event: MessageEvent) => {
    try {
      const response: MessageResponse = JSON.parse(event.data)
      onMessage(response)
    } catch (error) {
      console.error('解析用户交互SSE消息失败:', error)
    }
  })

  // 监听成功消息事件
  eventSource.addEventListener('success', (event: MessageEvent) => {
    try {
      const response = JSON.parse(event.data)
      onMessage(response)
    } catch (error) {
      console.error('解析成功消息失败:', error)
    }
  })

  // 监听错误消息事件
  eventSource.addEventListener('error', (event: MessageEvent) => {
    try {
      const response = JSON.parse(event.data)
      onMessage(response)
    } catch (error) {
      console.error('解析错误消息失败:', error)
    }
  })

  eventSource.onerror = (error: Event) => {
    console.error('用户交互SSE连接错误:', error)
    if (onError) {
      onError(error)
    }
  }

  return eventSource
}

/**
 * 重试请求接口
 * 对接后端的retry接口
 */
export interface RetryRequest {
  nodeId: string
  sessionId: string
  whyretry: string
}

export const retryQuestion = async (request: RetryRequest): Promise<void> => {
  const url = `${USER_INTERACTION_BASE}/retry`
  await apiRequest(url, {
    method: 'POST',
    body: JSON.stringify(request),
    headers: {
      'Content-Type': 'application/json'
    },
    requireAuth: false
  })
}

/**
 * 关闭SSE连接
 */
export const closeSSE = (eventSource: EventSource | null) => {
  if (eventSource) {
    eventSource.close()
  }
}
