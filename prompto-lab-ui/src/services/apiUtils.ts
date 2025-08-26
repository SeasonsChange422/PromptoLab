import { toast } from '@/utils/toast'

// API请求工具函数
export interface RequestOptions {
  method?: string
  headers?: Record<string, string>
  body?: string
  signal?: AbortSignal
  requireAuth?: boolean
}

// 获取存储的token
export function getAuthToken(): string | null {
  return localStorage.getItem('token')
}

// 清除认证信息
function clearAuthData() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

// 显示登录引导
function showLoginGuide(options: {
  title?: string
  description?: string
  autoRedirect?: boolean
} = {}) {
  // 简化的登录引导，直接跳转到登录页
  const message = options.description || '您的登录状态已过期，需要重新登录以继续使用'
  
  toast.error({
    title: options.title || '登录已过期',
    message,
    duration: 5000
  })
  
  // 跳转到登录页
  if (options.autoRedirect !== false) {
    setTimeout(() => {
      if (!window.location.pathname.includes('/auth')) {
        window.location.href = '/auth'
      }
    }, 3000)
  }
}

// 处理认证失败
function handleAuthFailure() {
  clearAuthData()
  showLoginGuide({
    title: '登录已过期',
    description: '您的登录状态已过期，为了保护您的账户安全，请重新登录',
    autoRedirect: true
  })
}

// 处理需要登录
function handleRequireLogin() {
  showLoginGuide({
    title: '需要登录',
    description: '此操作需要登录后才能继续，请先完成登录验证',
    autoRedirect: false
  })
}

// 统一的请求函数
// 获取指纹
function getFingerprint(): string | null {
  return localStorage.getItem('prompto_lab_fingerprint')
}

export async function apiRequest(url: string, options: RequestOptions = {}): Promise<Response> {
  const {
    method = 'GET',
    headers = {},
    body,
    signal,
    requireAuth = true
  } = options

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers
  }

  // 自动携带指纹
  const fingerprint = getFingerprint()
  if (fingerprint) {
    requestHeaders['X-Fingerprint'] = fingerprint
  }

  if (requireAuth) {
    const token = getAuthToken()
    // if (token) {
    //   requestHeaders['Authorization'] = `${token}`
    // } else {
    //   handleRequireLogin()
    // }
  }

  try {
    const response = await fetch(url, {
      method,
      headers: requestHeaders,
      body,
      signal
    })

    if (response.status === 401) {
      handleAuthFailure()
    }

    if (response.status === 403) {
      toast.error({
        title: '权限不足',
        message: '您没有权限执行此操作',
        duration: 4000
      })
    }

    if (response.status === 404) {
      toast.error({
        title: '资源不存在',
        message: '请求的资源不存在或已被删除',
        duration: 3000
      })
      throw new Error('资源不存在')
    }

    if (response.status >= 500) {
      toast.error({
        title: '服务器错误',
        message: '服务器暂时无法处理您的请求，请稍后重试',
        duration: 5000,
        closable: true
      })
      throw new Error('服务器错误')
    }

    return response
  } catch (error: any) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      toast.error({
        title: '网络连接失败',
        message: '无法连接到服务器，请检查网络连接',
        duration: 5000,
        closable: true
      })
    } else if (error.name === 'AbortError') {
      console.log('请求已取消')
    } else if (!error.message.includes('认证失败') &&
               !error.message.includes('权限不足') &&
               !error.message.includes('资源不存在') &&
               !error.message.includes('未登录')) {
      toast.error({
        title: '请求失败',
        message: error.message || '请求处理失败，请稍后重试',
        duration: 4000
      })
    }
    throw error
  }
}

// 便捷的JSON请求函数
export async function apiJsonRequest<T = any>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const response = await apiRequest(url, options)

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  try {
    return await response.json()
  } catch (error) {
    toast.error({
      title: '数据解析失败',
      message: '服务器返回的数据格式有误',
      duration: 3000
    })
    throw new Error('数据解析失败')
  }
}
