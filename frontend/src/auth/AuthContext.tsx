import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'
import { getStoredUser, getToken, login as apiLogin, logout as apiLogout, register as apiRegister } from '../api/client'
import type { User } from '../api/types'

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, name: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() =>
    getToken() ? getStoredUser() : null,
  )

  const value: AuthContextValue = {
    user,
    isAuthenticated: !!user,
    login: async (email, password) => {
      setUser(await apiLogin(email, password))
    },
    register: async (email, password, name) => {
      setUser(await apiRegister(email, password, name))
    },
    logout: () => {
      apiLogout()
      setUser(null)
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
