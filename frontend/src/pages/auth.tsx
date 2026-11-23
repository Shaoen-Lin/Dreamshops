import { useState, type FormEvent } from 'react'
import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { setSession, useSession } from '../api/session'
import { shop } from '../api/shop'
import { errorMessage } from '../components/format'

function destination(from: unknown) {
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.includes('\\')
    ? from : '/account'
}

export function RequireSession() {
  const session = useSession()
  const { pathname, search } = useLocation()
  return session ? <Outlet /> : <Navigate to="/login" replace state={{ from: pathname + search, protected: true }} />
}

export function LoginPage() {
  const session = useSession()
  const location = useLocation()
  const target = destination(location.state?.from)
  const [email, setEmail] = useState<string>(location.state?.email ?? '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (session) return <Navigate to={target} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true)
    setError('')
    try {
      setSession(await shop.login({ email: email.trim(), password: String(form.get('password')) }))
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <div className="shell view auth-view">
    <section className="panel auth-panel" aria-labelledby="login-title">
      <header><h1 id="login-title">登入 Dreamshop</h1><p>登入以管理購物車與訂單。</p></header>
      {location.state?.registered && <p className="notice success" role="status">帳戶已建立，請登入。</p>}
      {location.state?.protected && <p className="notice" role="status">請先登入，再繼續操作。</p>}
      <form className="stack-form" onSubmit={submit}>
        <label>電子郵件<input name="email" type="email" autoComplete="email" required value={email} onChange={event => setEmail(event.target.value)} /></label>
        <label>密碼<input name="password" type="password" autoComplete="current-password" required /></label>
        {error && <p className="notice error" role="alert">{error}</p>}
        <button className="button" disabled={busy}>{busy ? '登入中…' : '登入'}</button>
      </form>
      <p className="auth-switch">還沒有帳戶？ <Link className="text-link" to="/register" state={{ from: target }}>建立帳戶</Link></p>
    </section>
  </div>
}

export function RegisterPage() {
  const session = useSession()
  const navigate = useNavigate()
  const location = useLocation()
  const target = destination(location.state?.from)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (session) return <Navigate to={target} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const firstName = String(form.get('firstName')).trim()
    const lastName = String(form.get('lastName')).trim()
    const email = String(form.get('email')).trim()
    const password = String(form.get('password'))
    if (!firstName || !lastName) {
      setError('請填寫姓氏與名字。')
      return
    }
    if (password !== form.get('confirm')) {
      setError('兩次輸入的密碼不一致。')
      return
    }
    setBusy(true)
    setError('')
    try {
      await shop.register({ firstName, lastName, email, password })
      navigate('/login', { replace: true, state: { from: target, email, registered: true } })
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <div className="shell view auth-view">
    <section className="panel auth-panel" aria-labelledby="register-title">
      <header><h1 id="register-title">建立帳戶</h1><p>用你的電子郵件開始選購。</p></header>
      <form className="stack-form" onSubmit={submit}>
        <div className="name-fields">
          <label>姓氏<input name="lastName" autoComplete="family-name" required maxLength={100} /></label>
          <label>名字<input name="firstName" autoComplete="given-name" required maxLength={100} /></label>
        </div>
        <label>電子郵件<input name="email" type="email" autoComplete="email" required maxLength={254} /></label>
        <label>密碼<input name="password" type="password" autoComplete="new-password" required /></label>
        <label>確認密碼<input name="confirm" type="password" autoComplete="new-password" required /></label>
        {error && <p className="notice error" role="alert">{error}</p>}
        <button className="button" disabled={busy}>{busy ? '建立中…' : '註冊'}</button>
      </form>
      <p className="auth-switch">已經有帳戶？ <Link className="text-link" to="/login" state={{ from: target }}>登入</Link></p>
    </section>
  </div>
}
