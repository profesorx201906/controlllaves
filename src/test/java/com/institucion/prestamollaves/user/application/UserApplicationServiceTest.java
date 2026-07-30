package com.institucion.prestamollaves.user.application;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.auth.application.PasswordPolicy;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.user.application.UserApplicationService;
import com.institucion.prestamo_llaves_api.user.application.UserCreatedResult;
import com.institucion.prestamo_llaves_api.user.application.UserStatusChangedResult;
import com.institucion.prestamo_llaves_api.user.application.UserSummaryResult;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordPolicy passwordPolicy;

    private UserApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UserApplicationService(
                userRepository,
                passwordEncoder,
                passwordPolicy);
    }

    @Test
    void shouldCreateEnabledUserWithTemporaryPassword() {
        when(
                userRepository.existsByEmailIgnoreCase(
                        "usuario@example.com"))
                .thenReturn(false);

        when(
                passwordEncoder.encode(
                        "Clave_Temporal_2026!"))
                .thenReturn(
                        "{bcrypt}hash-temporal");

        when(
                userRepository.saveAndFlush(
                        any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            user,
                            "id",
                            10L);

                    return user;
                });

        UserCreatedResult result = service.createUser(
                "  Usuario de Prueba  ",
                "  USUARIO@EXAMPLE.COM  ",
                "Clave_Temporal_2026!",
                UserRole.USUARIO);

        assertEquals(10L, result.id());

        assertEquals(
                "Usuario de Prueba",
                result.fullName());

        assertEquals(
                "usuario@example.com",
                result.email());

        assertEquals(
                UserRole.USUARIO,
                result.role());

        assertTrue(result.enabled());
        assertTrue(result.mustChangePassword());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .saveAndFlush(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(
                "{bcrypt}hash-temporal",
                savedUser.getPasswordHash());

        verify(passwordPolicy).validate(
                "Clave_Temporal_2026!");
    }

    @Test
    void shouldCreateAnotherAdministrator() {
        when(
                userRepository.existsByEmailIgnoreCase(
                        "administrador2@example.com"))
                .thenReturn(false);

        when(
                passwordEncoder.encode(
                        "Clave_Temporal_2026!"))
                .thenReturn(
                        "{bcrypt}hash-administrador");

        when(
                userRepository.saveAndFlush(
                        any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserCreatedResult result = service.createUser(
                "Administrador secundario",
                "administrador2@example.com",
                "Clave_Temporal_2026!",
                UserRole.ADMINISTRADOR);

        assertEquals(
                UserRole.ADMINISTRADOR,
                result.role());
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        when(
                userRepository.existsByEmailIgnoreCase(
                        "usuario@example.com"))
                .thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.createUser(
                        "Usuario repetido",
                        "usuario@example.com",
                        "Clave_Temporal_2026!",
                        UserRole.USUARIO));

        assertEquals(
                "EMAIL_ALREADY_REGISTERED",
                exception.getCode());

        verify(passwordEncoder, never())
                .encode(any());

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectNullRole() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.createUser(
                        "Usuario sin rol",
                        "usuario@example.com",
                        "Clave_Temporal_2026!",
                        null));

        assertEquals(
                "ROLE_REQUIRED",
                exception.getCode());

        assertEquals(
                "El rol es obligatorio",
                exception.getMessage());

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldSearchUsersWithNormalizedText() {
        User user = new User(
                "María Rodríguez",
                "maria@example.com",
                "{bcrypt}hash",
                UserRole.USUARIO);

        ReflectionTestUtils.setField(
                user,
                "id",
                10L);

        PageRequest pageable = PageRequest.of(0, 20);

        when(
                userRepository.searchUsers(
                        "maría",
                        UserRole.USUARIO,
                        true,
                        pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(user),
                                pageable,
                                1));

        Page<UserSummaryResult> result = service.searchUsers(
                "  MARÍA  ",
                UserRole.USUARIO,
                true,
                pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(
                "maria@example.com",
                result.getContent().getFirst().email());
    }

    @Test
    void shouldDeactivateRegularUser() {
        User administrator = new User(
                "Administrador",
                "admin@example.com",
                "{bcrypt}hash-admin",
                UserRole.ADMINISTRADOR);

        ReflectionTestUtils.setField(
                administrator,
                "id",
                1L);

        administrator.markPasswordAsChanged();

        User regularUser = new User(
                "Usuario",
                "usuario@example.com",
                "{bcrypt}hash-user",
                UserRole.USUARIO);

        ReflectionTestUtils.setField(
                regularUser,
                "id",
                2L);

        when(
                userRepository.findAllEnabledByRoleForUpdate(
                        UserRole.ADMINISTRADOR))
                .thenReturn(
                        List.of(administrator));

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(
                        java.util.Optional.of(regularUser));

        UserStatusChangedResult result = service.changeUserStatus(
                2L,
                1L,
                false);

        assertFalse(result.enabled());
        assertFalse(regularUser.isEnabled());

        verify(userRepository).flush();
    }

    @Test
    void shouldRejectDeactivatingLastActiveAdministrator() {
        User administrator = new User(
                "Administrador",
                "admin@example.com",
                "{bcrypt}hash-admin",
                UserRole.ADMINISTRADOR);

        ReflectionTestUtils.setField(
                administrator,
                "id",
                1L);

        when(
                userRepository.findAllEnabledByRoleForUpdate(
                        UserRole.ADMINISTRADOR))
                .thenReturn(
                        List.of(administrator));

        when(userRepository.findByIdForUpdate(1L))
                .thenReturn(
                        java.util.Optional.of(administrator));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.changeUserStatus(
                        1L,
                        1L,
                        false));

        assertEquals(
                "LAST_ACTIVE_ADMIN",
                exception.getCode());

        assertTrue(administrator.isEnabled());

        verify(userRepository, never()).flush();
    }

    @Test
    void shouldActivateDisabledUser() {
        User administrator = new User(
                "Administrador",
                "admin@example.com",
                "{bcrypt}hash-admin",
                UserRole.ADMINISTRADOR);

        ReflectionTestUtils.setField(
                administrator,
                "id",
                1L);

        User disabledUser = new User(
                "Usuario",
                "usuario@example.com",
                "{bcrypt}hash-user",
                UserRole.USUARIO);

        ReflectionTestUtils.setField(
                disabledUser,
                "id",
                2L);

        disabledUser.deactivate();

        when(
                userRepository.findAllEnabledByRoleForUpdate(
                        UserRole.ADMINISTRADOR))
                .thenReturn(
                        List.of(administrator));

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(
                        java.util.Optional.of(disabledUser));

        UserStatusChangedResult result = service.changeUserStatus(
                2L,
                1L,
                true);

        assertTrue(result.enabled());
        assertTrue(disabledUser.isEnabled());

        verify(userRepository).flush();
    }

    @Test
    void shouldRejectInactiveAdministratorActor() {
        when(
                userRepository.findAllEnabledByRoleForUpdate(
                        UserRole.ADMINISTRADOR))
                .thenReturn(List.of());

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.changeUserStatus(
                        2L,
                        1L,
                        false));

        assertEquals(
                "La cuenta administrativa ya no está activa",
                exception.getMessage());

        verify(userRepository, never())
                .findByIdForUpdate(2L);

        verify(userRepository, never()).flush();
    }
}