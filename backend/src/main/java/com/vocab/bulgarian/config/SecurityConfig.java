package com.vocab.bulgarian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("#{'${app.allowed-emails}'.split(',')}")
    private List<String> allowedEmails;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService())
                )
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );
        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            String email = user.getAttribute("email");
            if (email == null || !allowedEmails.contains(email.toLowerCase())) {
                throw new OAuth2AuthenticationException("Email not authorized: " + email);
            }
            // Grant ROLE_ADMIN to the administrator, ROLE_USER to everyone else
            var authorities = email.equalsIgnoreCase(adminEmail)
                ? Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                         new SimpleGrantedAuthority("ROLE_USER"))
                : Set.of(new SimpleGrantedAuthority("ROLE_USER"));
            return new DefaultOAuth2User(authorities, user.getAttributes(), "email");
        };
    }
}
