export type Money = number
export interface ApiResponse<T> { message: string; data: T }
export interface Category { id: number; name: string }
export interface ImageDto { imageID: number; imageName: string; downloadURL: string }
export interface Product {
  id: number
  name: string
  brand: string
  price: Money
  inventory: number
  description: string | null
  category: Category | null
  images: ImageDto[]
}

export interface LoginRequest { email: string; password: string }
export interface Session { id: number; token: string }
export interface NameFields { firstName: string; lastName: string }
export type RegisterRequest = LoginRequest & NameFields

export interface UserProfile extends NameFields {
  id: number
  email: string
  order: Order[] | null
  cart: {
    id: number
    totalAmount: Money
    items: { itemId: number; quantity: number; unitPrice: Money; product: Product }[]
  } | null
}

// The cart read endpoint serializes entities. The user endpoint returns CartDto.
export interface Cart {
  id: number
  totalAmount: Money
  items: CartItem[]
  user: NameFields & { id: number; email: string; roles: { id: number; name: string }[] }
}
export interface CartItem {
  id: number
  quantity: number
  unitPrice: Money
  totalPrice: Money
  product: Omit<Product, 'images'> & {
    images: { id: number; fileName: string; fileType: string; downloadUrl: string }[]
  }
}

export type OrderStatus = 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export interface Order {
  id: number
  userId: number
  orderDate: string
  totalAmount: Money
  status: OrderStatus
  items: {
    productId: number
    productName: string
    productBrand: string
    quantity: number
    price: Money
  }[]
}
