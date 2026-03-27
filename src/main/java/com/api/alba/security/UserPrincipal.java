package com.api.alba.security;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Getter
public class UserPrincipal {
    private final Long userId;
    private final String loginId;
    private final String userType;

    public UserPrincipal(Long userId, String loginId, String userType) {
        this.userId = userId;
        this.loginId = loginId;
        this.userType = userType;
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(userType);
    }

    public UsernamePasswordAuthenticationToken toAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                this,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
