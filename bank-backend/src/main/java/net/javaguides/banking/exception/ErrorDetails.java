package net.javaguides.banking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 封裝 REST API 錯誤回應的標準資料模型。
 * 使用 Java record 特性來建立不可變 (Immutable) 的物件。
 */
// Jackson 標註：在轉換為 JSON 格式時，若屬性的值為 null，則直接忽略該欄位不顯示
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(
        // 錯誤發生的確切時間點
        LocalDateTime timestamp,

        // 簡短易懂的錯誤提示訊息（例如："Validation Failed" 或 "Account not found"）
        String message,

        // 詳細的除錯資訊，通常用來記錄發生錯誤的 API 請求路徑（例如："uri=/api/v1/accounts/transfer"）
        String details,

        // 系統內部定義的錯誤代碼，方便前端根據代碼做特定處理（例如："BANK_ERR_001"）
        String errorCode,

        // 用於記錄表單資料驗證失敗時的細節。Key 為出錯的欄位名稱，Value 為錯誤原因（例如："amount": "金額必須大於零"）
        Map<String, String> fieldErrors
) {
    /**
     * 自訂建構子 (Custom Constructor)
     * * 這是一個捷徑建構子。當發生的是一般業務錯誤（非表單欄位驗證錯誤）時，
     * 開發者只需傳入前 4 個參數即可，不需要手動傳入 null。
     */
    public ErrorDetails(LocalDateTime timestamp,
                        String message,
                        String details,
                        String errorCode) {
        // 透過 this(...) 呼叫 record 預設生成的「5 參數建構子」，並將最後一個 fieldErrors 參數補上 null。
        // 因為上面有加 @JsonInclude 標註，這個 null 值在最終回傳給前端的 JSON 裡會被自動隱藏。
        this(timestamp, message, details, errorCode, null);
    }
}