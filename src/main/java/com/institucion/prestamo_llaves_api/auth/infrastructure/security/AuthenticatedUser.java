package com.institucion.prestamo_llaves_api.auth.infrastructure.security;


import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Representación del usuario utilizada por Spring Security.
 */
public record AuthenticatedUser(
    Long id,
    String fullName,
    String email,
    String passwordHash,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword
) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole(),
            user.isEnabled(),
            user.isMustChangePassword()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority>
            getAuthorities() {

        /*
         * Un usuario con contraseña temporal no recibe todavía
         * su rol funcional.
         */
        if (mustChangePassword) {
            return List.of(
                new SimpleGrantedAuthority(
                    "ROLE_PASSWORD_CHANGE_REQUIRED"
                )
            );
        }

        return List.of(
            new SimpleGrantedAuthority(
                "ROLE_" + role.name()
            )
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}