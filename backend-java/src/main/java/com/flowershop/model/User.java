package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * USER MODEL
 * ----------
 * Maps to the "users" table in PostgreSQL.
 *
 * Key decisions:
 * - email is UNIQUE → no duplicate accounts
 * - password stores a BCrypt HASH, never the plain text
 * - role defaults to CUSTOMER on signup
 * - @Enumerated(STRING) stores "ADMIN" / "CUSTOMER" in DB (readable)
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email") // enforce unique email at DB level
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Primary key — auto-incremented by PostgreSQL */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name (e.g. "Jane Doe") */
    @Column(nullable = false)
    private String name;

    /**
     * Email — used as the login identifier.
     * Unique constraint ensures one account per email.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * HASHED password stored here.
     * BCrypt hash is a 60-character string like:
     * $2a$10$abc123...
     * We NEVER store the original "mypassword123" text.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Role of this user — CUSTOMER or ADMIN.
     * EnumType.STRING stores the word, not a number (0,1).
     * Defaults to CUSTOMER if not set.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;
}
