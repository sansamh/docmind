import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import AuthPage from './pages/AuthPage'
import KbListPage from './pages/KbListPage'
import KbDetailPage from './pages/KbDetailPage'
import ChatPage from './pages/ChatPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <KbListPage />
          </RequireAuth>
        }
      />
      <Route
        path="/kb/:kbId"
        element={
          <RequireAuth>
            <KbDetailPage />
          </RequireAuth>
        }
      />
      <Route
        path="/kb/:kbId/chat"
        element={
          <RequireAuth>
            <ChatPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
