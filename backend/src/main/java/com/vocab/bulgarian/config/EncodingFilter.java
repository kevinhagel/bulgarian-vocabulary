package com.vocab.bulgarian.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet filter to force UTF-8 encoding on all HTTP responses.
 * Ensures Bulgarian Cyrillic characters are properly encoded.
 */
@Component
@Order(1)
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("Content-Type", "application/json;charset=UTF-8");
        }

        chain.doFilter(request, response);
    }
}
