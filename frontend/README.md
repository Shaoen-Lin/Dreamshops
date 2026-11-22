# DreamShops 商店前端

繁體中文的響應式 React 商店，使用 TypeScript、Vite、Tailwind CSS、React Router 與 Fetch。首頁、商品、購物車、帳戶與訂單使用同一套 Spring Boot API。

## 本機執行

需要 Java 21、PostgreSQL、Node.js 22.13+、24.x 或 26+，以及 npm。先建立 `dream_shops_db`。以下本機 Docker 範例會產生一次性的資料庫密碼。已有 PostgreSQL 時，可以跳過 Docker 並設定對應的 `SPRING_DATASOURCE_*`。

```sh
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/dream_shops_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD="$(openssl rand -hex 24)"
export POSTGRES_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
docker run -d --rm --name dreamshops-db \
  -e POSTGRES_PASSWORD -e POSTGRES_DB=dream_shops_db \
  -p 127.0.0.1:5432:5432 postgres:16
unset POSTGRES_PASSWORD
```

在同一個終端機，從儲存庫根目錄啟動後端。

```sh
export AUTH_TOKEN_JWTSECRET="$(openssl rand -base64 32)"
cd dream-shops
sh mvnw spring-boot:run
```

Spring Boot 預設提供 `http://127.0.0.1:8080/api/v1`。Hibernate 建立缺少的資料表。後端必須取得至少 32 bytes 的 Base64 JWT 金鑰，每次換金鑰會使先前的登入失效。新資料庫預設為空，顧客帳戶在商店頁面註冊。

若需操作商品、分類或圖片管理 API，請在啟動後端前設定 `SHOP_BOOTSTRAP_ADMIN_EMAIL` 與 `SHOP_BOOTSTRAP_ADMIN_PASSWORD`。兩個變數同時設定才會建立管理員，密碼必須為 12 至 72 UTF-8 bytes。請以單一實例執行一次，建立後移除 bootstrap 設定。已存在的非管理員帳戶保留原權限。沿用舊資料庫時，公開前應先移除或停用舊版初始化的示範帳戶，並輪換曾存於 Git 的有效簽章金鑰及資料庫密碼。

另開終端機，在儲存庫根目錄啟動前端。

```sh
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

開啟 [本機商店](http://localhost:5173)。可從畫面註冊新帳戶。乾淨資料庫的商品目錄為空，請用既有的管理員商品 API 和圖片上傳 API 加入測試商品。可在後端的 [Swagger UI](http://localhost:8080/swagger-ui/index.html) 操作，或使用下列範例。資料庫中既有的商品與圖片也會直接顯示於前端。

### 建立一筆本機測試商品

從儲存庫根目錄執行。需要 `curl`、`jq` 與前述已建立的管理員帳戶。輸入密碼時，終端機會隱藏輸入。

```sh
api=http://127.0.0.1:8080/api/v1
printf '本機管理員電子郵件：'
read -r demo_email
printf '本機管理員密碼：'
read -rs demo_password
printf '\n'
demo_token=$(jq -n --arg email "$demo_email" --arg password "$demo_password" '{email:$email,password:$password}' |
  curl -fsS "$api/auth/login" -H 'Content-Type: application/json' -d @- |
  jq -er '.data.token')
unset demo_password
product_id=$(curl -fsS "$api/products/add" \
  -H "Authorization: Bearer $demo_token" -H 'Content-Type: application/json' \
  -d '{"name":"Apple Watch","brand":"Apple","description":"本機測試用穿戴裝置","inventory":10,"price":129.00,"category":{"name":"Wearables"}}' |
  jq -er '.data.id')
curl -fsS "$api/images/upload" \
  -H "Authorization: Bearer $demo_token" \
  -F "productID=$product_id" -F 'files=@e-commerce images/Apple Watch 1.jpg'
unset demo_token
```

若資料庫已存在同名同品牌商品，建立商品會回傳 `409`，可改用既有商品的 ID 上傳圖片。

## 功能與專案結構

- **首頁與目錄**提供分類入口、精選商品、關鍵字搜尋和價格排序。分類使用正式 API，關鍵字和排序套用在伺服器回傳的清單上。
- **商品詳情**顯示真實圖片、描述、庫存、價格和數量選擇。沒有圖片時顯示圖示。
- **會員**提供註冊、登入、登出、受保護路由和姓名更新。JWT 儲存在目前分頁的 `sessionStorage`，重新整理可保留登入。收到目前登入的 API `401` 時會清除該登入。分頁中的 JavaScript 可讀取 JWT。正式 Nginx 設定限制頁面腳本來源，登出清除分頁中的 token。
- **購物車與訂單**提供新增、變更數量、移除、後端回傳的金額、送出訂單、確認訊息、歷史和明細。

| 路徑 | 用途 |
| --- | --- |
| `src/api/` | API 路徑、型別、JWT 工作階段、可取消的讀取與重試 |
| `src/components/` | 導覽、版型、商品卡、數量控制和狀態元件 |
| `src/pages/` | 商店、商品、登入、註冊、帳戶、購物車與訂單 |
| `src/app.tsx`、`src/app.test.tsx` | 路由與使用者流程測試 |
| `public/shop-hero.jpg` | 商店首頁主視覺 |
| `Dockerfile`、`nginx.conf.template` | 正式建置、同源 API 代理、React Router 深層連結 |

API 端點、回傳欄位、錯誤和後端變更見 [API.md](API.md)。

## 環境與正式建置

| 變數 | 階段 | 用途 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Vite 開發及建置 | 瀏覽器使用的 API 路徑，預設 `/api/v1`。會寫入公開前端資產，只放公開設定。 |
| `API_PROXY_TARGET` | Vite 開發及本機 preview | 代理的 Spring Boot origin，預設 `http://127.0.0.1:8080`。 |
| `API_UPSTREAM` | Nginx 容器啟動 | 後端 origin，預設 `http://backend:8080`。填可從容器解析和連線的 origin，省略 API 路徑和結尾斜線。 |

開發時由 Vite 代理，容器中由 Nginx 轉送 `/api/`，瀏覽器可使用同源請求。若建置時指定完整的跨網域 API URL，需先在 API 端設定允許前端來源與 Authorization 標頭的 CORS 規則。後端的資料庫憑證和 JWT 金鑰應留在後端環境。

```sh
npm run typecheck
npm run lint
npm test
npm run build
npm run preview
```

`npm run build` 產出 `dist/`。從儲存庫根目錄建置並以 Docker Desktop 連到主機上的 Spring Boot。

```sh
docker build -t dreamshops-frontend ./frontend
docker run --rm -p 127.0.0.1:4180:8080 \
  -e API_UPSTREAM=http://host.docker.internal:8080 dreamshops-frontend
```

後端在 Docker Desktop 的主機模式下須可由容器連線。若後端也在容器中，把兩個容器放在同一個 Docker 網路，並將 `API_UPSTREAM` 指向後端的容器 DNS 名稱。

## 驗證結果

- Node.js 22.14.0 執行 `npm run typecheck`、`npm run lint`、`npm test` 和 `npm run build` 全部通過。Vitest 共 8 項，涵蓋登入、路由保護、401 清除登入、商品搜尋、購物車送單及錯誤提示。
- Java 21 搭配隔離 PostgreSQL 16 通過 Maven 建置及 7 項測試，包含啟動、JWT 與擁有者授權、管理操作保護、併發首次加入購物車、重複結帳、最後庫存競爭與交易回滾。後端整合測試會新增測試資料，請使用隔離 PostgreSQL。
- Chrome 透過本機 Vite 連到真實 API，驗證商品分類與圖片、關鍵字、註冊、登入失敗與成功、購物車數量與移除、結帳後庫存與空車、訂單歷史和明細、改名、登出及手機導覽。
- Docker 映像建置成功。Chrome 在正式容器重跑上述流程，並驗證直接開啟深層路由、同源 API 代理、庫存上限提示、缺少商品的 404，以及 320px 版面。

## 已知限制

- 後端提供精確名稱、品牌與分類查詢，缺少全文搜尋與分頁。前端的關鍵字搜尋使用已載入的清單，商品量增長時應加入後端搜尋和分頁。
- 訂單 API 會建立待處理訂單、扣減庫存並清空購物車。付款、運送地址、追蹤、取消與退款尚無 API。現有流程尚無跨請求的結帳冪等鍵。現有交易會鎖定購物車並以 PostgreSQL 條件式更新扣庫存，失敗會回滾整筆訂單。
- 會員 API 可更新姓氏與名字。電子郵件修改、忘記密碼、刷新 token 和伺服器端登出尚無 API。訂單和商品金額由後端提供，後端尚無幣別欄位。
- 商店介面以顧客操作為主。商品管理仍使用既有的管理員 API，分類與圖片寫入同樣需要管理員。

## GitHub 與 GCP 後續

1. 為 PR 加入 GitHub Actions，前端執行 `npm ci`、型別檢查、lint、測試與建置，後端測試連到一次性的 PostgreSQL。可將容器建置與本機 smoke test 加入 CI。 [GitHub Actions Node.js 指南](https://docs.github.com/en/actions/tutorials/build-and-test-code/nodejs)
2. 上公開環境前，停用舊資料庫的預設測試帳戶，輪換曾在 Git 歷史出現的有效密碼與 JWT 金鑰，評估結帳冪等與登入限流。以 Secret Manager 管理後端機密，使用資料庫遷移工具管理 schema。Spring Boot 3.5 已結束開源維護，需規劃遷移至受支援版本或取得商業支援。 [Spring Boot 支援說明](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)
3. 將前端和後端映像推至 Artifact Registry，規劃獨立的 Cloud Run 服務以及 Cloud SQL for PostgreSQL。前端容器以 8080 提供服務，建置需包含 `linux/amd64`。若後端設為私有 Cloud Run 服務，前端代理需另傳 Google ID token 給 Cloud Run，並保留應用程式的 Bearer JWT。現有 Nginx 模板只代理應用程式請求。 [Cloud Run 容器契約](https://docs.cloud.google.com/run/docs/container-contract) · [Cloud Run 服務間驗證](https://docs.cloud.google.com/run/docs/authenticating/service-to-service) · [Cloud SQL 連線指南](https://docs.cloud.google.com/sql/docs/postgres/connect-run)
4. 建立預備環境，驗證註冊、登入與結帳後，再規劃 GitHub Actions 的 Workload Identity Federation、版本回滾、錯誤率與延遲告警、資料庫備份。


## 首頁圖像

[shop-hero.jpg](public/shop-hero.jpg) 使用內建 ImageGen 生成。商品卡與詳情使用後端圖片。

> Create a wide 3:2 photorealistic ecommerce hero background for a premium electronics store called DreamShops, no lettering or graphic design. Close-up sculptural graphite and brushed champagne metal over-ear wireless headphones standing on their soft ear cushions on a textured deep blue-purple studio platform. Place headphones mostly in right half of frame, sweeping organic headband dominates right half, with a few soft lavender highlights and a deep charcoal navy gradient. Left 40% is dark matte navy negative space to place white text independently. Dramatic editorial studio light. Camera is at eye level with product, tasteful subtle grain, high-end material realism. No logo, no captions, no humans, no packaging. Keep the product inside frame.
