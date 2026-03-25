package com.api.alba.security;

import com.api.alba.domain.auth.User;
import com.api.alba.mapper.auth.UserMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserMapper userMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                User user = userMapper.findById(userId);
                if (user != null && "ACTIVE".equals(user.getStatus())) {
                    String loginId = jwtTokenProvider.getLoginId(token);
                    UserPrincipal principal = new UserPrincipal(userId, loginId);
                    SecurityContextHolder.getContext().setAuthentication(principal.toAuthentication());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
