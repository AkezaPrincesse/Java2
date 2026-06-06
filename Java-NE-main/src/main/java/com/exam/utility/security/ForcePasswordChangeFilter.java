package com.exam.utility.security;

import com.exam.utility.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Intercepts every authenticated request and blocks access to protected endpoints
 * when the user's forcePasswordChange flag is set to true.
 *
 * Only /auth/change-password is allowed until the user changes their password.
 *
 * Implementation note: the principal is read directly from SecurityContextHolder
 * (populated by JwtAuthenticationFilter earlier in the chain) — no repository
 * call is needed here, which avoids any JPA initialisation-order issues.
 */
@Component
@RequiredArgsConstructor
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // Let the change-password and all /auth/ endpoints through unconditionally
        if (path.equals("/auth/change-password") || path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JwtAuthenticationFilter sets the principal as the User object (User implements UserDetails)
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && user.isForcePasswordChange()) {

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                "success", false,
                "message", "You must change your password before accessing this resource. "
                         + "Please call POST /api/v1/auth/change-password.",
                "data", null
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui") || path.startsWith("/api-docs") || path.startsWith("/actuator");
    }
}
