import { useCallback } from 'react'
import { CheckCircle2, ReceiptText } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { useSession } from '../api/session'
import { shop } from '../api/shop'
import type { Order, OrderStatus } from '../api/types'
import { useRequest } from '../api/use-request'
import { dateTime, price, routeId } from '../components/format'
import { Empty, Failure, Loading, PageHeading } from '../components/ui'

const statuses: Record<OrderStatus, string> = {
  PENDING: '待處理', PROCESSING: '處理中', SHIPPED: '已出貨', DELIVERED: '已送達', CANCELLED: '已取消',
}

function Status({ order }: { order: Order }) {
  return <span className="order-status">{statuses[order.status] ?? order.status ?? '狀態未提供'}</span>
}

export function OrdersPage() {
  const session = useSession()
  const load = useCallback((signal: AbortSignal) =>
    session ? shop.orders(session, signal) : Promise.reject(new Error('請先登入。')), [session])
  const { resource, reload } = useRequest(load)

  return <div className="shell view">
    <PageHeading title="我的訂單"><p>查看訂單狀態與購買明細。</p></PageHeading>
    {resource.phase === 'loading' && <Loading label="載入訂單…" />}
    {resource.phase === 'error' && <Failure error={resource.error} retry={reload} />}
    {resource.phase === 'ready' && (resource.data.length
      ? <div className="order-list">{[...resource.data].sort((a, b) => b.orderDate.localeCompare(a.orderDate) || b.id - a.id).map(order =>
        <article className="panel order-row" key={order.id}>
          <span className="category-icon"><ReceiptText size={22} strokeWidth={1.6} /></span>
          <div><h2>訂單 #{order.id}</h2><time dateTime={order.orderDate}>{dateTime(order.orderDate)}</time></div>
          <Status order={order} />
          <strong>{price(order.totalAmount)}</strong>
          <Link className="button secondary" to={`/orders/${order.id}`}>查看明細<span className="sr-only">，訂單 {order.id}</span></Link>
        </article>)}</div>
      : <Empty title="目前沒有訂單"><Link className="button" to="/products">開始選購</Link></Empty>
    )}
  </div>
}

export function OrderPage() {
  const session = useSession()
  const id = routeId(useParams().id)
  const { state } = useLocation()
  const load = useCallback((signal: AbortSignal) => session && id
    ? shop.order(session, id, signal)
    : Promise.reject(new Error('找不到這筆訂單。')), [session, id])
  const { resource, reload } = useRequest(load)

  return <div className="shell view">
    <p className="breadcrumbs"><Link to="/orders">我的訂單</Link><span aria-hidden="true">/</span>訂單明細</p>
    {state?.confirmed && <p className="confirmation" role="status"><CheckCircle2 size={22} />訂單已成立。</p>}
    {resource.phase === 'loading' && <Loading label="載入訂單明細…" />}
    {resource.phase === 'error' && <Failure error={resource.error} retry={id ? reload : undefined} />}
    {resource.phase === 'ready' && <OrderDetail order={resource.data} />}
  </div>
}

function OrderDetail({ order }: { order: Order }) {
  return <>
    <PageHeading title={`訂單 #${order.id}`}><time dateTime={order.orderDate}>{dateTime(order.orderDate)}</time></PageHeading>
    <section className="panel detail-order" aria-label="訂單商品">
      <div className="section-heading"><h2>商品明細</h2><Status order={order} /></div>
      <ul>{order.items.map(item => <li key={item.productId}>
        <div><h3><Link to={`/products/${item.productId}`}>{item.productName}</Link></h3>
          <p className="muted">{item.productBrand} · {item.quantity} 件</p></div>
        <div className="order-item-price"><strong>{price(item.price * item.quantity)}</strong>
          <p className="muted">單價 {price(item.price)}</p></div>
      </li>)}</ul>
      <div className="summary-total"><span>訂單總額</span><strong>{price(order.totalAmount)}</strong></div>
    </section>
  </>
}
