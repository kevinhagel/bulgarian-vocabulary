package com.vocab.bulgarian.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("#{'${app.allowed-emails}'.split(',')}")
    private List<String> allowedEmails;

    @Value("${app.admin-email}")
    private String adminEmail;

    @PostConstruct
    private void normalizeAllowedEmails() {
        allowedEmails = allowedEmails.stream().map(String::toLowerCase).toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
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
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            String email = user.getAttribute("email");
            if (email == null || !allowedEmails.contains(email.toLowerCase())) {
                throw new OAuth2AuthenticationException("access_denied");
            }
            // Preserve existing OIDC authorities (OidcUserAuthority, SCOPE_*) and add role grants
            var authorities = new LinkedHashSet<org.springframework.security.core.GrantedAuthority>(user.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if (email.equalsIgnoreCase(adminEmail)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo());
        };
    }
}
