package com.flowershop.repository;

import com.flowershop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY
 * ---------------
 * Connects the app to the "users" table in PostgreSQL.
 *
 * JpaRepository<User, Long> gives us for free:
 * save(user) → INSERT or UPDATE
 * findById(id) → SELECT WHERE id = ?
 * findAll() → SELECT * FROM users
 * deleteById(id) → DELETE WHERE id = ?
 * existsById(id) → boolean check
 *
 * Custom methods below are written as method names —
 * Spring generates the SQL automatically.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used during login to look up the account.
     *
     * Spring generates: SELECT * FROM users WHERE email = ?
     * Returns Optional — safe way to handle "user not found".
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Used during signup to prevent duplicate accounts.
     *
     * Spring generates: SELECT COUNT(*) FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}
