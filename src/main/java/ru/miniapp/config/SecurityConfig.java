package ru.miniapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.miniapp.auth.JwtAuthFilter;

import java.util.List;

/**
 * Конфигурация Spring Security для Mini App REST API.
 * Режим: stateless (JWT). CORS разрешает фронт из MINIAPP_ORIGIN.
 * Telegram Webhook и бот работают вне этого контекста — не затрагиваем.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final MiniAppProperties miniAppProperties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF — API stateless, токен в заголовке
                .csrf(AbstractHttpConfigurer::disable)
                // CORS из бина corsConfigurationSource
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Stateless — никаких сессий
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Публичный эндпоинт аутентификации
                        .requestMatchers(HttpMethod.POST, "/api/auth/telegram").permitAll()
                        // Только ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Всё остальное /api/** — требует JWT
                        .requestMatchers("/api/**").authenticated()
                        // Всё остальное (бот, actuator) — разрешаем
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
                            ru.miniapp.dto.ErrorResponse errorResponse = new ru.miniapp.dto.ErrorResponse(
                                    java.time.LocalDateTime.now(),
                                    jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                                    "Unauthorized",
                                    "Требуется авторизация (Bearer JWT)",
                                    request.getRequestURI()
                            );
                            objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
                            ru.miniapp.dto.ErrorResponse errorResponse = new ru.miniapp.dto.ErrorResponse(
                                    java.time.LocalDateTime.now(),
                                    jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN,
                                    "Forbidden",
                                    "Доступ запрещен",
                                    request.getRequestURI()
                            );
                            objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        })
                )
                // JWT-фильтр перед стандартным фильтром username/password
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String origin = miniAppProperties.getOrigin();
        if (origin == null || origin.isBlank()) {
            throw new IllegalStateException(
                    "MINI_APP_ORIGIN не задан! Укажите переменную окружения MINI_APP_ORIGIN " +
                    "(например, MINI_APP_ORIGIN=https://<your-tunnel>.trycloudflare.com)");
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
