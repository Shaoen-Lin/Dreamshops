import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './app'
import { setSession } from './api/session'
import { shop } from './api/shop'
import type { Cart, Order, Product, UserProfile } from './api/types'

const session = { id: 14, token: 'test-session' }
const profile: UserProfile = {
  id: 14, firstName: '小夢', lastName: '陳', email: 'fixture@example.com',
  order: null, cart: null,
}
const product: Product = {
  id: 72, name: 'Studio Headphones', brand: 'Studio', description: '無線耳機',
  inventory: 8, price: 29.5, category: { id: 6, name: 'Audio' }, images: [],
}
const order: Order = {
  id: 4, userId: 14, orderDate: '2026-01-01T10:00:00', totalAmount: 59, status: 'PENDING',
  items: [{ productId: product.id, productName: product.name, productBrand: product.brand, quantity: 2, price: 29.5 }],
}

const json = (data: unknown, status = 200) => new Response(JSON.stringify({ message: 'test', data }), {
  status, headers: { 'Content-Type': 'application/json' },
})

const fetchMock = vi.fn<(url: string, init: RequestInit) => Promise<Response>>()

beforeEach(() => {
  window.scrollTo = vi.fn()
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  setSession(null)
})

afterEach(() => {
  act(() => setSession(null))
  vi.unstubAllGlobals()
})

function browser(path: string) {
  return render(<MemoryRouter initialEntries={[path]}><App /></MemoryRouter>)
}

describe('Dreamshop', () => {
  it('returns to a protected route after real-client login and reads an empty cart', async () => {
    fetchMock.mockImplementation(async (url, request) => {
      if (url.endsWith('/auth/login')) {
        expect(JSON.parse(request.body as string)).toEqual({ email: profile.email, password: 'test-password' })
        return json(session)
      }
      expect(url).toBe('/api/v1/users/14/user')
      expect(new Headers(request.headers).get('Authorization')).toBe('Bearer test-session')
      return json(profile)
    })
    const user = userEvent.setup()
    browser('/cart')
    await user.type(screen.getByLabelText('電子郵件'), profile.email)
    await user.type(screen.getByLabelText('密碼'), 'test-password')
    await user.click(screen.getByRole('button', { name: '登入' }))
    expect(await screen.findByRole('heading', { name: '購物車還沒有商品' })).toBeVisible()
  })

  it('shows invalid credentials and does not create an authenticated session', async () => {
    fetchMock.mockResolvedValue(json(null, 401))
    const user = userEvent.setup()
    browser('/login')
    await user.type(screen.getByLabelText('電子郵件'), profile.email)
    await user.type(screen.getByLabelText('密碼'), 'incorrect')
    await user.click(screen.getByRole('button', { name: '登入' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('電子郵件或密碼不正確')
    expect(sessionStorage.getItem('dreamshops.session')).toBeNull()
  })

  it('uses the category endpoint and narrows its results by text', async () => {
    fetchMock.mockImplementation(async url => {
      if (url.endsWith('/categories/all')) return json([product.category])
      if (url.endsWith('/product/Audio/all/products')) return json([product])
      throw new Error(`Unexpected route: ${url}`)
    })
    const user = userEvent.setup()
    browser('/products?category=Audio')
    expect(await screen.findByRole('link', { name: '查看 Studio Headphones' })).toBeVisible()
    await user.type(screen.getByRole('textbox', { name: '搜尋型號、品牌或商品' }), '其他')
    expect(await screen.findByRole('heading', { name: '找不到符合條件的商品' })).toBeVisible()
  })

  it('updates cart quantity and submits an order using the authenticated user ID', async () => {
    setSession(session)
    let quantity = 1
    const cart = (): Cart => ({
      id: 31, totalAmount: quantity * 29.5,
      items: [{ id: 3, quantity, unitPrice: 29.5, totalPrice: quantity * 29.5, product: { ...product, images: [] } }],
      user: { ...profile, roles: [] },
    })
    fetchMock.mockImplementation(async (url, request) => {
      switch (url) {
        case '/api/v1/users/14/user': return json({ ...profile, cart: { id: 31 } })
        case '/api/v1/carts/31/my-cart': return json(cart())
        case '/api/v1/cartItems/cart/31/item/72/update?quantity=2':
          expect(request.method).toBe('PUT')
          quantity = 2
          return json(null)
        case '/api/v1/orders/order?userId=14':
          expect(request.method).toBe('POST')
          expect(quantity).toBe(2)
          return json(order)
        case '/api/v1/orders/4/order': return json(order)
        default: throw new Error(`Unexpected route: ${url}`)
      }
    })
    const user = userEvent.setup()
    browser('/cart')
    await user.click(await screen.findByRole('button', { name: '增加Studio Headphones數量' }))
    const summary = await screen.findByRole('complementary', { name: '訂單摘要' })
    expect(within(summary).getByText('59.00')).toBeVisible()
    await user.click(screen.getByRole('button', { name: '送出訂單' }))
    expect(await screen.findByRole('heading', { name: '訂單 #4' })).toBeVisible()
    expect(screen.getByText('訂單已成立。')).toBeVisible()
  })

  it('clears an expired session and redirects from a protected route', async () => {
    setSession(session)
    fetchMock.mockResolvedValue(new Response('expired', { status: 401 }))
    browser('/account')
    expect(await screen.findByRole('heading', { name: '登入 Dreamshop' })).toBeVisible()
    expect(sessionStorage.getItem('dreamshops.session')).toBeNull()
  })

  it.each([
    [403, '你沒有權限使用這項功能。'],
    [404, '找不到你要的資料。'],
    [500, '商店服務暫時無法使用，請稍後再試。'],
  ])('maps API status %s to a readable message', async (status, message) => {
    fetchMock.mockResolvedValue(json(null, status))
    await expect(shop.product(72)).rejects.toMatchObject({ status, message })
  })
})
