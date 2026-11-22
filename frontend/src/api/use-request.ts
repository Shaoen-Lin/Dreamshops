import { useCallback, useEffect, useState } from 'react'

type Resource<T> = { phase: 'loading' } | { phase: 'error'; error: Error } | { phase: 'ready'; data: T }

export function useRequest<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [version, setVersion] = useState(0)
  const [result, setResult] = useState<{ load: typeof load; version: number; resource: Resource<T> }>()
  const reload = useCallback(() => setVersion(value => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    const resolve = (resource: Resource<T>) => {
      if (!controller.signal.aborted) setResult({ load, version, resource })
    }
    load(controller.signal).then(
      data => resolve({ phase: 'ready', data }),
      error => resolve({ phase: 'error', error }),
    )
    return () => controller.abort()
  }, [load, version])

  const resource: Resource<T> = result?.load === load && result.version === version
    ? result.resource : { phase: 'loading' }
  return { resource, reload }
}
