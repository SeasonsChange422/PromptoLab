import { apiJsonRequest, apiRequest } from './apiUtils'
import { API_CONFIG } from './apiConfig'

// 表单答案项类型
export interface FormAnswerItem {
  id: string
  value: string[]
}

// 统一答案请求类型
export interface UnifiedAnswerRequest {
  sessionId: string | null
  questionType: 'single' | 'multi' | 'input' | 'form'
  answer: string | string[] | FormAnswerItem[]
  context?: Record<string, any>
}

// API响应类型
export interface ApiResponse {
  success: boolean
  message: string
  data?: any
  timestamp?: number
}

// 消息响应类型
export interface MessageResponse {
  nodeId: string
  content: string
  type: 'USER_ANSWER' | 'AI_QUESTION' | 'AI_SELECTION_QUESTION' | 'SYSTEM_INFO'
  options?: string[]
  timestamp: number
}

// SSE状态类型
export interface SseStatus {
  connectedSessions: string[]
  totalConnections: number
  timestamp: number
}

// API基础URL
const API_BASE = `${API_CONFIG.BASE_URL}/api/user-interaction`

/**
 * 发送统一答案请求
 * 支持单选、多选、输入框、表单等多种问题类型的回答
 */
export const sendAnswer = async (request: UnifiedAnswerRequest): Promise<string> => {
  const url = `${API_BASE}/message`

  try {
    const response = await apiRequest(url, {
      method: 'POST',
      body: JSON.stringify(request),
      headers: {
        'Content-Type': 'application/json'
      },
      requireAuth: false
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.text()
  } catch (error) {
    console.error('发送答案请求失败:', error)
    throw error
  }
}

// SSE连接功能已迁移到 conversationApi.ts 中的 connectUserInteractionSSE 方法
// 请使用 import { connectUserInteractionSSE } from './conversationApi' 来建立SSE连接

/**
 * 获取SSE连接状态
 */
export const getSseStatus = async (): Promise<SseStatus> => {
  const url = `${API_BASE}/sse-status`

  try {
    const response = await apiJsonRequest<SseStatus>(url, {
      method: 'GET',
      requireAuth: false
    })

    return response
  } catch (error) {
    console.error('获取SSE状态失败:', error)
    throw error
  }
}

/**
 * 重试请求
 */
export const retryRequest = async (request: {
  nodeId: string
  sessionId: string
  whyretry: string
}): Promise<any> => {
  const url = `${API_BASE}/retry`

  try {
    const response = await apiJsonRequest<any>(url, {
      method: 'POST',
      body: JSON.stringify(request),
      headers: {
        'Content-Type': 'application/json'
      },
      requireAuth: false
    })

    return response
  } catch (error) {
    console.error('重试请求失败:', error)
    throw error
  }
}

/**
 * 生成提示词请求
 */
export const generatePrompt = async (request: {
  sessionId: string | null
  nodeId?: string
  answer?: any
}): Promise<string> => {
  const url = `${API_BASE}/gen-prompt`

  console.log('准备发送生成提示词请求:', { url, request });

  try {
    const response = await apiRequest(url, {
      method: 'POST',
      body: JSON.stringify(request),
      headers: {
        'Content-Type': 'application/json'
      },
      requireAuth: false
    })

    console.log('收到生成提示词响应:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.text()
  } catch (error) {
    console.error('生成提示词请求失败:', error)
    throw error
  }
}

/**
 * 对话历史响应类型
 */
export interface ConversationHistoryResponse {
  sessionId: string;
  userId: string;
  currentNode: string;
  createTime: string;
  updateTime: string;
  qaTree: any;
}

/**
 * 获取对话历史
 */
export const getConversationHistory = async (sessionId: string): Promise<ApiResult<ConversationHistoryResponse>> => {
  const url = `${API_BASE}/history?sessionId=${encodeURIComponent(sessionId)}`;

  try {
    const response = await apiJsonRequest<ApiResult<ConversationHistoryResponse>>(url, {
      method: 'GET',
      requireAuth: false
    });

    return response;
  } catch (error) {
    console.error('获取对话历史失败:', error);
    throw error;
  }
};

/**
 * 设置用户画像
 */
export const setUserProfile = async (request: {
  sessionId: string
  userId: string
  userProfile: string
}): Promise<boolean> => {
  try {
    console.log('发送设置用户画像请求:', request)

    const response = await apiJsonRequest(`${API_BASE}/set-user-profile`, {
      method: 'POST',
      body: JSON.stringify(request)
    })

    console.log('设置用户画像响应:', response)
    return response === true || response.success === true

  } catch (error) {
    console.error('设置用户画像请求异常:', error)
    throw error
  }
}

/**
 * 关闭SSE连接
 */
export const closeUserInteractionSSE = (eventSource: EventSource | null) => {
  if (eventSource) {
    eventSource.close();
    console.log('用户交互SSE连接已关闭');
  }
}

// 工具函数：创建不同类型的答案请求
export const createAnswerRequest = {
  /**
   * 创建单选题答案请求
   */
  single: (sessionId: string | null, selectedOption: string): UnifiedAnswerRequest => ({
    sessionId,
    questionType: 'single',
    answer: selectedOption // 直接传递格式化后的字符串
  }),

  /**
   * 创建多选题答案请求
   */
  multi: (sessionId: string | null, selectedOptions: string[]): UnifiedAnswerRequest => ({
    sessionId,
    questionType: 'multi',
    answer: selectedOptions // 直接传递格式化后的字符串数组
  }),

  /**
   * 创建输入框答案请求
   */
  input: (sessionId: string | null, inputText: string): UnifiedAnswerRequest => ({
    sessionId,
    questionType: 'input',
    answer: inputText
  }),

  /**
   * 创建表单答案请求
   */
  form: (sessionId: string | null, formAnswers: FormAnswerItem[]): UnifiedAnswerRequest => ({
    sessionId,
    questionType: 'form',
    answer: formAnswers
  })
}

// 导出所有类型和函数
export {
  type UnifiedAnswerRequest as UnifiedAnswerRequestType,
  type FormAnswerItem as FormAnswerItemType,
  type MessageResponse as MessageResponseType,
  type SseStatus as SseStatusType
}
