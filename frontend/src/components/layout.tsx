import { Menu, Search, ShoppingBag, UserRound, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useSession } from '../api/session'

function Brand() {
  return <Link className="wordmark" to="/" aria-label="Dreamshop 首頁">
    <span className="brand-mark"><ShoppingBag size={18} strokeWidth={2.2} /></span>
    <span>Dream<span>shop</span></span>
  </Link>
}

function Header() {
  const [menu, setMenu] = useState(false)
  const [query, setQuery] = useState('')
  const navigate = useNavigate()
  const session = useSession()

  return <header className="site-header"><div className="shell header-inner">
    <Brand />
    <nav className={menu ? 'main-nav menu-open' : 'main-nav'} aria-label="主選單" id="main-nav">
      <NavLink to="/" end onClick={() => setMenu(false)}>首頁</NavLink>
      <NavLink to="/products" onClick={() => setMenu(false)}>探索商品</NavLink>
      {session && <NavLink to="/orders" onClick={() => setMenu(false)}>我的訂單</NavLink>}
    </nav>
    <div className="header-actions">
      <form className="header-search" role="search" onSubmit={event => {
        event.preventDefault()
        const params = new URLSearchParams()
        if (query.trim()) params.set('q', query.trim())
        navigate({ pathname: '/products', search: params.toString() })
        setQuery('')
      }}>
        <label className="sr-only" htmlFor="global-search">搜尋商品</label>
        <Search size={16} aria-hidden="true" />
        <input id="global-search" placeholder="搜尋商品" value={query} onChange={event => setQuery(event.target.value)} />
      </form>
      <Link className="mobile-search icon-link" to="/products#catalog-search" aria-label="搜尋商品"><Search size={20} /></Link>
      <Link className="icon-link" to={session ? '/account' : '/login'} aria-label={session ? '我的帳戶' : '登入'}><UserRound size={20} /></Link>
      <Link className="icon-link" to="/cart" aria-label="購物車"><ShoppingBag size={20} /></Link>
      <button className="mobile-menu icon-link" type="button" aria-controls="main-nav" aria-expanded={menu}
        aria-label={menu ? '關閉選單' : '開啟選單'} onClick={() => setMenu(value => !value)}>
        {menu ? <X size={22} /> : <Menu size={22} />}
      </button>
    </div>
  </div></header>
}

export function Layout() {
  const { pathname, hash } = useLocation()
  useEffect(() => {
    if (hash === '#catalog-search') {
      document.getElementById('catalog-search')?.focus()
    } else {
      window.scrollTo(0, 0)
    }
  }, [pathname, hash])

  return <>
    <a className="skip-link" href="#main">跳至主要內容</a>
    <Header />
    <main id="main" className="site-main" tabIndex={-1}><Outlet /></main>
    <footer className="site-footer"><div className="shell footer-inner">
      <Brand />
      <p>日常科技選物</p>
      <nav aria-label="頁尾選單">
        <Link to="/products">所有商品</Link>
        <Link to="/account">我的帳戶</Link>
        <Link to="/orders">訂單紀錄</Link>
      </nav>
    </div></footer>
  </>
}
