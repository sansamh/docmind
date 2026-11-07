import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createKb, deleteKb, listKbs } from '../api/client'
import type { KnowledgeBase } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export default function KbListPage() {
  const { user, logout } = useAuth()
  const [kbs, setKbs] = useState<KnowledgeBase[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  useEffect(() => {
    listKbs()
      .then(setKbs)
      .finally(() => setLoading(false))
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    const kb = await createKb(name.trim(), description.trim())
    setKbs((prev) => [kb, ...prev])
    setName('')
    setDescription('')
    setCreating(false)
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this knowledge base and all its documents?')) return
    await deleteKb(id)
    setKbs((prev) => prev.filter((kb) => kb.id !== id))
  }

  return (
    <div className="min-h-full">
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-indigo-500 flex items-center justify-center text-sm font-bold">D</div>
            <span className="font-semibold">DocMind</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-slate-400">{user?.name}</span>
            <button onClick={logout} className="text-slate-400 hover:text-slate-200 transition-colors">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-10">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Knowledge bases</h1>
            <p className="text-slate-400 text-sm mt-1">
              Upload documents, then ask questions with cited answers.
            </p>
          </div>
          <button
            onClick={() => setCreating(true)}
            className="bg-indigo-500 hover:bg-indigo-400 rounded-lg px-4 py-2 text-sm font-medium transition-colors"
          >
            New knowledge base
          </button>
        </div>

        {creating && (
          <form
            onSubmit={handleCreate}
            className="bg-slate-900 border border-slate-800 rounded-xl p-4 mb-6 space-y-3 fade-up"
          >
            <input
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Name, e.g. Product Manuals"
              required
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
            />
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Description (optional)"
              rows={2}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
            />
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setCreating(false)}
                className="px-3 py-1.5 text-sm text-slate-400 hover:text-slate-200"
              >
                Cancel
              </button>
              <button type="submit" className="bg-indigo-500 hover:bg-indigo-400 rounded-lg px-4 py-1.5 text-sm font-medium">
                Create
              </button>
            </div>
          </form>
        )}

        {loading ? (
          <p className="text-slate-500 text-sm">Loading…</p>
        ) : kbs.length === 0 ? (
          <div className="text-center py-20 text-slate-500">
            <p className="text-4xl mb-3">📚</p>
            <p className="text-sm">No knowledge bases yet. Create one to get started.</p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {kbs.map((kb) => (
              <div
                key={kb.id}
                className="group bg-slate-900 border border-slate-800 hover:border-indigo-500/50 rounded-xl p-5 transition-colors fade-up"
              >
                <Link to={`/kb/${kb.id}`} className="block">
                  <h2 className="font-medium group-hover:text-indigo-300 transition-colors">{kb.name}</h2>
                  <p className="text-slate-400 text-sm mt-1 line-clamp-2 min-h-[2.5rem]">
                    {kb.description || 'No description'}
                  </p>
                  <p className="text-slate-500 text-xs mt-3">
                    {kb.documentCount} document{kb.documentCount === 1 ? '' : 's'}
                  </p>
                </Link>
                <div className="flex justify-end mt-2">
                  <button
                    onClick={() => handleDelete(kb.id)}
                    className="text-xs text-slate-600 hover:text-rose-400 transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
