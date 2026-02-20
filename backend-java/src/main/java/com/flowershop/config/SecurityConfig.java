package com.flowershop.config;

import com.flowershop.security.CustomUserDetailsService;
import com.flowershop.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIGURATION
 * ----------------------
 * Full JWT-based stateless security.
 *
 * ── PUBLIC (no token needed) ──────────────────────────────────────────
 * POST /api/auth/signup → register a new account
 * POST /api/auth/login → get a JWT token
 * GET /api/flowers/** → browse the flower catalogue
 * /swagger-ui/**, /v3/api-docs/** → Swagger docs
 *
 * ── ADMIN ONLY ────────────────────────────────────────────────────────
 * POST /api/flowers/** → add a flower
 * PUT /api/flowers/** → update a flower
 * DELETE /api/flowers/** → delete a flower
 * GET /api/admin/** → view all orders
 * PATCH /api/admin/** → change order status
 *
 * ── AUTHENTICATED (any valid JWT) ─────────────────────────────────────
 * Everything else (cart, order placement, profile, etc.)
 *
 * Role-based authority names (stored as-is, no ROLE_ prefix):
 * "ADMIN" → use hasAuthority("ADMIN")
 * "CUSTOMER" → use hasAuthority("CUSTOMER")
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            CustomUserDetailsService customUserDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF disabled (stateless REST API does not use sessions) ───────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Custom 401 / 403 JSON responses ───────────────────────────────
                .exceptionHandling(ex -> ex

                        // 401 Unauthorized – missing or invalid token
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\"," +
                                            "\"message\":\"Authentication required. Please login first.\"}");
                        })

                        // 403 Forbidden – valid token but insufficient role
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\"," +
                                            "\"message\":\"You do not have permission to perform this action.\"}");
                        }))

                // ── Authorization rules ────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Public — no login needed
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/flowers", "/api/flowers/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/v3/api-docs")
                        .permitAll()

                        // Admin-only — flower management
                        .requestMatchers(HttpMethod.POST, "/api/flowers/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/flowers/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/flowers/**").hasAuthority("ADMIN")

                        // Admin-only — order management dashboard
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // Everything else requires a valid JWT (any role)
                        .anyRequest().authenticated())

                // ── Stateless session — no HttpSession ever created ────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Wire our DaoAuthenticationProvider (email + BCrypt) ────────────
                .authenticationProvider(authenticationProvider())

                // ── JWT filter runs BEFORE Spring's default username/password filter ─
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // ── Disable form login and HTTP Basic (we use JSON + JWT) ──────────
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Uses CustomUserDetailsService to load users by email,
     * and BCryptPasswordEncoder to verify passwords on login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposed as a bean so UserService can inject it (avoids creating two BCrypt
     * instances).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager bean needed if we ever authenticate programmatically.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
