import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function AuthPage() {
  const { login, register, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (isAuthenticated) {
    navigate('/', { replace: true })
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await register(email, password, name || email.split('@')[0])
      }
      navigate('/', { replace: true })
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 401) setError('Wrong email or password')
      else if (status === 409) setError('This email is already registered')
      else setError('Something went wrong, please try again')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-full flex items-center justify-center bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 px-4">
      <div className="w-full max-w-sm fade-up">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-500 flex items-center justify-center text-lg font-bold">
              D
            </div>
            <span className="text-2xl font-semibold tracking-tight">DocMind</span>
          </div>
          <p className="text-slate-400 text-sm">Upload documents. Ask questions. Get cited answers.</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl">
          <div className="flex rounded-lg bg-slate-800 p-1 mb-6 text-sm">
            {(['login', 'register'] as const).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => { setMode(m); setError(null) }}
                className={`flex-1 py-1.5 rounded-md transition-colors ${
                  mode === m ? 'bg-indigo-500 text-white' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {m === 'login' ? 'Sign in' : 'Create account'}
              </button>
            ))}
          </div>

          <form onSubmit={submit} className="space-y-4">
            {mode === 'register' && (
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2.5 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
              />
            )}
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2.5 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
            />
            <input
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password (min. 8 characters)"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2.5 text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
            />

            {error && <p className="text-sm text-rose-400">{error}</p>}

            <button
              type="submit"
              disabled={busy}
              className="w-full bg-indigo-500 hover:bg-indigo-400 disabled:opacity-50 rounded-lg py-2.5 text-sm font-medium transition-colors"
            >
              {busy ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
