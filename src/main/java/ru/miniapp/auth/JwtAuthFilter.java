package ru.miniapp.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.model.enums.UserRole;

import java.io.IOException;
import java.util.List;

/**
 * JWT-фильтр для Mini App REST API.
 * Читает заголовок Authorization: Bearer <token>, валидирует его
 * и кладёт Authentication в SecurityContext.
 * Пропускает запросы без токена — Spring Security разберётся сам по конфигу.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Нет заголовка или не Bearer — пропускаем, Security сам вернёт 401 если нужно
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Claims claims = jwtService.parseToken(token);

        if (claims == null) {
            log.debug("Невалидный JWT-токен в запросе {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Уже аутентифицирован (например, несколько фильтров)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Long telegramId = jwtService.extractTelegramId(claims);
        UserRole role = jwtService.extractRole(claims);

        // Создаём Authentication с ролью в формате ROLE_USER / ROLE_ADMIN
        var auth = new UsernamePasswordAuthenticationToken(
                telegramId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("JWT аутентификация: telegramId={}, role={}", telegramId, role);

        filterChain.doFilter(request, response);
    }
}
