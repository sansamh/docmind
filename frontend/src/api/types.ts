export interface User {
  userId: number
  email: string
  name: string
}

export interface KnowledgeBase {
  id: number
  name: string
  description: string | null
  documentCount: number
  createdAt: string
}

export interface KbDocument {
  id: number
  fileName: string
  fileType: string
  sizeBytes: number
  status: 'PROCESSING' | 'READY' | 'FAILED'
  chunkCount: number
  errorMessage: string | null
  createdAt: string
}

export interface SourceChunk {
  index: number
  docName: string
  snippet: string
  score: number | null
}

export interface ChatMessage {
  id: number | string
  role: 'user' | 'assistant'
  content: string
  sources: SourceChunk[]
  createdAt?: string
}
