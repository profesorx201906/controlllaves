package com.institucion.prestamollaves.auth.application;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.auth.application.PasswordChangeTransactionService;
import com.institucion.prestamo_llaves_api.auth.application.PasswordPolicy;
import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUser;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordChangeTransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordPolicy passwordPolicy;

    private PasswordChangeTransactionService service;

    private User administrator;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeTransactionService(
            userRepository,
            passwordEncoder,
            passwordPolicy
        );

        administrator = new User(
            "Administrador",
            "administrador@example.com",
            "{bcrypt}hash-temporal",
            UserRole.ADMINISTRADOR
        );

        ReflectionTestUtils.setField(
            administrator,
            "id",
            1L
        );
    }

    @Test
    void shouldChangePasswordAndRemoveTemporaryFlag() {
        when(userRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(administrator));

        when(
            passwordEncoder.matches(
                "Clave_Temporal_2026!",
                "{bcrypt}hash-temporal"
            )
        ).thenReturn(true);

        when(
            passwordEncoder.matches(
                "Nueva_Clave_Segura_2026!",
                "{bcrypt}hash-temporal"
            )
        ).thenReturn(false);

        when(
            passwordEncoder.encode(
                "Nueva_Clave_Segura_2026!"
            )
        ).thenReturn(
            "{bcrypt}hash-nuevo"
        );

        AuthenticatedUser result =
            service.changePassword(
                1L,
                "Clave_Temporal_2026!",
                "Nueva_Clave_Segura_2026!"
            );

        assertEquals(
            "{bcrypt}hash-nuevo",
            administrator.getPasswordHash()
        );

        assertFalse(
            administrator.isMustChangePassword()
        );

        assertFalse(
            result.mustChangePassword()
        );

        verify(passwordPolicy).validate(
            "Nueva_Clave_Segura_2026!"
        );

        verify(userRepository).flush();
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        when(userRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(administrator));

        when(
            passwordEncoder.matches(
                "Contraseña_Incorrecta!",
                "{bcrypt}hash-temporal"
            )
        ).thenReturn(false);

        InvalidRequestException exception =
            assertThrows(
                InvalidRequestException.class,
                () -> service.changePassword(
                    1L,
                    "Contraseña_Incorrecta!",
                    "Nueva_Clave_Segura_2026!"
                )
            );

        assertEquals(
            "CURRENT_PASSWORD_INVALID",
            exception.getCode()
        );

        verify(passwordEncoder, never())
            .encode(
                "Nueva_Clave_Segura_2026!"
            );

        verify(userRepository, never()).flush();
    }

    @Test
    void shouldRejectReusingCurrentPassword() {
        when(userRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(administrator));

        when(
            passwordEncoder.matches(
                "Clave_Temporal_2026!",
                "{bcrypt}hash-temporal"
            )
        ).thenReturn(true);

        InvalidRequestException exception =
            assertThrows(
                InvalidRequestException.class,
                () -> service.changePassword(
                    1L,
                    "Clave_Temporal_2026!",
                    "Clave_Temporal_2026!"
                )
            );

        assertEquals(
            "NEW_PASSWORD_MUST_DIFFER",
            exception.getCode()
        );

        verify(userRepository, never()).flush();
    }
}