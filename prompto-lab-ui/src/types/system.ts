
export interface SystemOverview {
  totalModels: number
  enabledModels: number
  totalOperations: number
  enabledOperations: number
  [property: string]: number
}

export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data?: T
  timestamp?: number
  code?: number
}

export interface ModelConfigData {
  modelName: string
  baseUrl: string
  apiKey: string
  defaultMaxTokens?: number
  defaultTemperature?: number
  supportStream?: boolean
  supportJsonOutput?: boolean
  supportThinking?: boolean
  additionalHeaders?: Record<string, string>
  description?: string
  provider?: string
  enabled?: boolean
}

export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data?: T
  modelName?: string
  validated?: boolean
  error?: string
}

export interface ModelsResponse {
  models: Record<string, ModelConfigData>
  groupedByProvider: Record<string, ModelConfigData[]>
  total: number
}

export interface TestConnectionResponse {
  success: boolean
  message: string
  modelName: string
}

export interface ModelListResponse {
  models: Record<string, ModelConfigData>
  groupedByProvider: Record<string, ModelConfigData[]>
  total: number
}