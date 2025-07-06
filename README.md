-----

# 銀行後端系統範例 (Banking Backend System)

## 專案概述 (Project Overview)

這是一個基於 **Spring Boot** 開發的 RESTful API 專案，模擬了現代銀行的核心後端功能。專案的設計目標是展現一個**結構清晰、功能完整且兼顧執行緒安全**的後端系統。它不僅包含帳戶管理、存提款、轉帳等基本操作，更在服務層實現了針對**併發交易的鎖定機制**，並採用分層架構、DTO 模式與全域例外處理等業界標準實踐，是展現 Java 後端開發專業能力的最佳範例。

-----

## 🚀 核心功能 (Core Features)

  * **帳戶管理 (Account Management)**：提供完整的 CRUD（建立、讀取、更新、刪除）操作。

      * `POST /api/accounts`：建立新銀行帳戶。
      * `GET /api/accounts/{id}`：依 ID 查詢帳戶詳情。
      * `GET /api/accounts`：分頁查詢所有帳戶列表，並支援排序。
      * `DELETE /api/accounts/{id}`：刪除指定帳戶。

  * **資金操作 (Fund Operations)**：處理核心的金融交易。

      * `PUT /api/accounts/{id}/deposit`：向指定帳戶存款。
      * `PUT /api/accounts/{id}/withdraw`：從指定帳戶提款，並進行餘額檢查。

  * **安全轉帳 (Secure Transfers)**：

      * `POST /api/accounts/transfer`：在兩個帳戶間安全地轉移資金，並處理潛在的死鎖問題。

  * **交易追蹤 (Transaction Tracking)**：

      * `GET /api/accounts/{accountId}/transactions`：分頁查詢特定帳戶的所有交易歷史紀錄。

-----

## ✨ 技術亮點 (Technical Highlights)

  * **分層架構 (Layered Architecture)**：嚴格遵循 `Controller` (API 層) → `Service` (業務邏輯層) → `Repository` (資料存取層) 的設計模式。這種架構確保了**高內聚、低耦合**，使程式碼易於理解、維護與擴展。

  * **事務完整性與併發控制 (Transactional Integrity & Concurrency Control)**：

      * 所有關鍵金融操作（存款、提款、轉帳）皆透過 Spring 的 `@Transactional` 註解管理，確保資料庫操作的**原子性 (Atomicity)**，避免資料不一致。
      * 在 `AccountRepository` 中，針對 `findById` 方法使用了 `PESSIMISTIC_WRITE` 悲觀鎖，確保在讀取帳戶進行更新時，能有效**防止高併發場景下的競態條件 (Race Condition)**。
      * 在轉帳邏輯中，透過**按帳戶 ID 排序後再鎖定**的策略，巧妙地**避免了交易死鎖 (Deadlock)** 的風險。

  * **DTO 與現代 Java 實踐 (DTO Pattern & Modern Java)**：

      * 全面採用 **Java Record** 來定義 DTO (Data Transfer Object)，不僅程式碼極其簡潔，其**不可變 (Immutable)** 的特性更天然地保障了執行緒安全。
      * 透過 DTO 將內部資料庫實體 (`Entity`) 與對外 API 模型進行解耦，保護了內部資料結構，並提升了 API 的穩定性。

  * **全域例外處理 (Global Exception Handling)**：

      * 利用 `@ControllerAdvice` 建立全域例外處理器 (`GlobalExceptionHandler`)，集中捕獲自定義的 `AccountException` 與其他潛在錯誤。此舉讓 API 能向客戶端返回**統一、標準化的錯誤回應格式**，提升了使用者體驗與除錯效率。

  * **高效分頁與參數驗證 (Efficient Pagination & Validation)**：

      * 整合 Spring Data JPA 的 `Pageable` 介面，以標準化且高效的方式實現對帳戶與交易紀錄的**伺服器端分頁 (Server-Side Pagination)**，能輕鬆應對大量資料。
      * 在 Controller 層與 DTO 中使用 `jakarta.validation` 註解（如 `@Valid`, `@Min`, `@NotEmpty`），在進入業務邏輯前對傳入參數進行**前置驗證**，確保了資料的有效性與系統的健壯性。

-----

## 🛠️ 技術棧 (Technology Stack)

| 類別              | 技術                                                    |
| :---------------- | :------------------------------------------------------ |
| **核心框架** | `Spring Boot`, `Spring MVC`, `Spring Data JPA`          |
| **語言** | `Java 17+`                                              |
| **資料庫** | `H2` (開發/測試), 可輕易配置為 `MySQL`, `PostgreSQL` 等 |
| **資料庫互動** | `Hibernate`                                             |
| **建置工具** | `Maven`                                                 |
| **API & DTO 工具** | `Lombok`, `Java Records`                                |

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
    專案預設使用 H2 內嵌式資料庫，無需額外配置。

    ```bash
    mvn spring-boot:run
    ```

    應用程式啟動後，API 服務將運行於 `http://localhost:8080`。

3.  **(選用) 配置 MySQL 資料庫**
    若要切換至 MySQL，請在 `src/main/resources/application.properties` 中更新以下設定，並確保已手動建立名為 `banking_db` 的資料庫。

    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/banking_db
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    ```

-----

## 📝 API 文件與範例 (API Docs & Examples)

### 1\. 建立帳戶

  * **請求**: `POST /api/accounts`
    ```json
    {
        "accountHolderName": "Lisa Su",
        "balance": 100000.00
    }
    ```
  * **回應**: `201 Created`
    ```json
    {
        "id": 1,
        "accountHolderName": "Lisa Su",
        "balance": 100000.00
    }
    ```

### 2\. 轉帳

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

### 3\. 查詢交易紀錄 (分頁)

  * **請求**: `GET /api/accounts/1/transactions?pageNo=0&pageSize=5`
  * **回應**: `200 OK`
    ```json
    {
        "content": [
            {
                "id": 1,
                "accountId": 1,
                "amount": 5000.00,
                "transactionType": "transaction",
                "timestamp": "2025-07-07T12:30:00.123456"
            }
        ],
        "pageNo": 0,
        "pageSize": 5,
        "totalElements": 1,
        "totalPages": 1,
        "last": true
    }
    ```

### 4\. 錯誤回應範例

  * **情境**: 查詢不存在的帳戶 (`GET /api/accounts/999`)
  * **回應**: `404 Not Found`
    ```json
    {
        "timestamp": "2025-07-07T12:35:10.987654",
        "message": "Account does not exist",
        "details": "uri=/api/accounts/999",
        "errorCode": "ACCOUNT_NOT_FOUND"
    }
    ```

-----

## 🔮 未來展望 (Future Work)

  * **整合 Spring Security**：導入 JWT (JSON Web Token) 進行使用者認證與授權，保護 API 端點安全。
  * **強化測試覆蓋**：使用 JUnit 5 與 Mockito 編寫更完整的單元測試與整合測試，確保程式碼品質。
  * **API 文件自動化**：整合 `springdoc-openapi` (Swagger) 自動生成互動式 API 文件。
  * **容器化部署**：提供 `Dockerfile` 與 `docker-compose.yml`，以利於使用 Docker 進行快速部署與擴展。
