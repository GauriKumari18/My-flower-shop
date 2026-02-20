package com.flowershop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT AUTHENTICATION FILTER
 * --------------------------
 * Intercepts EVERY HTTP request once (OncePerRequestFilter).
 *
 * Flow:
 * 1. Read "Authorization: Bearer <token>" header
 * 2. Extract email from token subject
 * 3. Load UserDetails from DB (CustomUserDetailsService)
 * 4. Validate token (signature + expiry + subject match)
 * 5. Register Authentication in Spring Security context
 *
 * If no token / invalid token → continues without setting authentication.
 * Spring Security will then deny access to any protected endpoint (401/403).
 *
 * This filter runs BEFORE UsernamePasswordAuthenticationFilter (see
 * SecurityConfig).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ── No token or wrong format → pass through (unauthenticated) ─────────
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // strip "Bearer "

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            // Only proceed if email extracted and user not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // ── Token valid → set authentication in Security context ───
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials not needed after auth
                            userDetails.getAuthorities() // [ADMIN] or [CUSTOMER]
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Malformed / expired / tampered token → silently skip
            // Spring Security rejects the request when it hits the protected route
        }

        filterChain.doFilter(request, response);
    }
}
