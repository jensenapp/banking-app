
# 全端銀行系統 (Full-Stack Banking Application)

> **企業級高併發數位銀行系統**
> Spring Boot · React · JWT · Docker · MySQL

[](https://openjdk.org/)
[](https://spring.io/projects/spring-boot)
[](https://react.dev/)
[](https://www.docker.com/)
[](https://www.mysql.com/)

## 專案簡介

本專案為一個完整的**前後端分離銀行系統**，模擬了現代銀行的核心業務流程，並針對金融交易場景實作了**高併發安全防護（樂觀鎖重試機制、悲觀鎖防死鎖策略）**。

後端採用 **Spring Boot 3** 建置 RESTful API 並深度整合 Spring Security + JWT 實現無狀態認證；前端採用 **React 18 + Vite** 打造響應式 SPA；透過 **Docker Compose** 實現一鍵容器化部署，並搭配 **GitHub Actions** 達成 CI/CD 自動化部署。

> **線上 Demo:** [https://bank.jensen-store.online](https://bank.jensen-store.online)
> **API 文件:** [https://bank-api.jensen-store.online/swagger-ui/index.html](https://bank-api.jensen-store.online/swagger-ui/index.html)

### 預設測試帳號

| 角色 | 帳號 | 密碼 | 權限說明 |
|------|------|------|------|
| **系統管理員** | `admin` | `adminPass` | 可檢視全行帳戶、執行臨櫃存款/提款、替客戶開戶 |
| **一般客戶** | `user1` | `password1` | 僅可檢視自己名下帳戶、執行帳戶間轉帳、查看個人交易明細 |

-----

##  核心功能

* ** 安全認證與權限管控 (RBAC)**
    * 基於 JWT 的無狀態身分驗證。
    * 嚴格區分 `ROLE_ADMIN` 與 `ROLE_USER`，並實作方法級 (Method-Level) 授權，確保「帳戶擁有者才能操作自己帳戶」。
* ** 帳戶與資金管理**
    * **臨櫃存提款**：管理員專屬權限，進行資金的存入與提取。
    * **跨帳戶轉帳**：客戶可將資金安全轉移至其他有效帳戶。
    * **交易明細追蹤**：完整紀錄每一筆資金流動，並支援伺服器端分頁 (Server-side Pagination) 查詢。
* ** 系統防呆與健壯性**
    * 系統啟動時自動初始化預設角色與測試資料。
    * `@RestControllerAdvice` 全域例外處理，結合 `@Valid` 進行嚴謹的參數前置校驗，統一回傳標準化 JSON 錯誤訊息。

-----

##  工程與架構亮點 (面試重點)

本專案針對金融系統常見的「高併發」與「資料一致性」問題，提出了具體的架構解法：

### 1\. 避免轉帳交易死鎖 (悲觀鎖 + 排序鎖定策略)

在轉帳操作中，需同時扣除 A 帳戶與增加 B 帳戶的資金。若採用樂觀鎖，在併發互轉的情境下極易全數失敗。此處改用 `SELECT ... FOR UPDATE` 悲觀鎖。為徹底解決潛在的**死鎖 (Deadlock)** 問題（例如 A 轉 B 的同時 B 轉 A），邏輯中**強制對 Account ID 進行大小排序**，確保無論併發順序為何，資料庫永遠以相同的順序獲取行鎖。

```java
// 永遠按帳戶 ID 從小到大上鎖，打破循環等待條件
if (fromAccountId < toAccountId) {
    account1 = accountRepository.findByIdForUpdate(fromAccountId);
    account2 = accountRepository.findByIdForUpdate(toAccountId);
} else {
    account2 = accountRepository.findByIdForUpdate(toAccountId);
    account1 = accountRepository.findByIdForUpdate(fromAccountId);
}
```

### 2\. 解決高併發下的資金更新衝突 (樂觀鎖 + 自動重試機制)

在存提款等高頻操作中，為避免髒讀與遺失更新，引入 `@Version` 實作樂觀鎖。並在 Service 層設計**自動重試機制**：當捕捉到併發衝突時，系統會自動重新拉取最新資料並重試（最多 3 次），兼顧吞吐量與資料一致性。

```java
for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    try {
        // 存款邏輯 ...
        accountRepository.save(account);
        return; // 成功即離開
    } catch (ObjectOptimisticLockingFailureException e) {
        logger.warn("版本衝突，準備重試... (第 {} 次)", attempt + 1);
    }
}
throw new AccountException("高併發衝突，請稍後再試");
```

### 3\. 細粒度的資源權限控制 (Custom Method Security)

結合 Spring Expression Language (SpEL) 與自定義的授權服務，在進入 Controller 前精準攔截越權存取：
`@PreAuthorize("hasRole('ADMIN') or @accountSecurityService.isOwner(authentication,#id)")`

### 4\. DTO Pattern 與 Java Record

API 層全面使用 Java Record 作為 DTO，天然不可變、執行緒安全，內部 Entity 不對外暴露，降低 API 破壞性變更的風險。

### 5\. 前端 Auth State 持久化與攔截器設計

* **Axios Interceptors**：請求自動注入 JWT Header；回應捕捉 401 錯誤，自動清除本機狀態並跳轉登入頁。
* **Context API 持久化**：使用 React Context + `useReducer` 管理全域認證狀態，搭配 `localStorage` 實現頁面重整不掉線。

-----

##  系統架構與技術棧

### 技術棧

| 層級 | 技術 |
|------|------|
| **後端框架** | Java 17, Spring Boot 3.5, Spring MVC, Spring Data JPA |
| **安全性** | Spring Security 6, JWT (jjwt 0.12.6), BCrypt |
| **ORM / DB** | Hibernate, MySQL 8.0, H2 (測試) |
| **前端框架** | React 18 (Vite), React Router v6, Tailwind CSS |
| **測試** | JUnit 5, Mockito, Spring MockMvc |
| **基礎設施** | Docker, Docker Compose, Nginx, GitHub Actions |

### 容器化架構圖

```text
使用者瀏覽器
     │ (HTTPS)
     ▼
[Nginx Proxy Manager] ── SSL 憑證 / 反向代理
     │
     ├─────────────────────────────────┐
     ▼                                 ▼
[React 前端容器 :8086]         [Spring Boot 後端容器 :8089]
 (Nginx 靜態伺服器)                     │
                                       ▼
                               [MySQL 資料庫容器 :3308]
                               (僅綁定 127.0.0.1，防禦外部掃描)

所有容器共享同一個 Docker Bridge Network: bank-network
```

-----

##  DevOps 與 CI/CD 流程

### Docker 輕量化建置流程

前後端均採用分離編譯環境與運行環境的 Docker 建置策略，確保正式環境的映像檔不包含多餘的編譯工具與原始碼，極大化縮減 Image 體積並提升安全性：

* **後端**：首先以 `maven:3.9-eclipse-temurin` 進行專案編譯與打包生成 `.jar` 檔；接著將打包好的應用程式轉移至 `eclipse-temurin:17-jre-alpine` 輕量化環境中執行。
* **前端**：首先在 Node 環境中完成靜態檔案的打包；接著由 Nginx Alpine 提供 Web 服務，並寫入客製化 `nginx.conf` 處理 SPA 的路由 Fallback (`try_files $uri /index.html;`)。

### GitHub Actions 自動化流水線

整合 GitHub Actions 實現持續交付。推送到 `main` 分支時的自動化流程：

1.  透過 SSH 建立與 VPS 伺服器的安全連線。
2.  自動拉取 (Pull) 最新程式碼。
3.  從 GitHub Secrets 動態注入敏感變數（資料庫密碼、JWT 密鑰）生成 `.env`。
4.  執行 `docker compose up -d --build` 進行平滑重啟。
5.  自動清理無用的懸空映像檔 (`docker image prune`)。

-----

## 測試策略與資料庫設計

### 測試涵蓋

* **單元測試 (Unit Tests)**：使用 JUnit 5 + Mockito，採 BDD 風格，深入測試樂觀鎖重試邏輯與死鎖預防機制。
* **Web 層測試 (MVC Tests)**：透過 `@WebMvcTest` + `MockMvc` 專注測試 Controller 參數驗證、HTTP 狀態碼及回應格式。

### 資料庫關聯結構 (MySQL)
<img width="1496" height="639" alt="image" src="https://github.com/user-attachments/assets/bf95caf7-18c5-42ae-b660-9b1468329745" />

```text
本專案包含使用者、角色、帳戶和交易四個核心實體，其關係如下：

  * **關聯**:
      * 一個 `User` 擁有一個 `Role` (多對一)。
      * 一個 `User` 可以擁有多個 `Account` (一對多)。
      * 一個 `Account` 可以擁有多筆 `Transaction` 紀錄 (一對多)。

```

-----

## 本機運行指南

### 前置需求

* Docker Desktop / Docker Engine
* Git

### 快速啟動

```bash
# 1. Clone 專案
git clone https://github.com/your-account/banking-app.git
cd banking-app

# 2. 建立本地環境變數檔 (.env)
cat <<EOF > .env
DB_ROOT_PASSWORD=LocalRootPassword
DB_PASSWORD=LocalAppPassword
JWT_SECRET=ThisIsALocalDevelopmentSecretKeyThatIsLongEnough2026
EOF

# 3. 一鍵建置並啟動所有服務 (MySQL, 後端 API, React 前端)
docker-compose up -d --build

# 4. 檢視容器運行狀態
docker-compose ps
```

啟動後約等待 15-30 秒讓 MySQL 完成初始化。

* **前端介面 (React)**：`http://localhost:8086`
* **後端 API**：`http://localhost:8089/api`
* **Swagger 文件**：`http://localhost:8089/swagger-ui/index.html`

-----

## API 端點總覽

| 模組 | Method | 路徑 | 說明 | 權限 |
|------|--------|------|------|------|
| **Auth** | `POST` | `/api/auth/public/signin` | 使用者登入並核發 JWT | 公開 |
| **Auth** | `POST` | `/api/auth/public/signup` | 註冊新帳號 | 公開 |
| **Account** | `POST` | `/api/accounts` | 開立新帳戶 | ADMIN |
| **Account** | `GET` | `/api/accounts/my-accounts` | 查詢個人名下帳戶 | USER |
| **Account** | `PUT` | `/api/accounts/{id}/deposit` | 臨櫃存款 (樂觀鎖重試) | ADMIN |
| **Account** | `PUT` | `/api/accounts/{id}/withdraw` | 臨櫃提款 (樂觀鎖重試) | ADMIN |
| **Account** | `POST` | `/api/accounts/transfer` | 跨帳戶轉帳 (悲觀鎖排序防死鎖) | 帳戶本人 |
| **Account** | `GET` | `/api/accounts/{id}/transactions` | 查詢帳戶交易明細 (分頁) | ADMIN 或帳戶本人 |

-----

## 🚀 未來規劃

- **Refresh Token 機制**：提升 JWT 的安全性與使用者體驗。
- **非同步處理**：引入 RabbitMQ，將交易紀錄寫入改為非同步，降低主流程延遲。
- **Redis 快取**：快取高頻查詢（如帳戶資訊）以提升 API 效能。

-----

## 👨‍💻 聯絡作者

如有任何問題歡迎透過以下方式聯繫：

- **GitHub：** [jensenapp](https://github.com/jensenapp)
