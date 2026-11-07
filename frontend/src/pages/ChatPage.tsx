import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchHistory, streamChat } from '../api/client'
import type { ChatMessage, SourceChunk } from '../api/types'

function MessageBubble({ msg }: { msg: ChatMessage }) {
  const isUser = msg.role === 'user'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} fade-up`}>
      <div
        className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap ${
          isUser
            ? 'bg-indigo-500 text-white rounded-br-md'
            : 'bg-slate-900 border border-slate-800 rounded-bl-md'
        }`}
      >
        {msg.content}
        {!isUser && msg.sources.length > 0 && <Sources sources={msg.sources} />}
      </div>
    </div>
  )
}

function Sources({ sources }: { sources: SourceChunk[] }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="mt-3 pt-3 border-t border-slate-700/60">
      <button
        onClick={() => setOpen(!open)}
        className="text-xs text-indigo-300 hover:text-indigo-200 transition-colors"
      >
        {open ? '▾' : '▸'} {sources.length} source{sources.length === 1 ? '' : 's'}
      </button>
      {open && (
        <div className="mt-2 space-y-2">
          {sources.map((s) => (
            <div key={s.index} className="bg-slate-800/60 rounded-lg p-3 text-xs">
              <div className="flex justify-between items-center mb-1">
                <span className="font-medium text-slate-300">
                  [{s.index}] {s.docName}
                </span>
                {s.score != null && (
                  <span className="text-slate-500">{(s.score * 100).toFixed(0)}% match</span>
                )}
              </div>
              <p className="text-slate-400 line-clamp-3">{s.snippet}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function ChatPage() {
  const { kbId } = useParams<{ kbId: string }>()
  const id = Number(kbId)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    fetchHistory(id).then(setMessages).catch(() => setMessages([]))
  }, [id])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = async (e: React.FormEvent) => {
    e.preventDefault()
    const question = input.trim()
    if (!question || streaming) return

    setError(null)
    setInput('')
    setStreaming(true)

    const userMsg: ChatMessage = { id: `u-${Date.now()}`, role: 'user', content: question, sources: [] }
    const assistantId = `a-${Date.now()}`
    const assistantMsg: ChatMessage = { id: assistantId, role: 'assistant', content: '', sources: [] }
    setMessages((prev) => [...prev, userMsg, assistantMsg])

    await streamChat(id, question, {
      onSources: (sources) =>
        setMessages((prev) =>
          prev.map((m) => (m.id === assistantId ? { ...m, sources } : m)),
        ),
      onToken: (token) =>
        setMessages((prev) =>
          prev.map((m) => (m.id === assistantId ? { ...m, content: m.content + token } : m)),
        ),
      onDone: () => setStreaming(false),
      onError: (message) => {
        setError(message)
        setStreaming(false)
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId && !m.content
              ? { ...m, content: `⚠️ ${message}` }
              : m,
          ),
        )
      },
    })
  }

  return (
    <div className="min-h-full flex flex-col">
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-10">
        <div className="max-w-3xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link to={`/kb/${id}`} className="text-slate-400 hover:text-slate-200 text-sm">
            ← Documents
          </Link>
          <span className="text-sm text-slate-400">RAG chat</span>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto">
        <div className="max-w-3xl mx-auto px-4 py-6 space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-20 text-slate-500">
              <p className="text-4xl mb-3">💬</p>
              <p className="text-sm">Ask anything about the documents in this knowledge base.</p>
              <p className="text-xs mt-2">Answers include citations linking back to the source chunks.</p>
            </div>
          )}
          {messages.map((msg) => (
            <MessageBubble key={msg.id} msg={msg} />
          ))}
          {streaming && messages[messages.length - 1]?.content === '' && (
            <div className="text-slate-500 text-sm animate-pulse">Thinking…</div>
          )}
          <div ref={bottomRef} />
        </div>
      </div>

      <div className="border-t border-slate-800 bg-slate-900/60 backdrop-blur">
        <form onSubmit={send} className="max-w-3xl mx-auto px-4 py-4 flex gap-3">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask a question…"
            disabled={streaming}
            className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-4 py-3 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={streaming || !input.trim()}
            className="bg-indigo-500 hover:bg-indigo-400 disabled:opacity-40 rounded-xl px-5 text-sm font-medium transition-colors"
          >
            Send
          </button>
        </form>
        {error && <p className="max-w-3xl mx-auto px-4 pb-3 text-xs text-rose-400">{error}</p>}
      </div>
    </div>
  )
}
