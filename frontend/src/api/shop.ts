import { expireSession } from './session'
import type {
  ApiResponse, Cart, Category, LoginRequest, NameFields, Order, Product, RegisterRequest, Session, UserProfile,
} from './types'

const base = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/+$/, '')

const messages: Record<number, string> = {
  400: '請確認輸入內容是否正確。',
  401: '登入已失效，請重新登入。',
  403: '你沒有權限使用這項功能。',
  404: '找不到你要的資料。',
  409: '資料已變更，請確認後再試一次。',
}

export class ApiError extends Error {
  constructor(readonly status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

type Options = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: object
  signal?: AbortSignal
  session?: Session
  errors?: Record<number, string>
}

async function request<T>(path: string, { method = 'GET', body, signal, session, errors }: Options = {}): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' })
  if (session) headers.set('Authorization', `Bearer ${session.token}`)
  if (body) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(base + path, { method, headers, signal, body: body && JSON.stringify(body) })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ApiError(0, '無法連線到商店，請稍後再試。')
  }

  if (!response.ok) {
    if (response.status === 401 && session) expireSession(session.token)
    throw new ApiError(response.status, errors?.[response.status] ?? messages[response.status] ?? '商店服務暫時無法使用，請稍後再試。')
  }

  try {
    const result: ApiResponse<T> = await response.json()
    if (!result || !Object.hasOwn(result, 'data')) throw new Error('Missing response data')
    return result.data
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ApiError(response.status, '無法讀取商店回傳的資料。')
  }
}

export const shop = {
  products: (signal?: AbortSignal) => request<Product[]>('/products/all', { signal }),
  product: (id: number, signal?: AbortSignal) => request<Product>(`/products/product/${id}/product`, { signal }),
  categories: (signal?: AbortSignal) => request<Category[]>('/categories/all', { signal }),
  async productsIn(category: string, signal?: AbortSignal) {
    try {
      return await request<Product[]>(`/products/product/${encodeURIComponent(category)}/all/products`, { signal })
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return []
      throw error
    }
  },
  login: (body: LoginRequest) => request<Session>('/auth/login', {
    method: 'POST', body, errors: { 401: '電子郵件或密碼不正確。' },
  }),
  register: (body: RegisterRequest) => request<UserProfile>('/users/add', {
    method: 'POST', body, errors: { 409: '這個電子郵件已註冊。' },
  }),
  profile: (session: Session, signal?: AbortSignal) =>
    request<UserProfile>(`/users/${session.id}/user`, { session, signal }),
  updateProfile: (session: Session, body: NameFields) =>
    request<UserProfile>(`/users/${session.id}/update`, { session, method: 'PUT', body }),
  cart: (session: Session, cartId: number, signal?: AbortSignal) =>
    request<Cart>(`/carts/${cartId}/my-cart`, { session, signal }),
  addItem: (session: Session, productId: number, quantity: number) =>
    request<number>(`/cartItems/item/add?productId=${productId}&quantity=${quantity}`, { session, method: 'POST', errors: { 409: '購物車中的商品數量已達庫存上限。' } }),
  updateItem: (session: Session, cartId: number, productId: number, quantity: number) =>
    request<null>(`/cartItems/cart/${cartId}/item/${productId}/update?quantity=${quantity}`, { session, method: 'PUT', errors: { 409: '商品庫存不足，請重新檢查數量。' } }),
  removeItem: (session: Session, cartId: number, productId: number) =>
    request<null>(`/cartItems/cart/${cartId}/item/${productId}/remove`, { session, method: 'DELETE' }),
  checkout: (session: Session) => request<Order>(`/orders/order?userId=${session.id}`, {
    session, method: 'POST', errors: { 409: '購物車為空或商品庫存不足，請重新檢查購物車。' },
  }),
  orders: (session: Session, signal?: AbortSignal) =>
    request<Order[]>(`/orders/${session.id}/orders`, { session, signal }),
  order: (session: Session, orderId: number, signal?: AbortSignal) =>
    request<Order>(`/orders/${orderId}/order`, { session, signal }),
}

export function productImageUrl(id: number) {
  return `${base}/images/image/download/${id}`
}
