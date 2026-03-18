package com.api.alba.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final String headerName;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyFilter(
            @Value("${security.api-key.header-name:X-API-KEY}") String headerName,
            @Value("${security.api-key.value}") String apiKey
    ) {
        this.headerName = headerName;
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestApiKey = request.getHeader(headerName);
        if (!StringUtils.hasText(requestApiKey) || !apiKey.equals(requestApiKey)) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Invalid API key.");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
