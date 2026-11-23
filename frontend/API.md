# Dreamshop REST API 合約

本文件以本機 Java 原始碼為準，涵蓋八個 Controller。後端是 Java 21、Spring Boot 3.5.16、Spring MVC、Spring Security、Spring Data JPA/Hibernate、PostgreSQL、ModelMapper、JJWT 與 Springdoc。Controller 負責 HTTP 契約，Service 負責購物流程與 DTO 轉換，Repository 使用 Spring Data JPA，Entity 定義資料庫與關聯。

- API 前綴由 `api.prefix` 設定，預設 `/api/v1`。下表路徑均相對於此前綴。
- PostgreSQL 預設為 `localhost:5432/dream_shops_db`，Hibernate 預設使用 `ddl-auto=update`。使用 Spring 環境變數覆蓋資料庫連線資訊。
- 後端缺少專用 CORS 設定。前端預設使用同源代理。
- 登入使用電子郵件與 BCrypt 密碼驗證，回傳 HS256 JWT。JWT 包含電子郵件 `sub`、`id`、`roles`、`iat`、`exp`，預設效期 3,600,000 ms。受保護請求傳送 `Authorization: Bearer <token>`。Spring Security 驗證簽章與有效期，再比對資料庫使用者的 ID。已刪除帳戶及新註冊的同電子郵件帳戶均無法使用舊 JWT。
- 除圖片下載外，成功回應通常是 HTTP `200` 與 `{"message":"…","data":…}`。表中列出 `data` 的型別。 `✓` 表示商店前端使用。

## 登入與會員

| 方法與路徑 | 輸入 | 成功 `data` | 權限 | 特定錯誤 | 前端 |
| --- | --- | --- | --- | --- | --- |
| `POST /auth/login` | JSON `{email,password}`，兩者必填 | `{id,token}` | 公開 | 400 驗證失敗，401 驗證憑證失敗 | ✓ |
| `POST /users/add` | JSON `{firstName,lastName,email,password}` | UserDto | 公開 | 409 電子郵件已存在 | ✓ |
| `GET /users/{id}/user` | 路徑使用者 ID | UserDto | 本人 | 404 使用者不存在 | ✓ |
| `PUT /users/{userId}/update` | JSON `{firstName,lastName}` | UserDto | 本人 | 404 使用者不存在 | ✓ |
| `DELETE /users/{userId}/delete` | 路徑使用者 ID | `null` | 本人 | 404 使用者不存在 | |

## 商品

商品新增與更新的 JSON 欄位為 `{id?,name,brand,price,inventory,description,category:{name}}`，更新以路徑中的 `id` 為準。分類查詢依完整名稱比對。結果為空時，`/products/all` 回傳空陣列，篩選查詢回傳 404。

| 方法與路徑 | 輸入 | 成功 `data` | 權限 | 特定錯誤 | 前端 |
| --- | --- | --- | --- | --- | --- |
| `GET /products/all` | 無 | ProductDto[] | 公開 | | ✓ |
| `GET /products/product/{id}/product` | 路徑商品 ID | ProductDto | 公開 | 404 | ✓ |
| `GET /products/product/{category}/all/products` | 路徑分類名 | ProductDto[] | 公開 | 404、500 | ✓ |
| `GET /products/product/{name}/products` | 路徑商品名 | ProductDto[] | 公開 | 404、500 | |
| `GET /products/product/by-brand` | 查詢 `brand` | ProductDto[] | 公開 | 404、500 | |
| `GET /products/products/by/brand-and-name` | 查詢 `brandName,productName` | ProductDto[] | 公開 | 404、500 | |
| `GET /products/products/by/category-and-brand` | 查詢 `category,brand` | ProductDto[] | 公開 | 404、500 | |
| `GET /products/products/count/by-brand/and-name` | 查詢 `brand,name` | 數字 | 公開 | 500 | |
| `POST /products/add` | 商品 JSON | ProductDto | `ROLE_ADMIN` | 409 同名同品牌商品 | |
| `PUT /products/product/{id}/update` | 商品 JSON | ProductDto | `ROLE_ADMIN` | 404 | |
| `DELETE /products/product/{id}/delete` | 路徑商品 ID | 商品 ID | `ROLE_ADMIN` | 404 | |

## 分類與圖片

| 方法與路徑 | 輸入 | 成功 `data` | 權限 | 特定錯誤 | 前端 |
| --- | --- | --- | --- | --- | --- |
| `GET /categories/all` | 無 | Category[] | 公開 | 500 | ✓ |
| `GET /categories/category/{id}/category` | 路徑分類 ID | Category | 公開 | 兩個路由樣式重疊，實際可能回傳 500 | |
| `GET /categories/category/{name}/category` | 路徑分類名 | Category 或 `null` | 公開 | 同上 | |
| `POST /categories/add` | JSON `{name}` | Category | `ROLE_ADMIN` | 409 重複分類 | |
| `PUT /categories/category/{id}/update` | JSON `{name}` | Category | `ROLE_ADMIN` | 404 | |
| `DELETE /categories/category/{id}/delete` | 路徑分類 ID | `null` | `ROLE_ADMIN` | 404 | |
| `GET /images/image/download/{imageID}` | 路徑圖片 ID | 原始二進位內容，使用圖片的 Content-Type | 公開 | 404、500 | ✓ |
| `POST /images/upload` | multipart `files`（可多檔）、`productID` | ImageDto[] | `ROLE_ADMIN` | 500 上傳處理失敗 | |
| `POST /images/image/{imageID}/update` | multipart `file` | `null` | `ROLE_ADMIN` | 404、500 | |
| `DELETE /images/image/{imageID}/update` | 路徑圖片 ID | `null` | `ROLE_ADMIN` | 404、500 | |

上傳的單檔與請求大小上限均為 5 MB。下載路由使用 `Content-Disposition: attachment`。安全清單內的點陣圖片維持原 MIME，其他格式以二進位附件下載。

## 購物車與訂單

購物車首次加入商品時建立，車號由新增回應或 `UserDto.cart.id` 取得。路徑有車號的操作限擁有者。缺少購物車時，會員資料回傳 `cart: null`。

| 方法與路徑 | 輸入 | 成功 `data` | 權限 | 特定錯誤 | 前端 |
| --- | --- | --- | --- | --- | --- |
| `GET /carts/{cartId}/my-cart` | 路徑車號 | Cart | 購物車本人 | 403 非擁有者或車號不存在 | ✓ |
| `GET /carts/{cartId}/cart/total-price` | 路徑車號 | 數字 | 購物車本人 | 403 非擁有者或車號不存在 | |
| `DELETE /carts/{cartId}/clear` | 路徑車號 | `null` | 購物車本人 | 403、404 | |
| `POST /cartItems/item/add` | 查詢 `productId,quantity` | 車號 | 已登入 | 400、404、409 | ✓ |
| `PUT /cartItems/cart/{cartId}/item/{productId}/update` | 路徑車號及商品 ID，查詢 `quantity` | `null` | 購物車本人 | 400、403、404、409 | ✓ |
| `DELETE /cartItems/cart/{cartId}/item/{productId}/remove` | 路徑車號及商品 ID | `null` | 購物車本人 | 403、404 | ✓ |
| `POST /orders/order` | 查詢 `userId` | OrderDto | 本人 | 404 無購物車，409 空車或庫存不足 | ✓ |
| `GET /orders/{userId}/orders` | 路徑使用者 ID | OrderDto[] | 本人 | 沒有訂單時回傳空陣列 | ✓ |
| `GET /orders/{orderId}/order` | 路徑訂單 ID | OrderDto | 訂單本人 | 404 訂單不存在或屬於其他使用者 | ✓ |

## 資料欄位

| 型別 | JSON 欄位 |
| --- | --- |
| Category | `{id,name}` |
| ImageDto | `{imageID,imageName,downloadURL}` |
| ProductDto | `{id,name,brand,price,inventory,description,category,images:ImageDto[]}` |
| UserDto | `{id,firstName,lastName,email,order:OrderDto[] 或 null,cart:CartDto 或 null}`。歷史以訂單端點為準。 |
| CartDto | `{id,totalAmount,items:[{itemId,quantity,unitPrice,product}]}` |
| Cart | `{id,totalAmount,user,items:[{id,quantity,unitPrice,totalPrice,product}]}`。此處商品的圖片欄位為 `{id,fileName,fileType,downloadUrl}`，使用者只含基本資料及角色。 |
| OrderDto | `{id,userId,orderDate,totalAmount,status,items:[{productId,productName,productBrand,quantity,price}]}` |
| 訂單狀態 | `PENDING`、`PROCESSING`、`SHIPPED`、`DELIVERED`、`CANCELLED`。新訂單為 `PENDING`。 |

ID、數量與 BigDecimal 金額序列化為 JSON 數字。訂單日期使用無時區的 ISO 本地日期時間。圖片下載由實際圖片 ID 和設定的 API base URL 組成。

一般錯誤包含 `400`（輸入缺漏、格式或驗證錯誤）、`401`（登入失敗、需要登入或 token 無效）、`403`（角色或擁有權不足）、`404`（找不到資源）、`409`（衝突、重複、庫存不足）、`500`（非預期例外）。錯誤回應可能是 API envelope、Spring Boot 的錯誤物件或 JWT entry point 的錯誤物件。前端以狀態碼提供中文錯誤狀態。

## 本次必要的後端變更

| 檔案或類別 | 變更與原因 |
| --- | --- |
| `pom.xml` | 升級至 Spring Boot 3.5.16 與 JJWT 0.13.0，套用同系列可取得的修補並更新 JWT parser。 |
| `application.properties`、`DataInitializer` | 將內建 JWT 與資料庫憑證改用環境變數，移除固定帳密，保留自選憑證的一次性管理員建立。乾淨資料庫預設可啟動。 |
| `ShopConfig`、`AuthTokenFilter`、`JwtUtils`、`ShopUserDetailsService` | API 安全規則使用設定前綴，保護會員、購物車與訂單，限制目錄管理角色並保留原錯誤狀態。JWT 維持 HS256 和原欄位，簽章、有效期、電子郵件及帳戶 ID 經後端驗證。 |
| `UserController`、`CartController`、`CartItemController`、`OrderController` | 已登入使用者限操作自己的資料。新增購物車商品取用登入身分，數量需為正數。原 HTTP 路徑、參數及成功 envelope 維持一致。 |
| `CartRepository`、`UserRepository`、`CartService`、`ICartService`、`CartItemService`、`ICartItemService`、`Cart` | 購物車擁有權查詢、交易鎖、首次建立與購物車項目變更使用同一交易。讀取購物車取消多餘寫入，數量與總額依目前品項更新。 |
| `OrderRepository`、`ProductRepository`、`OrderService`、`IOrderService` | 依擁有者查找訂單。結帳於同一交易鎖定購物車、按產品 ID 順序執行條件式庫存扣減、建立訂單並清空購物車。任一商品缺貨時整筆回滾。訂單 DTO 明確填入 ID、狀態和明細。 |
| `ProductService`、`ImageService`、`ImageController` | 對應圖片 ID、檔名及 URL，批次上傳以單一交易保存，下載 URL 使用 API 前綴。下載回應隔離，限制可渲染的 MIME。 |
| `User`、`Image` | 購物車實體回應排除密碼雜湊與圖片原始 BLOB。 |
| `GlobalExceptionHandler` | 遺失商品與圖片回傳 404。 |
| `ShopIntegrationTests` | 用 PostgreSQL 驗證使用者隔離、管理員權限、舊 JWT、併發新增與結帳、庫存回滾。 |
