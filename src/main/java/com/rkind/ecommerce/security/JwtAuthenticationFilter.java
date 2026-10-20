
package com.rkind.ecommerce.security;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight requests do not require a JWT.
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Requests without a token continue. SecurityConfig decides
        // whether the requested endpoint is public or protected.
        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // If Authorization is supplied, require the Bearer scheme.
        if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            writeUnauthorized(response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                writeUnauthorized(response);
                return;
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null
                    || !Boolean.TRUE.equals(user.getEnabled())
                    || user.getRole() == null
                    || !jwtService.isTokenValid(token, user.getEmail())) {

                SecurityContextHolder.clearContext();
                writeUnauthorized(response);
                return;
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();

            // Do not log the JWT, email, or other personal information.
            log.debug("JWT validation failed for request {} {}",
                    request.getMethod(),
                    request.getRequestURI());

            writeUnauthorized(response);
        }
    }

    private void writeUnauthorized(HttpServletResponse response)
            throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        response.getWriter().write(
                "{\"message\":\"Your session is invalid or expired. Please sign in again.\"}"
        );
    }
}
