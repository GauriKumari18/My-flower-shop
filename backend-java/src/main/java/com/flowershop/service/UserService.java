package com.flowershop.service;

import com.flowershop.exception.DuplicateEmailException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.model.Role;
import com.flowershop.model.User;
import com.flowershop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * USER SERVICE
 * ------------
 * Business logic for user accounts:
 * - signup → validate, hash password, save user
 * - login → verify credentials, return User (JWT issued by AuthController)
 *
 * Logging:
 * INFO – successful registration, successful login
 * WARN – failed login attempt (wrong password or email not found)
 *
 * Exceptions thrown (caught by GlobalExceptionHandler):
 * DuplicateEmailException → 409 Conflict (duplicate registration)
 * ResourceNotFoundException → 404 Not Found (user not found)
 * IllegalArgumentException → 400 Bad Request (blank name, short password)
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // injected from SecurityConfig bean

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────
    // SIGNUP
    // ─────────────────────────────────────────

    /**
     * Registers a new user account.
     *
     * Validation rules:
     * ✗ Name cannot be blank
     * ✗ Email must not already be registered → DuplicateEmailException (409)
     * ✗ Password must be at least 6 characters
     * ✓ Password is BCrypt-hashed before saving
     * ✓ Role defaults to CUSTOMER if not provided
     */
    public User signup(String name, String email, String password, Role role) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }

        // ── Validation: no duplicate email ────────────────────────────────────
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration attempt with already-registered email: {}", email);
            throw new DuplicateEmailException("Email is already registered: " + email);
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = User.builder()
                .name(name)
                .email(email)
                .password(hashedPassword)
                .role(role != null ? role : Role.CUSTOMER)
                .build();

        User saved = userRepository.save(newUser);
        log.info("New user registered: id={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return saved;
    }

    // ─────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────

    /**
     * Verifies login credentials.
     * Returns the User on success — JWT token is generated in AuthController.
     *
     * Logging:
     * WARN on each failed login attempt (wrong email OR wrong password).
     * INFO on successful login.
     */
    public User login(String email, String password) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            log.warn("Login attempt failed — email not found: {}", email);
            // Vague message: don't reveal whether email exists
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOptional.get();

        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        if (!passwordMatches) {
            log.warn("Login attempt failed — wrong password for email: {}", email);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        log.info("User logged in: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());
        return user;
    }

    // ─────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────

    /** Find user by email (used internally and by security layer). */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Resolves the userId from an email address.
     * Used by controllers to extract current user's ID from JWT principal.
     */
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
