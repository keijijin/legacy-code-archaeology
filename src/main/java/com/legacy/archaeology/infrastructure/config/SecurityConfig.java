package com.legacy.archaeology.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * セキュリティ設定。
 * MVP: API/静的レビューUIは許可。CSRFはAPI用途のため無効。
 * 次段: Keycloak SSO へ切替（.anyRequest().authenticated()）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/health",
                                                "/actuator/health",
                                                "/actuator/info",
                                                "/review/**",
                                                "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .permitAll() // TODO: Keycloak導入時に authenticated へ変更
                        );
        return http.build();
    }
}
