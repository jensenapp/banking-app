# 銀行後端系統 (Banking Backend System)

## 專案概述 (Project Overview)

這是一個基於 **Spring Boot** 開發的 RESTful API 專案，模擬了現代銀行的核心後端功能。專案的設計目標是展現一個**結構清晰、功能完整、安全可靠**的後端系統。它不僅包含帳戶管理、存提款、轉帳等基本操作，更整合了 **Spring Security 與 JWT** 實現了完整的使用者認證與授權，並在服務層實現了針對**高併發交易的樂觀鎖與悲觀鎖**機制。

專案採用分層架構、DTO 模式與全域例外處理等業界標準實踐。

-----

## 🚀 核心功能 (Core Features)

  * **使用者認證與授權 (User Authentication & Authorization)**

      * `POST /api/auth/public/signup`：註冊新使用者帳號。
      * `POST /api/auth/public/signin`：使用者登入並獲取 JWT (JSON Web Token)。
      * `GET /api/auth/user`：獲取當前登入使用者的詳細資訊。
      * **基於角色的存取控制 (RBAC)**：區分 `ROLE_USER` 和 `ROLE_ADMIN`，保護特定 API 端點。

  * **帳戶管理 (Account Management)**：提供完整的 CRUD（建立、讀取、更新、刪除）操作，並受權限保護。

      * `POST /api/accounts`：為已登入使用者建立新銀行帳戶。
      * `GET /api/accounts/{id}`：依 ID 查詢帳戶詳情 (僅限帳戶擁有者或管理員)。
      * `GET /api/accounts`：分頁查詢所有帳戶列表 (僅限管理員)。
      * `DELETE /api/accounts/{id}`：刪除指定帳戶 (僅限管理員)。

  * **資金操作 (Fund Operations)**：處理核心的金融交易。

      * `PUT /api/accounts/{id}/deposit`：向指定帳戶存款。
      * `PUT /api/accounts/{id}/withdraw`：從指定帳戶提款，並進行餘額檢查。

  * **安全轉帳 (Secure Transfers)**：

      * `POST /api/accounts/transfer`：在兩個帳戶間安全地轉移資金。

  * **交易追蹤 (Transaction Tracking)**：

      * `GET /api/accounts/{id}/transactions`：分頁查詢特定帳戶的所有交易歷史紀錄。

-----

## ✨ 技術亮點 (Technical Highlights)

  * **完整的安全框架 (Comprehensive Security Framework)**：

      * **JWT Token-Based Authentication**：採用無狀態 (Stateless) 的 JWT 進行使用者認證，適用於現代前後端分離架構。
      * **方法級授權 (Method-Level Authorization)**：利用 `@PreAuthorize` 註解進行細粒度的權限控制，不僅能基於角色 (`hasRole('ADMIN')`)，更能結合自訂的 `AccountSecurityService` 實現複雜的業務授權邏輯 (例如：`@accountSecurityService.isOwner(authentication, #id)`)。
      * **安全實踐**: 實現了密碼的 BCrypt 加密儲存、CSRF 防護以及自訂的認證/授權失敗處理器，提供統一的 JSON 錯誤回應。

  * **企業級併發控制 (Enterprise-Grade Concurrency Control)**：

      * **樂觀鎖 (Optimistic Locking)**：在存款 (`deposit`) 和提款 (`withdraw`) 操作中，透過 `@Version` 欄位實現樂觀鎖。當多個請求同時修改同一帳戶時，只有第一個成功，其餘會因版本衝突而失敗並**自動重試**，在高吞吐量場景下兼顧了效能與資料一致性。
      * **悲觀鎖與死鎖預防 (Pessimistic Locking & Deadlock Prevention)**：在轉帳 (`transfer`) 邏輯中，使用資料庫的 `SELECT ... FOR UPDATE` 悲觀鎖。更重要的是，透過**按帳戶 ID 排序後再鎖定**的策略，從根本上**避免了交易死鎖 (Deadlock)** 的風險。

  * **分層架構 (Layered Architecture)**：嚴格遵循 `Controller` → `Service` → `Repository` 的設計模式，確保**高內聚、低耦合**，使程式碼易於理解、維護與擴展。

  * **DTO 與現代 Java 實踐 (DTO Pattern & Modern Java)**：

      * 全面採用 **Java Record** 來定義 DTO (Data Transfer Object)，其**不可變 (Immutable)** 的特性天然地保障了執行緒安全，並使程式碼極其簡潔。
      * 透過 DTO 將內部資料庫實體 (`Entity`) 與對外 API 模型進行解耦，保護了內部資料結構，並提升了 API 的穩定性。

  * **全域例外處理 (Global Exception Handling)**：

      * 利用 `@ControllerAdvice` 建立全域例外處理器，集中捕獲自定義的 `AccountNotFoundException` 與其他潛在錯誤，向客戶端返回**統一、標準化的錯誤回應格式**。

  * **高效分頁與參數驗證 (Efficient Pagination & Validation)**：

      * 整合 Spring Data JPA 的 `Pageable` 介面，實現高效的**伺服器端分頁 (Server-Side Pagination)**。
      * 在 DTO 中使用 `jakarta.validation` 註解，在進入業務邏輯前對傳入參數進行**前置驗證**，確保了資料的有效性與系統的健壯性。

-----

## 🛠️ 技術棧 (Technology Stack)

| 類別 | 技術 |
| :--- | :--- |
| **核心框架** | `Spring Boot`, `Spring MVC`, `Spring Data JPA` |
| **安全性** | `Spring Security`, `JWT (jjwt-api)` |
| **語言** | `Java 17+` |
| **資料庫** | `H2` (開發/測試), 可輕易配置為 `MySQL`, `PostgreSQL` 等 |
| **資料庫互動** | `Hibernate`, 使用 `BigDecimal` 處理金融數據 |
| **建置工具** | `Maven` |
| **API & DTO 工具** | `Lombok`, `Java Records`, `jakarta.validation` |

-----

## ⚙️ 安裝與執行 (Installation & Setup)

### 環境需求

  * JDK 17 或更高版本
  * Maven 3.8 或更高版本

### 執行步驟

1.  **複製專案**

    ```bash
    git clone <your-repository-url>
    cd <project-directory>
    ```

2.  **執行應用程式**
    專案預設使用 H2 內嵌式資料庫，無需額外配置。啟動時會自動建立兩個測試帳號：

      * **管理員**: username=`admin`, password=`adminPass`
      * **一般使用者**: username=`user1`, password=`password1`

    <!-- end list -->

    ```bash
    mvn spring-boot:run
    ```

    應用程式啟動後，API 服務將運行於 `http://localhost:8080`。

-----

## 📝 API 文件與範例 (API Docs & Examples)

**注意**: 訪問受保護的端點時，需在 HTTP 請求的標頭中加入 `Authorization: Bearer <Your-JWT-Token>`。

### 1\. 註冊與登入

  * **註冊**: `POST /api/auth/public/signup`
    ```json
    {
        "username": "testuser",
        "email": "test@example.com",
        "password": "password123",
        "role": ["user"]
    }
    ```
  * **登入**: `POST /api/auth/public/signin`
    ```json
    {
        "username": "user1",
        "password": "password1"
    }
    ```
  * **登入成功回應**: `200 OK`
    ```json
    {
        "username": "user1",
        "roles": ["ROLE_USER"],
        "jwtToken": "eyJhbGciOiJIUz..."
    }
    ```

### 2\. 建立帳戶 (需 Token)

  * **請求**: `POST /api/accounts`
    ```json
    {
        "accountHolderName": "user1",
        "balance": 50000.00
    }
    ```
  * **回應**: `201 Created`
    ```json
    {
        "id": 1,
        "accountHolderName": "user1",
        "balance": 50000.00
    }
    ```

### 3\. 轉帳 (需 Token)

  * **請求**: `POST /api/accounts/transfer`
    ```json
    {
        "fromAccountId": 1,
        "toAccountId": 2,
        "amount": 5000.00
    }
    ```
  * **回應**: `200 OK`
    ```
    transfer successful
    ```

### 4\. 錯誤回應範例

  * **情境**: 未授權存取 (`GET /api/accounts/1` 但未使用 Token)
  * **回應**: `401 Unauthorized`
    ```json
    {
        "status": 401,
        "error": "Unauthorized",
        "message": "Full authentication is required to access this resource",
        "path": "/api/accounts/1"
    }
    ```

-----

## 🗃️ 資料庫結構 (Database Schema)

本專案包含使用者、角色、帳戶和交易四個核心實體，其關係如下：

```
+---------------+      +-------------+      +----------------+      +--------------------+
|     roles     |      |    users    |      |    accounts    |      |    transactions    |
+---------------+      +-------------+      +----------------+      +--------------------+
|PK role_id     |<--(1..1)---(n..1)--|FK role_id  |      |PK id           |<--(1..1)---(n..1)--|FK account_id     |
|   role_name   |      |PK user_id   |--(1..n)--->|FK user_id    |      |   id               |
+---------------+      |   username  |      |   holder_name  |      |   amount           |
                       |   email     |      |   balance      |      |   transaction_type |
                       |   password  |      |   version      |      |   timestamp        |
                       +-------------+      +----------------+      +--------------------+
```

  * **關聯**:
      * 一個 `User` 擁有一個 `Role` (多對一)。
      * 一個 `User` 可以擁有多個 `Account` (一對多)。
      * 一個 `Account` 可以擁有多筆 `Transaction` 紀錄 (一對多)。

-----

## 🔮 未來展望 (Future Work)

  * **強化測試覆蓋**：使用 JUnit 5 與 Mockito 編寫更完整的單元測試與整合測試。
  * **API 文件自動化**：整合 `springdoc-openapi` (Swagger) 自動生成互動式 API 文件。
  * **容器化部署**：提供 `Dockerfile` 與 `docker-compose.yml`，以利於使用 Docker 進行快速部署。
  * **非同步處理**：對於交易紀錄等非核心路徑操作，可引入訊息佇列 (如 RabbitMQ) 進行非同步處理，提升主流程效能。
  * **Refresh Token 機制**：實作 JWT 的 Refresh Token 機制，提供更安全、更長效的登入狀態管理。
  * **兩步驟驗證 (2FA)**：啟用已設計的兩步驟驗證相關欄位，提升帳戶安全性。
