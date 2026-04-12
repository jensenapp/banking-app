// file: src/main/java/net/javaguides/banking/controller/AuthController.java
package net.javaguides.banking.controller;

// 導入相關類別和套件
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import net.javaguides.banking.entity.AppRole;
import net.javaguides.banking.entity.Role;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.repository.RoleRepository;
import net.javaguides.banking.repository.UserRepository;
import net.javaguides.banking.security.jwt.JwtUtils;
import net.javaguides.banking.security.request.LoginRequest;
import net.javaguides.banking.security.request.SignupRequest;
import net.javaguides.banking.security.response.LoginResponse;
import net.javaguides.banking.security.response.MessageResponse;
import net.javaguides.banking.security.response.UserInfoResponse;
import net.javaguides.banking.security.services.UserDetailsImpl;
import net.javaguides.banking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 認證控制器類別
 * 負責處理用戶登入認證相關的 HTTP 請求
 * 實現 JWT 基於 Token 的認證機制
 */
@RestController                 // 標記為 REST 控制器，自動將方法回傳值序列化為 JSON
@RequestMapping("/api/auth")    // 定義控制器的基礎路徑，所有端點都會以 /api/auth 開頭
@Tag(name = "Authentication", description = "使用者認證相關 API") // 群組名稱
public class AuthController {

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserService userService;

    @Autowired
    AuthenticationManager authenticationManager;

    @PostMapping("/public/signin")
    @Operation(
            summary = "使用者登入",
            description = "驗證使用者帳密並回傳 JWT Token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登入成功"),
                    @ApiResponse(responseCode = "401", description = "帳號或密碼錯誤")
            }
    )
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication;

        authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        LoginResponse response = new LoginResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                jwtToken
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/signup")
    @Operation(summary = "註冊新使用者")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // 檢查1：使用者名稱是否已存在
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        // 檢查2：Email 是否已存在
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // 步驟1：建立新使用者帳號，並加密密碼
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getRealName()
        );

        // 步驟2：處理與指派角色
        Set<String> strRoles = signUpRequest.getRole();

        Role role = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.setRole(role);

        // 步驟3：設定使用者帳號的預設屬性
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);

        // 步驟4：儲存使用者到資料庫
        userRepository.save(user);

        // 步驟5：回傳成功訊息
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/user")
    @Operation(summary = "取得當前使用者資訊", description = "需攜帶 JWT Token，回傳完整的 User 詳細資料")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得資訊",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授權 (Token 無效或過期)")
    })
    public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
        // 步驟 1: 透過 username 取得完整的 User Entity 物件
        User user = userService.findByUsername(userDetails.getUsername());

        // 步驟 2: 從 UserDetails 中提取使用者的角色 (Authorities)
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 步驟 3: 準備回傳給前端的 DTO 物件 (已移除過期日與 2FA 屬性)
        UserInfoResponse response = new UserInfoResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isEnabled(),
                roles // 將提取出的角色列表放入
        );

        // 步驟 4: 回傳 200 OK 狀態碼及使用者資訊
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/username")
    @Operation(summary = "取得當前使用者名稱", description = "簡易測試端點，需攜帶 JWT Token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "回傳使用者名稱字串")
    public String currentUserName(@AuthenticationPrincipal UserDetails userDetails){
        return userDetails !=null ? userDetails.getUsername() : "";
    }
}