/**
 * SSE连接管理器
 * 全局管理SSE连接状态，避免重复建立连接
 */

interface SSEConnectionInfo {
  eventSource: EventSource | null
  fingerprint: string | null
  isConnected: boolean
  lastActivity: number
  connectionId: string
}

class SSEConnectionManager {
  private static instance: SSEConnectionManager
  private connectionInfo: SSEConnectionInfo | null = null
  private readonly CONNECTION_TIMEOUT = 5 * 60 * 1000 // 5分钟超时
  private cleanupInterval: number | null = null

  private constructor() {
    // 启动定期清理机制
    this.startCleanupInterval()
  }

  static getInstance(): SSEConnectionManager {
    if (!SSEConnectionManager.instance) {
      SSEConnectionManager.instance = new SSEConnectionManager()
    }
    return SSEConnectionManager.instance
  }

  /**
   * 检查是否已有活跃连接
   */
  hasActiveConnection(): boolean {
    if (!this.connectionInfo) {
      return false
    }

    const { eventSource, isConnected, lastActivity } = this.connectionInfo
    const now = Date.now()

    // 检查连接是否超时
    if (now - lastActivity > this.CONNECTION_TIMEOUT) {
      console.log('SSE连接已超时，清理连接信息')
      this.clearConnection()
      return false
    }

    // 检查EventSource状态
    if (!eventSource || eventSource.readyState !== EventSource.OPEN || !isConnected) {
      console.log('SSE连接状态无效，清理连接信息')
      this.clearConnection()
      return false
    }

    // 额外检查：页面刷新后EventSource可能失效但readyState仍为OPEN
    // 通过尝试访问EventSource的url属性来验证连接是否真正有效
    try {
      if (eventSource.url && eventSource.readyState === EventSource.OPEN) {
        // 连接看起来是有效的
        return true
      } else {
        console.log('EventSource URL无效，可能是页面刷新导致的连接失效')
        this.clearConnection()
        return false
      }
    } catch (error) {
      console.log('EventSource访问异常，清理连接信息:', error)
      this.clearConnection()
      return false
    }

    return true
  }

  /**
   * 获取当前连接信息
   */
  getConnectionInfo(): SSEConnectionInfo | null {
    return this.connectionInfo
  }

  /**
   * 注册新的SSE连接
   */
  registerConnection(
    eventSource: EventSource,
    fingerprint: string,
    isConnected: boolean = false
  ): string {
    // 如果已有连接，先清理
    if (this.connectionInfo?.eventSource) {
      console.log('清理旧的SSE连接')
      this.clearConnection()
    }

    const connectionId = `sse_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    
    this.connectionInfo = {
      eventSource,
      fingerprint,
      isConnected,
      lastActivity: Date.now(),
      connectionId
    }

    console.log(`SSE连接已注册: ${connectionId}`)
    return connectionId
  }

  /**
   * 更新连接状态
   */
  updateConnectionStatus(isConnected: boolean): void {
    if (this.connectionInfo) {
      this.connectionInfo.isConnected = isConnected
      this.connectionInfo.lastActivity = Date.now()
    }
  }

  /**
   * 更新活跃时间
   */
  updateActivity(): void {
    if (this.connectionInfo) {
      this.connectionInfo.lastActivity = Date.now()
    }
  }

  /**
   * 清理连接信息
   */
  clearConnection(): void {
    if (this.connectionInfo?.eventSource) {
      try {
        this.connectionInfo.eventSource.close()
      } catch (error) {
        console.warn('关闭EventSource时出错:', error)
      }
    }
    this.connectionInfo = null
    console.log('SSE连接信息已清理')
  }

  /**
   * 启动定期清理机制
   */
  private startCleanupInterval(): void {
    // 每30秒检查一次连接状态
    this.cleanupInterval = window.setInterval(() => {
      if (this.connectionInfo) {
        const now = Date.now()
        if (now - this.connectionInfo.lastActivity > this.CONNECTION_TIMEOUT) {
          console.log('定期清理：连接已超时')
          this.clearConnection()
        } else if (this.connectionInfo.eventSource) {
          try {
            if (this.connectionInfo.eventSource.readyState !== EventSource.OPEN) {
              console.log('定期清理：连接状态无效')
              this.clearConnection()
            }
          } catch (error) {
            console.log('定期清理：检查连接状态出错，清理连接', error)
            this.clearConnection()
          }
        }
      }
    }, 30000)
  }

  /**
   * 停止定期清理机制
   */
  private stopCleanupInterval(): void {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval)
      this.cleanupInterval = null
    }
  }

  /**
   * 获取连接状态信息（用于调试）
   */
  getDebugInfo(): any {
    if (!this.connectionInfo) {
      return { status: 'no_connection' }
    }

    const { eventSource, fingerprint, isConnected, lastActivity, connectionId } = this.connectionInfo
    return {
      status: 'has_connection',
      connectionId,
      fingerprint,
      isConnected,
      readyState: eventSource?.readyState,
      readyStateText: this.getReadyStateText(eventSource?.readyState),
      lastActivity: new Date(lastActivity).toISOString(),
      timeSinceLastActivity: Date.now() - lastActivity
    }
  }

  private getReadyStateText(readyState?: number): string {
    switch (readyState) {
      case EventSource.CONNECTING: return 'CONNECTING'
      case EventSource.OPEN: return 'OPEN'
      case EventSource.CLOSED: return 'CLOSED'
      default: return 'UNKNOWN'
    }
  }
}

// 导出单例实例
export const sseConnectionManager = SSEConnectionManager.getInstance()
export default sseConnectionManager