package com.flowershop.security;

import com.flowershop.model.User;
import com.flowershop.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CUSTOM USER DETAILS SERVICE
 * ---------------------------
 * Implements Spring Security's UserDetailsService.
 * Called during:
 * 1. Login — AuthenticationProvider verifies password
 * 2. JWT filter — reloads user to validate each request token
 *
 * Loads the user by email (our login identifier) and wraps it
 * in Spring Security's UserDetails with the correct authority.
 *
 * Authority mapping:
 * Role.ADMIN → GrantedAuthority("ADMIN")
 * Role.CUSTOMER → GrantedAuthority("CUSTOMER")
 * Used with hasAuthority("ADMIN") / hasAuthority("CUSTOMER") in SecurityConfig.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // BCrypt hash stored in DB
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .build();
    }
}
