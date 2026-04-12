// file: src/main/java/net/javaguides/banking/security/response/UserInfoResponse.java
package net.javaguides.banking.security.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private boolean accountNonLocked;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
    private boolean enabled;
    private List<String> roles;

    public UserInfoResponse(Long id, String username, String email, boolean accountNonLocked, boolean accountNonExpired,
                            boolean credentialsNonExpired, boolean enabled, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.accountNonLocked = accountNonLocked;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.roles = roles;
    }
}