package com.flowershop.controller;

import com.flowershop.model.Role;
import com.flowershop.model.User;
import com.flowershop.security.CustomUserDetailsService;
import com.flowershop.security.JwtService;
import com.flowershop.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AUTH CONTROLLER
 * ---------------
 * Public endpoints — no JWT required.
 *
 * POST /api/auth/signup → register a new account
 * POST /api/auth/login → verify credentials, receive a real JWT token
 *
 * Login response includes:
 * {
 * "token": "<JWT>", ← use this in Authorization: Bearer <token> header
 * "id": 1,
 * "name": "Jane Doe",
 * "email": "jane@example.com",
 * "role": "CUSTOMER"
 * }
 *
 * All validation errors bubble up to GlobalExceptionHandler (no try-catch
 * needed).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(UserService userService,
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    // ─── SIGNUP ───────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/signup
     *
     * Request body:
     * {
     * "name": "Jane Doe",
     * "email": "jane@example.com",
     * "password": "secret123",
     * "role": "CUSTOMER" ← optional, defaults to CUSTOMER
     * }
     *
     * Responses:
     * 201 Created → registration successful
     * 409 Conflict → email already registered (DuplicateEmailException)
     * 400 Bad Request → blank name / short password
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> request) {

        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");
        String roleStr = request.get("role");

        // Parse role — default to CUSTOMER if missing or unrecognised
        Role role;
        try {
            role = (roleStr != null) ? Role.valueOf(roleStr.toUpperCase()) : Role.CUSTOMER;
        } catch (IllegalArgumentException e) {
            role = Role.CUSTOMER;
        }

        // Delegates to UserService — throws DuplicateEmailException /
        // IllegalArgumentException
        // Both are caught by GlobalExceptionHandler and returned as JSON
        User registered = userService.signup(name, email, password, role);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        response.put("id", registered.getId());
        response.put("name", registered.getName());
        response.put("email", registered.getEmail());
        response.put("role", registered.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/login
     *
     * Request body:
     * {
     * "email": "jane@example.com",
     * "password": "secret123"
     * }
     *
     * Responses:
     * 200 OK → login successful, JWT returned in "token" field
     * 400 Bad Request → invalid email or password
     *
     * How to use the token:
     * Add header: Authorization: Bearer <token>
     * to every subsequent request that requires authentication.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        // Step 1: Verify credentials — throws IllegalArgumentException if invalid
        User user = userService.login(email, password);

        // Step 2: Load Spring Security UserDetails (needed by JwtService)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Step 3: Generate the real JWT token with role claim
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name()); // embed role in token payload
        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        // Step 4: Return token + user info (never return the password!)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful!");
        response.put("token", jwtToken); // real JWT
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }
}
