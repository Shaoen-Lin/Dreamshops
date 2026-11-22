import { Link, Route, Routes } from 'react-router-dom'
import { Layout } from './components/layout'
import { Empty } from './components/ui'
import { AccountPage } from './pages/account'
import { LoginPage, RegisterPage, RequireSession } from './pages/auth'
import { CartPage } from './pages/cart'
import { CatalogPage, HomePage, ProductPage } from './pages/catalog'
import { OrderPage, OrdersPage } from './pages/orders'

export function App() {
  return <Routes>
    <Route element={<Layout />}>
      <Route index element={<HomePage />} />
      <Route path="products" element={<CatalogPage />} />
      <Route path="products/:id" element={<ProductPage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="register" element={<RegisterPage />} />
      <Route element={<RequireSession />}>
        <Route path="account" element={<AccountPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="orders/:id" element={<OrderPage />} />
      </Route>
      <Route path="*" element={<div className="shell view">
        <Empty title="找不到這個頁面"><Link className="button" to="/">回到首頁</Link></Empty>
      </div>} />
    </Route>
  </Routes>
}
