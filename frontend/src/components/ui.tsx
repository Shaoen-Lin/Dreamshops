import { AlertCircle, Box, ImageOff, Minus, Plus } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { productImageUrl } from '../api/shop'
import type { Product } from '../api/types'
import { price } from './format'

export function Loading({ label = '載入中…' }: { label?: string }) {
  return <div className="feedback" role="status"><span className="spinner" />{label}</div>
}

export function Failure({ error, retry }: { error: Error; retry?: () => void }) {
  return <div className="feedback error" role="alert">
    <AlertCircle size={21} /><span>{error.message}</span>
    {retry && <button className="text-link" onClick={retry}>重試</button>}
  </div>
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return <div className="empty-state">
    <Box size={28} strokeWidth={1.5} />
    <h2>{title}</h2>
    {children}
  </div>
}

export function PageHeading({ title, children }: { title: string; children?: ReactNode }) {
  return <header className="page-heading"><h1>{title}</h1>{children}</header>
}

export function ProductImage({ id, name, eager }: { id?: number; name: string; eager?: boolean }) {
  const [failedId, setFailedId] = useState<number>()
  return id && failedId !== id
    ? <img src={productImageUrl(id)} alt={name} loading={eager ? 'eager' : 'lazy'} onError={() => setFailedId(id)} />
    : <div className="image-placeholder" role="img" aria-label={`${name}暫無圖片`}><ImageOff size={32} strokeWidth={1.3} /></div>
}

export function ProductCard({ product }: { product: Product }) {
  return <article className="product-card">
    <Link to={`/products/${product.id}`} aria-label={`查看 ${product.name}`}>
      <div className="product-media">
        <ProductImage id={product.images?.[0]?.imageID} name={product.name} />
        {product.inventory < 1 && <span className="stock-tag">已售完</span>}
      </div>
      <div className="product-copy">
        <span className="brand-label">{product.brand}</span>
        <h3>{product.name}</h3>
        <p className="product-price">{price(product.price)}</p>
      </div>
    </Link>
  </article>
}

export function ProductGrid({ products }: { products: Product[] }) {
  return <div className="product-grid">{products.map(product => <ProductCard key={product.id} product={product} />)}</div>
}

export function Quantity({ value, max, onChange, label = '數量', disabled = false }: {
  value: number; max: number; onChange: (value: number) => void; label?: string; disabled?: boolean
}) {
  return <div className="stepper" role="group" aria-label={label}>
    <button type="button" aria-label={`減少${label}`} disabled={disabled || value <= 1} onClick={() => onChange(value - 1)}>
      <Minus size={16} />
    </button>
    <output aria-label={label}>{value}</output>
    <button type="button" aria-label={`增加${label}`} disabled={disabled || value >= max} onClick={() => onChange(value + 1)}>
      <Plus size={16} />
    </button>
  </div>
}
