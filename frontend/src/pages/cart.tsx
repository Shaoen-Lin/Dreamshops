import { useCallback, useState } from 'react'
import { ShoppingBag, Trash2 } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useSession } from '../api/session'
import { shop } from '../api/shop'
import type { Cart, Session } from '../api/types'
import { useRequest } from '../api/use-request'
import { errorMessage, price } from '../components/format'
import { Empty, Failure, Loading, PageHeading, ProductImage, Quantity } from '../components/ui'

export function CartPage() {
  const session = useSession()
  const load = useCallback(async (signal: AbortSignal) => {
    if (!session) throw new Error('請先登入。')
    const profile = await shop.profile(session, signal)
    return profile.cart?.id ? shop.cart(session, profile.cart.id, signal) : null
  }, [session])
  const { resource, reload } = useRequest(load)

  return <div className="shell view">
    <PageHeading title="購物車" />
    {resource.phase === 'loading' && <Loading label="載入購物車…" />}
    {resource.phase === 'error' && <Failure error={resource.error} retry={reload} />}
    {resource.phase === 'ready' && (resource.data?.items.length && session
      ? <CartContents cart={resource.data} session={session} reload={reload} />
      : <Empty title="購物車還沒有商品"><Link className="button" to="/products">探索商品</Link></Empty>)}
  </div>
}

function CartContents({ cart, session, reload }: { cart: Cart; session: Session; reload: () => void }) {
  const navigate = useNavigate()
  const [pending, setPending] = useState<'item' | 'order' | null>(null)
  const [error, setError] = useState('')
  const [checkoutError, setCheckoutError] = useState(false)
  const unavailable = cart.items.some(item => item.quantity < 1 || item.quantity > item.product.inventory)

  async function edit(action: () => Promise<null>) {
    setPending('item')
    setError('')
    setCheckoutError(false)
    try {
      await action()
      reload()
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setPending(null)
    }
  }

  async function checkout() {
    setPending('order')
    setError('')
    setCheckoutError(false)
    try {
      const order = await shop.checkout(session)
      navigate(`/orders/${order.id}`, { replace: true, state: { confirmed: true } })
    } catch (error) {
      setCheckoutError(true)
      setError(errorMessage(error))
    } finally {
      setPending(null)
    }
  }

  return <div className="cart-layout">
    <section className="panel cart-items" aria-label="購物車商品" aria-busy={pending === 'item'}>
      {cart.items.map(item => <article className="cart-row" key={item.id}>
        <Link className="cart-image" to={`/products/${item.product.id}`} aria-label={`查看 ${item.product.name}`}>
          <ProductImage id={item.product.images?.[0]?.id} name={item.product.name} />
        </Link>
        <div className="cart-product">
          <span className="brand-label">{item.product.brand}</span>
          <h2><Link to={`/products/${item.product.id}`}>{item.product.name}</Link></h2>
          <p className="muted">單價 {price(item.unitPrice)}</p>
          {item.quantity > item.product.inventory && <p className="out-of-stock">
            {item.product.inventory > 0 ? `僅剩 ${item.product.inventory} 件` : '已售完'}
          </p>}
        </div>
        <Quantity value={item.quantity} max={item.product.inventory} disabled={pending !== null || item.product.inventory < 1}
          label={`${item.product.name}數量`}
          onChange={quantity => void edit(() => shop.updateItem(session, cart.id, item.product.id, Math.min(quantity, item.product.inventory)))} />
        <p className="line-total">{price(item.totalPrice)}</p>
        <button className="icon-link remove-button" aria-label={`移除 ${item.product.name}`} disabled={pending !== null}
          onClick={() => void edit(() => shop.removeItem(session, cart.id, item.product.id))}><Trash2 size={18} /></button>
      </article>)}
      <Link className="text-link" to="/products">繼續選購</Link>
    </section>
    <aside className="panel order-summary" aria-label="訂單摘要">
      <h2>訂單摘要</h2>
      <dl><div><dt>商品數量</dt><dd>{cart.items.reduce((count, item) => count + item.quantity, 0)} 件</dd></div>
        <div className="summary-total"><dt>商品總額</dt><dd>{price(cart.totalAmount)}</dd></div></dl>
      {unavailable && <p className="notice error" role="alert">請調整庫存不足的商品，再送出訂單。</p>}
      <button className="button" disabled={pending !== null || unavailable || checkoutError} onClick={() => void checkout()}>
        <ShoppingBag size={18} />{pending === 'order' ? '送出中…' : '送出訂單'}
      </button>
      {error && <p className="notice error" role="alert">{error}</p>}
      {checkoutError && <div className="recovery-actions">
        <Link className="text-link" to="/orders">查看訂單紀錄</Link>
        <button className="text-link" onClick={reload}>重新載入購物車</button>
      </div>}
    </aside>
  </div>
}
