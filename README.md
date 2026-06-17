# Banking App 銀行帳戶管理系統

一個以 **Spring Boot + React** 開發的全端銀行帳戶管理系統，支援使用者註冊登入、JWT 身分驗證、角色權限控管、帳戶查詢、存款、提款、轉帳、交易紀錄查詢、管理員後台與 Docker 化部署。

此專案主要用於展示後端工程能力，包含 RESTful API 設計、Spring Security 權限控管、JPA 關聯建模、交易一致性處理、分頁查詢、例外處理、Swagger API 文件，以及 GitHub Actions 自動部署到 VPS。

---
## 線上 Demo

- 線上 Demo：https://bank.jensen-store.online/
- API 文件：https://bank-api.jensen-store.online/swagger-ui/index.html

> Demo 環境僅供面試展示與功能測試使用，資料可能會因測試操作而變動。

---

## Demo 測試帳號

> 以下帳號僅供 Demo / 面試展示使用，請勿用於正式環境。正式環境應改由環境變數、初始化腳本或後台流程建立管理員帳號，並更換預設密碼。

| 角色 | 帳號 | 密碼 | 可操作功能 |
|---|---|---|---|
| 管理員 | `admin` | `adminPass` | 可檢視全行帳戶、替客戶開戶，並執行臨櫃存款與提款 |
| 一般客戶 | `user1` | `password1` | 可檢視名下帳戶、執行跨帳戶轉帳，並查詢個人交易明細 |
## 專案亮點

- 使用 **Spring Boot 3 + Java 17** 建立 RESTful API
- 使用 **Spring Security + JWT** 實作無狀態登入驗證
- 支援 **ROLE_USER / ROLE_ADMIN** 角色權限控管
- 一般使用者只能操作自己的帳戶，管理員可管理所有帳戶
- 金額欄位使用 **BigDecimal**，避免浮點數精度問題
- 存款 / 提款使用 `@Version` 樂觀鎖與重試機制處理併發
- 轉帳流程使用 `@Transactional`，確保扣款與入帳具備一致性
- 轉帳時依照帳戶 ID 順序加鎖，降低死鎖風險
- 使用 Spring Data JPA + MySQL 進行資料持久化
- 使用 DTO / Mapper 分離 Entity 與 API 回傳資料
- 使用 Bean Validation 驗證請求資料
- 使用 Global Exception Handler 統一處理 API 錯誤回應
- 整合 Swagger / OpenAPI 文件
- 前端使用 React + Vite + Tailwind CSS
- 使用 Axios Interceptor 自動附加 JWT Token
- 使用 Docker Compose 一鍵啟動前端、後端、MySQL
- 使用 GitHub Actions 透過 SSH 自動部署到 VPS
- 使用 k6 對登入、帳戶查詢、交易紀錄、存提款與轉帳 API 進行本機壓力測試
- 透過併發壓測驗證樂觀鎖、悲觀鎖、retry 與 idempotency key 設計
- 使用 SQL 驗證壓測後帳戶餘額、交易紀錄與冪等性資料一致性

---

## 技術棧

### Backend

| 技術 | 用途 |
|---|---|
| Java 17 | 後端主要開發語言 |
| Spring Boot 3.5 | 後端框架 |
| Spring Web | RESTful API |
| Spring Security | 身分驗證與授權 |
| JWT / JJWT | Token-based Authentication |
| Spring Data JPA / Hibernate | ORM 與資料庫操作 |
| MySQL 8 | 正式環境資料庫 |
| H2 Database | 測試 / 開發輔助資料庫 |
| Bean Validation | Request DTO 驗證 |
| Lombok | 減少樣板程式碼 |
| Springdoc OpenAPI | Swagger API 文件 |
| JUnit / MockMvc | Controller 測試 |
| Docker | 容器化部署 |

### Frontend

| 技術 | 用途 |
|---|---|
| React | 前端 UI |
| Vite | 前端建置工具 |
| React Router | 前端路由 |
| Tailwind CSS | UI 樣式 |
| Axios | API 請求 |
| React Context | 全域登入狀態管理 |
| React Toastify | 操作提示訊息 |
| Nginx | 前端靜態檔案部署與 SPA fallback |

### DevOps / Deployment

| 技術 | 用途 |
|---|---|
| Docker Compose | 一鍵啟動多容器服務 |
| GitHub Actions | CI/CD 自動部署 |
| VPS | 遠端部署環境 |
| Nginx | 前端容器服務 |
| GitHub Secrets | 管理部署密碼與環境變數 |

---

## 系統架構

```mermaid
flowchart LR
    User[使用者 / 管理員] --> Frontend[React Frontend]
    Frontend -->|Axios + JWT| Backend[Spring Boot Backend]
    Backend --> Security[Spring Security + JWT Filter]
    Security --> Controller[REST Controllers]
    Controller --> Service[Service Layer]
    Service --> Repository[Spring Data JPA Repository]
    Repository --> DB[(MySQL Database)]

    Github[GitHub Push to main] --> Actions[GitHub Actions]
    Actions --> VPS[VPS Server]
    VPS --> Docker[Docker Compose]
    Docker --> Frontend
    Docker --> Backend
    Docker --> DB
```

---

## Entity 關係設計

```mermaid
erDiagram
    USER ||--o{ ACCOUNT : owns
    ROLE ||--o{ USER : assigned_to
    ACCOUNT ||--o{ TRANSACTION : records

    USER {
        Long userId
        String username
        String email
        String password
        String realName
        boolean enabled
        LocalDateTime createdDate
        LocalDateTime updatedDate
    }

    ROLE {
        Integer roleId
        AppRole roleName
    }

    ACCOUNT {
        Long id
        String accountHolderName
        BigDecimal balance
        Long version
    }

    TRANSACTION {
        Long id
        Long accountId
        BigDecimal amount
        TransactionType transactionType
        LocalDateTime timestamp
    }
```

---

## 核心功能

### 使用者功能

- 註冊帳號
- 登入並取得 JWT Token
- 查看個人資料
- 查看自己名下的所有銀行帳戶
- 查詢單一帳戶資訊
- 進行帳戶轉帳
- 查看交易紀錄
- 分頁瀏覽交易明細

### 管理員功能

- 查看所有帳戶
- 建立使用者帳戶
- 查詢任意帳戶
- 對指定帳戶存款
- 對指定帳戶提款
- 刪除帳戶
- 查看指定帳戶交易紀錄
- 使用分頁與排序管理帳戶列表

---

## 權限設計

| 角色 | 權限 |
|---|---|
| ROLE_USER | 查看自己的帳戶、查詢自己的交易紀錄、從自己的帳戶轉帳 |
| ROLE_ADMIN | 建立帳戶、查詢所有帳戶、存款、提款、刪除帳戶、查看所有交易紀錄 |

### 權限控管重點

後端使用 `@PreAuthorize` 進行方法層級授權，例如：

```java
@PreAuthorize("hasRole('ADMIN') or @accountSecurityService.isOwner(authentication,#id)")
```

此設計可確保：

- 管理員可以查詢任意帳戶
- 一般使用者只能查詢自己的帳戶
- 轉帳時會檢查轉出帳戶是否屬於目前登入者
- 避免使用者透過修改 URL ID 存取他人帳戶

---

## API 端點總覽

### Authentication API

Base URL：

```text
/api/auth
```

| Method | Endpoint | 權限 | 說明 |
|---|---|---|---|
| POST | `/public/signup` | Public | 使用者註冊 |
| POST | `/public/signin` | Public | 使用者登入並取得 JWT |
| GET | `/user` | Authenticated | 取得目前登入使用者資訊 |
| GET | `/username` | Authenticated | 取得目前登入使用者名稱 |

---

### Account API

Base URL：

```text
/api/accounts
```

| Method | Endpoint | 權限 | 說明 |
|---|---|---|---|
| GET | `/my-accounts` | USER | 取得目前使用者名下帳戶 |
| POST | `/` | ADMIN | 建立新帳戶 |
| GET | `/{id}` | ADMIN 或帳戶本人 | 查詢單一帳戶 |
| PUT | `/{id}/deposit` | ADMIN | 存款 |
| PUT | `/{id}/withdraw` | ADMIN | 提款 |
| GET | `/` | ADMIN | 分頁查詢所有帳戶 |
| DELETE | `/{id}` | ADMIN | 刪除帳戶 |
| POST | `/transfer` | 轉出帳戶本人 | 轉帳 |
| GET | `/{id}/transactions` | ADMIN 或帳戶本人 | 分頁查詢交易紀錄 |

---

## 交易與併發處理設計

### 金額處理

本專案使用 `BigDecimal` 儲存與計算金額，避免 `double` / `float` 在金融情境中可能產生的精度問題。

---

### 存款 / 提款

存款與提款流程會：

1. 查詢帳戶是否存在
2. 驗證提款時餘額是否足夠
3. 更新帳戶餘額
4. 新增交易紀錄
5. 回傳更新後帳戶資訊

帳戶 Entity 使用 `@Version` 欄位支援樂觀鎖。當高併發更新同一帳戶導致版本衝突時，Service 會捕捉 `ObjectOptimisticLockingFailureException` 並進行重試。

---

### 轉帳

轉帳流程使用 `@Transactional` 包住整個操作，確保以下操作要嘛全部成功，要嘛全部回滾：

1. 驗證轉出帳戶與轉入帳戶不可相同
2. 查詢並鎖定兩個帳戶
3. 驗證轉出帳戶餘額是否足夠
4. 從轉出帳戶扣款
5. 對轉入帳戶加款
6. 儲存兩筆帳戶異動
7. 新增 `TRANSFER_OUT` 與 `TRANSFER_IN` 兩筆交易紀錄

轉帳時依照帳戶 ID 順序加鎖，降低多筆交易同時轉帳時產生死鎖的機率。

---

## 錯誤處理

後端使用 `@ControllerAdvice` 建立全域例外處理，統一處理 API 錯誤回應。

常見錯誤包含：

| 錯誤類型 | HTTP Status | 說明 |
|---|---:|---|
| `AccountNotFoundException` | 404 | 帳戶不存在 |
| `InsufficientAmountException` | 400 | 餘額不足 |
| `AccountException` | 400 | 不合法的帳戶操作 |
| `AuthenticationException` | 401 | 身分驗證失敗 |
| `MethodArgumentNotValidException` | 400 | Request Body 驗證失敗 |
| `Exception` | 500 | 未預期錯誤 |

錯誤回應格式範例：

```json
{
  "timestamp": "2026-01-01T12:00:00",
  "message": "Insufficient amount",
  "details": "uri=/api/accounts/1/withdraw",
  "errorCode": "INSUFFICIENT_AMOUNT"
}
```

---

## 前端頁面

| 路徑 | 權限 | 說明 |
|---|---|---|
| `/` | Public | 首頁 |
| `/login` | Public | 登入頁 |
| `/signup` | Public | 註冊頁 |
| `/dashboard` | Authenticated | 使用者帳戶儀表板 |
| `/accounts/:id/transactions` | Authenticated | 交易紀錄頁 |
| `/accounts/:id/transfer` | Authenticated | 轉帳頁 |
| `/admin` | ADMIN | 管理員後台 |
| `/admin/accounts/create` | ADMIN | 建立帳戶 |
| `/admin/accounts/:id/deposit` | ADMIN | 存款 |
| `/admin/accounts/:id/withdraw` | ADMIN | 提款 |

---

## 專案結構

```text
banking-app
├── bank-backend
│   ├── src/main/java/net/javaguides/banking
│   │   ├── config
│   │   │   └── OpenApiConfig.java
│   │   ├── controller
│   │   │   ├── AuthController.java
│   │   │   ├── AccountController.java
│   │   │   └── CsrfController.java
│   │   ├── dto
│   │   │   ├── AccountDto.java
│   │   │   ├── AmountRequestDto.java
│   │   │   ├── CreateAccountRequest.java
│   │   │   ├── PageResponseDTO.java
│   │   │   ├── TransactionDTO.java
│   │   │   ├── TransferFundDTO.java
│   │   │   └── UserDTO.java
│   │   ├── entity
│   │   │   ├── Account.java
│   │   │   ├── User.java
│   │   │   ├── Role.java
│   │   │   ├── Transaction.java
│   │   │   └── AppRole.java
│   │   ├── exception
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── mapper
│   │   │   └── AccountMapper.java
│   │   ├── repository
│   │   ├── security
│   │   │   ├── jwt
│   │   │   ├── request
│   │   │   ├── response
│   │   │   └── services
│   │   └── service
│   ├── Dockerfile
│   └── pom.xml
│
├── bank-frontend
│   ├── src
│   │   ├── api
│   │   │   └── apiClient.js
│   │   ├── components
│   │   ├── services
│   │   ├── store
│   │   │   └── auth-context.jsx
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── tailwind.config.js
│   └── vite.config.js
│
├── docker-compose.yml
└── .github/workflows/deploy.yml
```

---

## 本機啟動方式

### 1. Clone 專案

```bash
git clone https://github.com/jensenapp/banking-app.git
cd banking-app
```

---

### 2. 建立 `.env`

請在專案根目錄建立 `.env`：

```env
DB_ROOT_PASSWORD=your_mysql_root_password
DB_PASSWORD=your_mysql_user_password
JWT_SECRET=your_base64_jwt_secret
```

> 注意：正式專案請勿將 `.env` 提交到 GitHub，應改用 `.env.example` 提供欄位範例。

---

### 3. 使用 Docker Compose 啟動

```bash
docker compose up -d --build
```

啟動後服務位置：

| Service | URL |
|---|---|
| Frontend | `http://localhost:8086` |
| Backend API | `http://localhost:8089/api` |
| Swagger UI | `http://localhost:8089/swagger-ui/index.html` |
| MySQL | `127.0.0.1:3308` |

---

### 4. 關閉服務

```bash
docker compose down
```

若需要連資料庫 volume 一起刪除：

```bash
docker compose down -v
```

---

## 後端單獨啟動

```bash
cd bank-backend
mvn spring-boot:run
```

---

## 前端單獨啟動

```bash
cd bank-frontend
npm install
npm run dev
```

開發環境 API Base URL：

```env
VITE_API_BASE_URL=http://localhost:8089/api
```

---

## Swagger API 文件

啟動後可透過以下網址查看 API 文件：

```text
http://localhost:8089/swagger-ui/index.html
```

Swagger 已整合 JWT Bearer Token 設定，可在登入取得 Token 後，透過 Authorize 按鈕測試需要驗證的 API。

---

## 測試

後端包含 Controller 測試，使用 MockMvc 驗證 API 行為，例如：

- 建立帳戶成功回傳 201
- 輸入資料不合法回傳 400
- 查詢不存在帳戶回傳 404
- 分頁查詢帳戶
- 查詢交易紀錄
- 提款成功
- 餘額不足時回傳錯誤

執行測試：

```bash
cd bank-backend
mvn test
```

---

## 部署流程

此專案已設定 GitHub Actions，當程式碼推送到 `main` 分支時，會自動透過 SSH 連線到 VPS 並執行部署流程。

部署流程包含：

1. 進入 VPS 上的專案部署目錄
2. 若尚未 clone 專案，則從 GitHub clone
3. 若已存在專案，則 pull 最新程式碼
4. 從 GitHub Secrets 產生 `.env`
5. 執行 Docker Compose 重建與啟動服務
6. 清理 dangling Docker images，避免 VPS 空間不足

GitHub Secrets 需設定：

| Secret Name | 說明 |
|---|---|
| `VPS_HOST` | VPS IP 或網域 |
| `VPS_USERNAME` | VPS 使用者名稱 |
| `VPS_SSH_KEY` | SSH Private Key |
| `DB_ROOT_PASSWORD` | MySQL root 密碼 |
| `DB_PASSWORD` | MySQL 使用者密碼 |
| `JWT_SECRET` | JWT 簽章密鑰 |

---


## Load Testing / 壓力測試

本專案除了單元測試與 Controller 測試外，也使用 k6 對核心 API 進行本機壓力測試，用來驗證系統在多使用者併發情境下的穩定性、交易一致性與查詢效能。

壓測重點包含：

- JWT 登入是否穩定
- 帳戶列表查詢效能
- 交易紀錄分頁查詢效能
- 存款 / 提款在高併發下的樂觀鎖處理
- 轉帳在高併發下的悲觀鎖、死鎖風險與冪等性處理
- 是否出現 500 Internal Server Error
- 是否發生重複轉帳、餘額錯誤或交易紀錄不一致

---

### 壓測環境

目前壓測環境為本機開發環境：

| 項目 | 設定 |
|---|---|
| Backend | IntelliJ 啟動 Spring Boot |
| Database | 本機 MySQL |
| Load Testing Tool | k6 |
| API Base URL | `http://localhost:8080/api` |

> 注意：壓測會實際修改資料庫中的帳戶餘額與交易紀錄，請勿直接對正式環境或 Demo 環境執行高併發存款、提款與轉帳測試。

---

### 本機測試帳號與資料

| 角色 | username | password | 用途 |
|---|---|---|---|
| 一般使用者 | `user1` | `password1` | 登入、查詢自己的帳戶、查詢交易紀錄、轉帳 |
| 管理員 | `admin` | `adminPass` | 存款、提款、查詢全部帳戶 |
| 一般使用者 | `user2` | - | 作為轉帳收款人 |

目前本機可用帳戶資料範例：

| Account ID | 所屬使用者 | 用途 |
|---:|---|---|
| `2` | `user1` | 轉出帳戶、查詢交易紀錄、存提款測試 |
| `3` | `user2` | 轉入帳戶 |

> 帳戶餘額會因每次壓測而變動，實際測試前請先確認資料庫中的帳戶 ID 與餘額。

---

### k6 測試檔案

k6 測試腳本放置於：

```text
k6-tests/
├── common.js
├── 01-smoke.js
├── 02-login-load.js
├── 03-read-load.js
├── 04-money-concurrency.js
└── 05-transfer-idempotency.js