package net.javaguides.banking.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.javaguides.banking.exception.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 自訂的身份驗證進入點 (Authentication Entry Point)。
 * * 作用：當使用者嘗試存取受保護（需登入）的 API 資源，
 * 但未提供 JWT Token、Token 已過期，或 Token 無效時，Spring Security 會攔截該請求並觸發此類別。
 * 我們在這裡統一回傳 HTTP 401 (Unauthorized) 狀態碼，以及標準化的 ErrorDetails JSON 錯誤格式。
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    // 建立 Logger，用於記錄系統日誌
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    // 建立 Jackson 的 ObjectMapper，用於將 Java 物件 (ErrorDetails) 序列化轉成 JSON 格式字串
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 當身份驗證失敗時，Spring Security 會自動呼叫這個 commence 方法。
     *
     * @param request       使用者的 HTTP 請求物件 (可以從中獲取請求路徑等資訊)
     * @param response      準備回傳給使用者的 HTTP 回應物件
     * @param authException 導致驗證失敗的例外狀況物件 (包含具體的錯誤原因)
     * @throws IOException 當寫入 HTTP Response 發生錯誤時拋出
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {

        // 1. 記錄警告日誌 (Log)，方便後端開發人員從後台追蹤誰在嘗試未授權存取
        logger.warn("Unauthorized error: {}", authException.getMessage());

        // 2. 設定 HTTP 回應的 Content-Type 為 JSON (application/json)
        // 告訴前端：接下來回傳的內容是 JSON 格式，請用 JSON 解析
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 3. 設定 HTTP 狀態碼為 401 Unauthorized (未經授權)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 4. 建立統一的錯誤訊息結構
        // 使用「自訂 4 參數建構子」，因為這不是表單驗證錯誤，不需要 fieldErrors
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),                        // timestamp: 發生時間
                "Unauthorized",                             // message: 簡短錯誤提示
                "uri=" + request.getServletPath(),          // details: 記錄使用者當時想呼叫的 API 路徑 (例如 /api/accounts)
                "AUTHENTICATION_FAILED"                     // errorCode: 自定義錯誤碼
        );

        // 5. 將 errorDetails 物件轉換成 JSON，並直接寫入到 HTTP Response 的輸出流中回傳給前端
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
}