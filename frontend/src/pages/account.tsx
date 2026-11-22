import { useCallback, useState, type FormEvent } from 'react'
import { LogOut, ReceiptText } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { setSession, useSession } from '../api/session'
import { shop } from '../api/shop'
import type { Session, UserProfile } from '../api/types'
import { useRequest } from '../api/use-request'
import { errorMessage } from '../components/format'
import { Failure, Loading, PageHeading } from '../components/ui'

export function AccountPage() {
  const session = useSession()
  const navigate = useNavigate()
  const load = useCallback((signal: AbortSignal) =>
    session ? shop.profile(session, signal) : Promise.reject(new Error('請先登入。')), [session])
  const { resource, reload } = useRequest(load)

  return <div className="shell view">
    <PageHeading title="我的帳戶"><p>檢視及更新你的個人資料。</p></PageHeading>
    <div className="account-layout">
      <div className="panel">
        {resource.phase === 'loading' && <Loading label="載入帳戶…" />}
        {resource.phase === 'error' && <Failure error={resource.error} retry={reload} />}
        {resource.phase === 'ready' && session && <ProfileForm key={session.id} initial={resource.data} session={session} />}
      </div>
      <aside className="panel account-actions" aria-label="帳戶功能">
        <Link className="button secondary" to="/orders"><ReceiptText size={18} />查看訂單紀錄</Link>
        <button className="button secondary" onClick={() => { setSession(null); navigate('/', { replace: true }) }}><LogOut size={18} />登出</button>
      </aside>
    </div>
  </div>
}

function ProfileForm({ initial, session }: { initial: UserProfile; session: Session }) {
  const [profile, setProfile] = useState(initial)
  const [firstName, setFirstName] = useState(initial.firstName)
  const [lastName, setLastName] = useState(initial.lastName)
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')
  const changed = firstName.trim() !== profile.firstName || lastName.trim() !== profile.lastName

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved(false)
    setError('')
    if (!firstName.trim() || !lastName.trim()) {
      setError('請填寫姓氏與名字。')
      return
    }
    setBusy(true)
    try {
      const updated = await shop.updateProfile(session, { firstName: firstName.trim(), lastName: lastName.trim() })
      setProfile(updated)
      setFirstName(updated.firstName)
      setLastName(updated.lastName)
      setSaved(true)
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <form className="stack-form" onSubmit={submit}>
    <h2>個人資料</h2>
    <label>電子郵件<input type="email" value={profile.email} readOnly /></label>
    <div className="name-fields">
      <label>姓氏<input autoComplete="family-name" required maxLength={100} readOnly={busy} value={lastName}
        onChange={event => { setLastName(event.target.value); setSaved(false) }} /></label>
      <label>名字<input autoComplete="given-name" required maxLength={100} readOnly={busy} value={firstName}
        onChange={event => { setFirstName(event.target.value); setSaved(false) }} /></label>
    </div>
    {saved && <p className="notice success" role="status">個人資料已更新。</p>}
    {error && <p className="notice error" role="alert">{error}</p>}
    <button className="button" disabled={busy || !changed}>{busy ? '儲存中…' : '儲存變更'}</button>
  </form>
}
