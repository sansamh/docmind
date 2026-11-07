import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { deleteDocument, listDocuments, uploadDocument } from '../api/client'
import type { KbDocument } from '../api/types'

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const STATUS_STYLES: Record<KbDocument['status'], string> = {
  PROCESSING: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  READY: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  FAILED: 'bg-rose-500/15 text-rose-300 border-rose-500/30',
}

export default function KbDetailPage() {
  const { kbId } = useParams<{ kbId: string }>()
  const id = Number(kbId)
  const [docs, setDocs] = useState<KbDocument[]>([])
  const [uploading, setUploading] = useState(false)
  const [dragOver, setDragOver] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)

  const refresh = useCallback(async () => {
    const list = await listDocuments(id)
    setDocs(list)
    return list
  }, [id])

  useEffect(() => {
    let timer: ReturnType<typeof setInterval>
    const poll = async () => {
      const list = await refresh()
      // keep polling while any document is still processing
      if (!list.some((d) => d.status === 'PROCESSING')) {
        clearInterval(timer)
      }
    }
    poll()
    timer = setInterval(poll, 2000)
    return () => clearInterval(timer)
  }, [refresh])

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return
    setUploading(true)
    try {
      for (const file of Array.from(files)) {
        await uploadDocument(id, file)
      }
      await refresh()
      setIntervalPollAgain()
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  // restart the polling loop after a new upload
  const setIntervalPollAgain = () => {
    const timer = setInterval(async () => {
      const list = await listDocuments(id).catch(() => [])
      setDocs(list)
      if (!list.some((d) => d.status === 'PROCESSING')) clearInterval(timer)
    }, 2000)
  }

  const handleDelete = async (docId: number) => {
    await deleteDocument(id, docId)
    setDocs((prev) => prev.filter((d) => d.id !== docId))
  }

  const readyCount = docs.filter((d) => d.status === 'READY').length

  return (
    <div className="min-h-full">
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-10">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link to="/" className="text-slate-400 hover:text-slate-200 text-sm">← Knowledge bases</Link>
          </div>
          {readyCount > 0 && (
            <Link
              to={`/kb/${id}/chat`}
              className="bg-indigo-500 hover:bg-indigo-400 rounded-lg px-4 py-1.5 text-sm font-medium transition-colors"
            >
              Open chat
            </Link>
          )}
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-xl font-semibold mb-6">Documents</h1>

        <label
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => { e.preventDefault(); setDragOver(false); handleFiles(e.dataTransfer.files) }}
          className={`block border-2 border-dashed rounded-xl p-10 text-center cursor-pointer transition-colors ${
            dragOver ? 'border-indigo-400 bg-indigo-500/10' : 'border-slate-700 hover:border-slate-500'
          }`}
        >
          <input
            ref={fileInput}
            type="file"
            multiple
            accept=".pdf,.docx,.txt,.md,.csv,.json"
            className="hidden"
            onChange={(e) => handleFiles(e.target.files)}
          />
          <p className="text-slate-300 text-sm font-medium">
            {uploading ? 'Uploading…' : 'Drop files here or click to upload'}
          </p>
          <p className="text-slate-500 text-xs mt-1">PDF, DOCX, TXT, MD, CSV, JSON — up to 20 MB each</p>
        </label>

        <div className="mt-6 space-y-2">
          {docs.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">
              No documents yet. Upload a file to build your knowledge base.
            </p>
          ) : (
            docs.map((doc) => (
              <div
                key={doc.id}
                className="flex items-center gap-4 bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 fade-up"
              >
                <div className="w-9 h-9 rounded-lg bg-slate-800 flex items-center justify-center text-xs uppercase font-semibold text-slate-400 shrink-0">
                  {doc.fileType || '?'}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{doc.fileName}</p>
                  <p className="text-xs text-slate-500">
                    {formatSize(doc.sizeBytes)}
                    {doc.status === 'READY' && ` · ${doc.chunkCount} chunks indexed`}
                    {doc.status === 'FAILED' && doc.errorMessage && ` · ${doc.errorMessage}`}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded-md border shrink-0 ${STATUS_STYLES[doc.status]}`}>
                  {doc.status === 'PROCESSING' ? 'Indexing…' : doc.status}
                </span>
                <button
                  onClick={() => handleDelete(doc.id)}
                  className="text-xs text-slate-600 hover:text-rose-400 transition-colors shrink-0"
                >
                  Delete
                </button>
              </div>
            ))
          )}
        </div>
      </main>
    </div>
  )
}
