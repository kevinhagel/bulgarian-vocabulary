package com.vocab.bulgarian.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests from the Telegram bot via a shared secret header.
 * If X-Bot-Token matches BOT_API_KEY, the request is granted ROLE_ADMIN
 * without going through OAuth2.
 */
@Component
public class BotAuthFilter extends OncePerRequestFilter {

    private final String botApiKey;

    public BotAuthFilter(@Value("${BOT_API_KEY}") String botApiKey) {
        this.botApiKey = botApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("X-Bot-Token");
        if (token != null && token.equals(botApiKey)) {
            var auth = new UsernamePasswordAuthenticationToken(
                "telegram-bot", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
