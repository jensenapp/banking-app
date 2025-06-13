# Spring Boot 銀行應用程式後端 (Banking Application Backend)

![Java](https://img.shields.io/badge/Java-17+-blue?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?style=for-the-badge&logo=spring)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

## 專案概述 (Project Overview)
本專案是一個基於 Spring Boot 的 RESTful API，用於模擬一套基礎但功能完整的銀行核心系統。系統支援客戶帳戶管理、資金存提、帳戶間轉帳以及交易歷史追蹤等功能，旨在提供一個結構清晰、易於擴充且遵循現代軟體開發實踐的後端服務。

---

## 🚀 功能特色 (Features)

### 帳戶管理 (Account Management)
- 建立新銀行帳戶 (`POST /api/accounts`)
- 根據 ID 查詢帳戶詳情 (`GET /api/accounts/{id}`)
- 獲取所有帳戶列表 (`GET /api/accounts`)
- 刪除指定帳戶 (`DELETE /api/accounts/{id}`)

### 資金操作 (Fund Operations)
- 安全地存款至指定帳戶 (`PUT /api/accounts/{id}/deposit`)
- 從指定帳戶提款，並進行餘額檢查 (`PUT /api/accounts/{id}/withdraw`)

### 轉帳服務 (Transfer Service)
- 在兩個帳戶之間進行資金轉移 (`POST /api/accounts/transfer`)

### 交易追蹤 (Transaction Tracking)
- 查詢特定帳戶的交易歷史，並支援分頁 (`GET /api/accounts/{accountId}/transactions`)

---

## ✨ 技術亮點 (Technical Highlights)
- **分層式架構**：嚴格遵循 `Controller` (API 接口層) → `Service` (業務邏輯層) → `Repository` (資料存取層) 的設計模式，確保程式碼高度模組化、低耦合且易於維護。
- **事務完整性**：在存款 (`deposit`)、提款 (`withdraw`) 及轉帳 (`transferFunds`) 等關鍵金融操作中，透過 Spring 的 `@Transactional` 註解確保資料庫操作的原子性，有效防止因部分失敗而導致的資料不一致問題。
- **DTO 模式**：採用 DTO (Data Transfer Object) 模式，將內部的資料庫實體 (`Entity`) 與對外暴露的 API 模型 (`record DTOs`) 進行解耦。這不僅保護了內部資料結構，也提高了 API 的穩定性與安全性。
- **全域例外處理**：利用 `@ControllerAdvice` 建立全域例外處理器，集中管理 `AccountException` 等自定義業務例外，為 API 消費者提供統一、標準化的錯誤回應格式。
- **現代化 Java 實踐**：專案全面採用 Java 17+ 開發，並活用 `record` 類型來建立簡潔且不可變的 DTO，提升了程式碼的可讀性與執行緒安全性。
- **高效能分頁**：整合 Spring Data JPA 的 `Pageable` 介面，以標準化且高效的方式實現對交易紀錄的分頁查詢，能輕鬆應對大量資料情境。

---

## 🛠️ 技術棧 (Technology Stack)

| 類別 | 技術 |
| :--- | :--- |
| **語言 (Language)** | `Java 17+` |
| **核心框架** | `Spring Boot 3.x`, `Spring MVC`, `Spring Data JPA` |
| **資料庫 (Database)** | `H2` (預設), `MySQL` / `PostgreSQL` (可配置) |
| **資料庫互動** | `Hibernate` |
| **建置工具** | `Maven` |
| **輔助工具** | `Lombok` |

---

## ⚙️ 安裝與執行指南 (Installation and Setup)

### 系統需求
- JDK 17 或更高版本
- Maven 3.8 或更高版本
- 一個您偏好的關聯式資料庫，如 MySQL (或使用預設的 H2)

### 安裝步驟

1.  **複製專案**
    ```bash
    git clone [https://github.com/your-username/banking-application.git](https://github.com/your-username/banking-application.git)
    cd banking-application
    ```

2.  **設定資料庫連線** (選用)
    在 `src/main/resources/application.properties` 中更新您的資料庫連線資訊：
    ```properties
    # MySQL 連線範例
    spring.datasource.url=jdbc:mysql://localhost:3306/banking_db?useSSL=false
    spring.datasource.username=root
    spring.datasource.password=your_password_here
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    ```
    > **注意**: 請先在您的 MySQL 中手動建立一個名為 `banking_db` 的資料庫。

3.  **啟動應用程式**
    ```bash
    mvn spring-boot:run
    ```
    API 服務將啟動於 `http://localhost:8080`。

---

## 📝 API 端點文件 (API Endpoints)

### Account (帳戶)

| 方法 | 路徑 | 描述 |
| :--- | :--- | :--- |
| `POST` | `/api/accounts` | 建立新帳戶 (Body: `AccountDto`) |
| `GET` | `/api/accounts/{id}` | 根據 ID 查詢帳戶資料 |
| `GET` | `/api/accounts` | 查詢所有帳戶列表 |
| `DELETE`| `/api/accounts/{id}` | 根據 ID 刪除帳戶 |

### Transaction (交易)

| 方法 | 路徑 | 描述 |
| :--- | :--- | :--- |
| `PUT` | `/api/accounts/{id}/deposit` | 存款 (Body: `{ "amount": double }`) |
| `PUT` | `/api/accounts/{id}/withdraw` | 提款 (Body: `{ "withdraw": double }`) |
| `POST`| `/api/accounts/transfer` | 轉帳 (Body: `TransferFundDTO`) |
| `GET` | `/api/accounts/{accountId}/transactions` | 查詢交易紀錄 (可帶分頁參數 `?pageNo=0&pageSize=5`) |


### 請求與回應範例

#### 1. 建立帳戶
* **請求**: `POST /api/accounts`
    ```json
    {
        "accountHolderName": "Jensen Huang",
        "balance": 50000.0
    }
    ```
* **回應**: `201 Created`
    ```json
    {
        "id": 1,
        "accountHolderName": "Jensen Huang",
        "balance": 50000.0
    }
    ```

#### 2. 轉帳
* **請求**: `POST /api/accounts/transfer`
    ```json
    {
        "fromAccountId": 1,
        "toAccountId": 2,
        "amount": 1000.0
    }
    ```
* **回應**: `200 OK`
    ```json
    "transfer successful"
    ```

#### 3. 錯誤回應
* **情境**: 查詢一個不存在的帳戶 (`GET /api/accounts/999`)
* **回應**: `404 Not Found`
    ```json
    {
        "timestamp": "2025-06-13T21:58:41.123456",
        "message": "Account does not exist",
        "details": "uri=/api/accounts/999",
        "errorCode": "ACCOUNT_NOT_FOUND"
    }
    ```
---

## 🗃️ 資料庫結構 (Database Schema)
本專案包含兩個核心資料表：`accounts` 和 `transactions`，其關係如下：

```
+---------------------+      +------------------------+
|      accounts       |      |      transactions      |
+---------------------+      +------------------------+
| PK id (BIGINT)      |      | PK id (BIGINT)         |
| account_holder_name |      | FK account_id (BIGINT) |---(1..n)---(1..1)
| balance (DOUBLE)    |      | amount (DOUBLE)        |
+---------------------+      | transaction_type (VARCHAR) |
                             | timestamp (TIMESTAMP)  |
                             +------------------------+
```
* **關聯**: 一個 `Account` 可以擁有多筆 `Transaction` 紀錄 (一對多)。

---

## 🔮 未來擴充規劃 (Future Work)
- **使用者認證與授權**：整合 Spring Security 與 JWT (JSON Web Token)，實現 API 端點保護，確保只有授權使用者才能存取對應資源。
- **加強輸入驗證**：在 DTO 中全面導入 `jakarta.validation` 註解 (如 `@NotNull`, `@Min`)，對所有傳入的請求進行更嚴格的參數校驗。
- **API 文件自動化**：整合 `springdoc-openapi` (Swagger 3)，自動生成互動式的 API 文件，提升開發與協作效率。
- **提升測試覆蓋率**：使用 JUnit 5 和 Mockito 為 Service 層和 Controller 層編寫完整的單元測試與整合測試，確保程式碼品質與功能穩定性。
- **容器化部署**：提供 `Dockerfile` 與 `docker-compose.yml`，以利於使用 Docker 快速建構、部署及擴展應用程式。
