package com.institucion.prestamo_llaves_api.bootstrap.application;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.bootstrap.infrastructure.config.BootstrapAdminProperties;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void shouldCreateInitialAdministrator() {
        BootstrapAdminProperties properties =
            enabledProperties();

        InitialAdminBootstrap bootstrap =
            new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                properties
            );

        when(
            userRepository.findByEmailIgnoreCase(
                "administrador@example.com"
            )
        ).thenReturn(Optional.empty());

        when(
            userRepository.existsByRole(
                UserRole.ADMINISTRADOR
            )
        ).thenReturn(false);

        when(
            passwordEncoder.encode(
                "Clave_Segura_2026!"
            )
        ).thenReturn(
            "{bcrypt}hash_de_prueba"
        );

        when(userRepository.saveAndFlush(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    user,
                    "id",
                    1L
                );

                return user;
            });

        bootstrap.run(applicationArguments);

        ArgumentCaptor<User> captor =
            ArgumentCaptor.forClass(User.class);

        verify(userRepository)
            .saveAndFlush(captor.capture());

        User createdAdministrator =
            captor.getValue();

        assertEquals(
            "Administrador inicial",
            createdAdministrator.getFullName()
        );

        assertEquals(
            "administrador@example.com",
            createdAdministrator.getEmail()
        );

        assertEquals(
            "{bcrypt}hash_de_prueba",
            createdAdministrator.getPasswordHash()
        );

        assertEquals(
            UserRole.ADMINISTRADOR,
            createdAdministrator.getRole()
        );

        assertEquals(
            true,
            createdAdministrator.isEnabled()
        );

        assertEquals(
            true,
            createdAdministrator
                .isMustChangePassword()
        );
    }

    @Test
    void shouldDoNothingWhenBootstrapIsDisabled() {
        BootstrapAdminProperties properties =
            new BootstrapAdminProperties(
                false,
                "",
                "",
                ""
            );

        InitialAdminBootstrap bootstrap =
            new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                properties
            );

        bootstrap.run(applicationArguments);

        verify(userRepository, never())
            .findByEmailIgnoreCase(any());

        verify(passwordEncoder, never())
            .encode(any());

        verify(userRepository, never())
            .saveAndFlush(any(User.class));
    }

    @Test
    void shouldBeIdempotentWhenAdministratorAlreadyExists() {
        User administrator = new User(
            "Administrador existente",
            "administrador@example.com",
            "{bcrypt}hash_existente",
            UserRole.ADMINISTRADOR
        );

        when(
            userRepository.findByEmailIgnoreCase(
                "administrador@example.com"
            )
        ).thenReturn(
            Optional.of(administrator)
        );

        InitialAdminBootstrap bootstrap =
            new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                enabledProperties()
            );

        bootstrap.run(applicationArguments);

        verify(passwordEncoder, never())
            .encode(any());

        verify(userRepository, never())
            .saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectEmailOwnedByRegularUser() {
        User regularUser = new User(
            "Usuario existente",
            "administrador@example.com",
            "{bcrypt}hash_existente",
            UserRole.USUARIO
        );

        when(
            userRepository.findByEmailIgnoreCase(
                "administrador@example.com"
            )
        ).thenReturn(
            Optional.of(regularUser)
        );

        InitialAdminBootstrap bootstrap =
            new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                enabledProperties()
            );

        IllegalStateException exception =
            assertThrows(
                IllegalStateException.class,
                () -> bootstrap.run(
                    applicationArguments
                )
            );

        assertEquals(
            true,
            exception.getMessage().contains(
                "ya pertenece a un usuario"
            )
        );

        verify(passwordEncoder, never())
            .encode(any());

        verify(userRepository, never())
            .saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectWeakInitialPassword() {
        BootstrapAdminProperties properties =
            new BootstrapAdminProperties(
                true,
                "Administrador inicial",
                "administrador@example.com",
                "clave123"
            );

        InitialAdminBootstrap bootstrap =
            new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                properties
            );

        IllegalStateException exception =
            assertThrows(
                IllegalStateException.class,
                () -> bootstrap.run(
                    applicationArguments
                )
            );

        assertEquals(
            true,
            exception.getMessage().contains(
                "como mínimo"
            )
        );

        verify(userRepository, never())
            .findByEmailIgnoreCase(any());

        verify(passwordEncoder, never())
            .encode(any());
    }

    private static BootstrapAdminProperties
            enabledProperties() {

        return new BootstrapAdminProperties(
            true,
            "Administrador inicial",
            "ADMINISTRADOR@EXAMPLE.COM",
            "Clave_Segura_2026!"
        );
    }
}