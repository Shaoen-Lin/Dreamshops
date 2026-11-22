import { useCallback, useState } from 'react'
import { ChevronDown, LayoutGrid, Search, ShoppingBag, Tag } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { shop } from '../api/shop'
import { useSession } from '../api/session'
import { useRequest } from '../api/use-request'
import type { Product } from '../api/types'
import { errorMessage, price, routeId } from '../components/format'
import { Empty, Failure, Loading, PageHeading, ProductGrid, ProductImage, Quantity } from '../components/ui'

function categoryLink(name: string) {
  return `/products?${new URLSearchParams({ category: name })}`
}

export function HomePage() {
  const products = useRequest(shop.products)
  const categories = useRequest(shop.categories)

  return <>
    <div className="shell">
      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-content">
          <span className="hero-label">DREAMSHOPS 科技選物</span>
          <h1 id="hero-title">日常科技，<br />精心挑選。</h1>
          <p>探索音訊、穿戴裝置與配件，找到適合你日常所需的好物。</p>
          <Link className="button hero-button" to="/products">開始選購</Link>
        </div>
      </section>
      <section className="home-section" aria-labelledby="categories-title">
        <div className="section-heading">
          <h2 id="categories-title">依分類探索</h2>
        </div>
        {categories.resource.phase === 'loading' && <Loading label="載入分類…" />}
        {categories.resource.phase === 'error' && <Failure error={categories.resource.error} retry={categories.reload} />}
        {categories.resource.phase === 'ready' && (
          categories.resource.data.length
            ? <div className="category-grid">{categories.resource.data.map(category => <Link key={category.id} to={categoryLink(category.name)}>
                <span className="category-icon"><Tag size={20} strokeWidth={1.7} /></span>{category.name}
              </Link>)}</div>
            : <p className="muted">目前沒有商品分類。</p>
        )}
      </section>
      <section className="home-section featured" aria-labelledby="featured-title">
        <div className="section-heading">
          <h2 id="featured-title">精選商品</h2>
          <Link className="text-link" to="/products">查看所有商品</Link>
        </div>
        {products.resource.phase === 'loading' && <Loading label="載入商品…" />}
        {products.resource.phase === 'error' && <Failure error={products.resource.error} retry={products.reload} />}
        {products.resource.phase === 'ready' && (
          products.resource.data.length
            ? <ProductGrid products={[...products.resource.data].sort((a, b) => Number(b.inventory > 0) - Number(a.inventory > 0) || a.id - b.id).slice(0, 4)} />
            : <Empty title="目前沒有上架商品" />
        )}
      </section>
    </div>
  </>
}

export function filterProducts(products: Product[], query: string, sort: string) {
  const words = query.toLocaleLowerCase().trim().split(/\s+/).filter(Boolean)
  const filtered = products.filter(product => {
    const haystack = [product.name, product.brand, product.description, product.category?.name].join(' ').toLocaleLowerCase()
    return words.every(word => haystack.includes(word))
  })
  if (sort === 'price-asc') return filtered.sort((a, b) => a.price - b.price)
  if (sort === 'price-desc') return filtered.sort((a, b) => b.price - a.price)
  return filtered
}

export function CatalogPage() {
  const [params, setParams] = useSearchParams()
  const category = params.get('category') ?? ''
  const query = params.get('q') ?? ''
  const sort = params.get('sort') ?? ''
  const loadProducts = useCallback((signal: AbortSignal) =>
    category ? shop.productsIn(category, signal) : shop.products(signal), [category])
  const products = useRequest(loadProducts)
  const categories = useRequest(shop.categories)

  function update(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    setParams(next, { replace: key === 'q' })
  }

  const visible = products.resource.phase === 'ready' ? filterProducts(products.resource.data, query, sort) : []

  return <div className="shell view">
    <PageHeading title="探索商品"><p>瀏覽科技用品，依分類與關鍵字找到你需要的商品。</p></PageHeading>
    <div className="catalog-controls">
      <label className="search-field"><Search size={19} /><span className="sr-only">搜尋型號、品牌或商品</span>
        <input id="catalog-search" placeholder="搜尋商品、品牌或型號" value={query} onChange={event => update('q', event.target.value)} />
      </label>
      <label className="select-field"><span className="sr-only">商品排序</span>
        <select value={sort} onChange={event => update('sort', event.target.value)}>
          <option value="">預設排序</option>
          <option value="price-asc">價格由低到高</option>
          <option value="price-desc">價格由高到低</option>
        </select>
        <ChevronDown size={17} aria-hidden="true" />
      </label>
    </div>
    <div className="filter-list" role="group" aria-label="商品分類">
      <button className="filter" aria-pressed={!category} onClick={() => update('category', '')}><LayoutGrid size={16} />全部商品</button>
      {categories.resource.phase === 'ready' && categories.resource.data.map(item =>
        <button className="filter" key={item.id} aria-pressed={category === item.name} onClick={() => update('category', item.name)}>{item.name}</button>
      )}
    </div>
    {categories.resource.phase === 'error' && <Failure error={categories.resource.error} retry={categories.reload} />}
    {products.resource.phase === 'loading' && <Loading label="載入商品…" />}
    {products.resource.phase === 'error' && <Failure error={products.resource.error} retry={products.reload} />}
    {products.resource.phase === 'ready' && <>
      <p className="results-count" role="status">共 {visible.length} 件商品</p>
      {visible.length ? <ProductGrid products={visible} /> : <Empty title="找不到符合條件的商品">
        {(query || category) && <button className="button secondary" onClick={() => setParams({})}>清除篩選</button>}
      </Empty>}
    </>}
  </div>
}

export function ProductPage() {
  const id = routeId(useParams().id)
  const load = useCallback((signal: AbortSignal) => id
    ? shop.product(id, signal)
    : Promise.reject(new Error('找不到這件商品。')), [id])
  const { resource, reload } = useRequest(load)

  return <div className="shell view">
    <p className="breadcrumbs"><Link to="/products">探索商品</Link><span aria-hidden="true">/</span>商品詳情</p>
    {resource.phase === 'loading' && <Loading label="載入商品…" />}
    {resource.phase === 'error' && <Failure error={resource.error} retry={id ? reload : undefined} />}
    {resource.phase === 'ready' && <ProductDetail key={resource.data.id} product={resource.data} />}
  </div>
}

function ProductDetail({ product }: { product: Product }) {
  const [index, setIndex] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [busy, setBusy] = useState(false)
  const [added, setAdded] = useState(false)
  const [error, setError] = useState('')
  const session = useSession()
  const location = useLocation()
  const navigate = useNavigate()
  const images = product.images ?? []

  async function add() {
    if (!session) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    setBusy(true)
    setError('')
    setAdded(false)
    try {
      await shop.addItem(session, product.id, quantity)
      setAdded(true)
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <article className="product-detail">
    <div>
      <div className="detail-image"><ProductImage id={images[index]?.imageID} name={product.name} eager /></div>
      {images.length > 1 && <div className="thumbnails" aria-label="商品圖片">
        {images.map((image, position) => <button key={image.imageID} aria-pressed={position === index}
          aria-label={`查看商品圖片 ${position + 1}`} onClick={() => setIndex(position)}>
          <ProductImage id={image.imageID} name={product.name} />
        </button>)}
      </div>}
    </div>
    <div className="detail-copy">
      {product.category && <Link className="text-link" to={categoryLink(product.category.name)}>{product.category.name}</Link>}
      <h1>{product.name}</h1>
      <p className="brand-label">{product.brand}</p>
      <p className="detail-price">{price(product.price)}</p>
      <p className="description">{product.description || '此商品尚無描述。'}</p>
      <div className="detail-buy">
        <p className={product.inventory > 0 ? 'in-stock' : 'out-of-stock'}>{product.inventory > 0 ? `庫存 ${product.inventory} 件` : '已售完'}</p>
        <div className="purchase-actions">
          <Quantity value={quantity} max={product.inventory} disabled={busy || product.inventory < 1}
            onChange={value => { setQuantity(value); setAdded(false) }} />
          <button className="button" disabled={busy || product.inventory < 1} onClick={add}>
            <ShoppingBag size={19} />{busy ? '加入中…' : '加入購物車'}
          </button>
        </div>
        {added && <p className="notice success" role="status">已加入購物車。 <Link className="text-link" to="/cart">查看購物車</Link></p>}
        {error && <p className="notice error" role="alert">{error}</p>}
      </div>
    </div>
  </article>
}
