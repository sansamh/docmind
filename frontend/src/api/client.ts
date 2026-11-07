import axios from 'axios'
import type { ChatMessage, KbDocument, KnowledgeBase, SourceChunk, User } from './types'

export const TOKEN_KEY = 'docmind_token'
export const USER_KEY = 'docmind_user'

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export async function register(email: string, password: string, name: string): Promise<User> {
  const { data } = await api.post('/auth/register', { email, password, name })
  return persistAuth(data)
}

export async function login(email: string, password: string): Promise<User> {
  const { data } = await api.post('/auth/login', { email, password })
  return persistAuth(data)
}

function persistAuth(data: { token: string; userId: number; email: string; name: string }): User {
  localStorage.setItem(TOKEN_KEY, data.token)
  const user: User = { userId: data.userId, email: data.email, name: data.name }
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function listKbs(): Promise<KnowledgeBase[]> {
  const { data } = await api.get('/kb')
  return data
}

export async function createKb(name: string, description: string): Promise<KnowledgeBase> {
  const { data } = await api.post('/kb', { name, description })
  return data
}

export async function deleteKb(id: number): Promise<void> {
  await api.delete(`/kb/${id}`)
}

export async function listDocuments(kbId: number): Promise<KbDocument[]> {
  const { data } = await api.get(`/kb/${kbId}/documents`)
  return data
}

export async function uploadDocument(kbId: number, file: File): Promise<KbDocument> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post(`/kb/${kbId}/documents`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function deleteDocument(kbId: number, docId: number): Promise<void> {
  await api.delete(`/kb/${kbId}/documents/${docId}`)
}

export async function fetchHistory(kbId: number): Promise<ChatMessage[]> {
  const { data } = await api.get(`/kb/${kbId}/chat/messages`)
  return data
}

export interface StreamHandlers {
  onSources: (sources: SourceChunk[]) => void
  onToken: (token: string) => void
  onDone: () => void
  onError: (message: string) => void
}

/**
 * POSTs the question and consumes the SSE response stream.
 * Events: sources (JSON array), token (text), done, error (JSON).
 */
export async function streamChat(
  kbId: number,
  question: string,
  handlers: StreamHandlers,
): Promise<void> {
  const token = getToken()
  let res: Response
  try {
    res = await fetch(`/api/kb/${kbId}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ question }),
    })
  } catch {
    handlers.onError('Network error — is the backend running?')
    return
  }

  if (!res.ok || !res.body) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch { /* keep default */ }
    handlers.onError(message)
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  const dispatch = (eventName: string, data: string) => {
    if (!eventName) return
    switch (eventName) {
      case 'sources': {
        try {
          handlers.onSources(JSON.parse(data) as SourceChunk[])
        } catch { /* ignore */ }
        break
      }
      case 'token':
        handlers.onToken(data)
        break
      case 'done':
        handlers.onDone()
        break
      case 'error': {
        let message = 'Generation failed'
        try {
          const parsed = JSON.parse(data)
          if (parsed?.message) message = parsed.message
        } catch { /* keep default */ }
        handlers.onError(message)
        break
      }
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let sep: number
    while ((sep = buffer.indexOf('\n\n')) !== -1) {
      const rawEvent = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)
      let eventName = ''
      const dataLines: string[] = []
      for (const line of rawEvent.split('\n')) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          // Spring's SseEmitter writes "data:" + payload verbatim (no protective
          // space), so keep everything after the colon
          dataLines.push(line.slice(5))
        }
      }
      dispatch(eventName, dataLines.join('\n'))
    }
  }
  handlers.onDone()
}
