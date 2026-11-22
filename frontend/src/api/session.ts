import { useSyncExternalStore } from 'react'
import type { Session } from './types'

const storageKey = 'dreamshops.session'
const listeners = new Set<() => void>()

function readSession(): Session | null {
  try {
    const saved = JSON.parse(sessionStorage.getItem(storageKey) ?? 'null')
    return saved && Number.isSafeInteger(saved.id) && typeof saved.token === 'string' && saved.token
      ? saved : null
  } catch {
    return null
  }
}

let current = readSession()
const snapshot = () => current
const subscribe = (listener: () => void) => {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

export function setSession(session: Session | null) {
  current = session
  try {
    if (session) sessionStorage.setItem(storageKey, JSON.stringify(session))
    else sessionStorage.removeItem(storageKey)
  } catch {
    // The current tab can still use the in-memory session when storage is disabled.
  }
  listeners.forEach(listener => listener())
}

export function expireSession(token: string) {
  if (current?.token === token) setSession(null)
}

export function useSession() {
  return useSyncExternalStore(subscribe, snapshot)
}
