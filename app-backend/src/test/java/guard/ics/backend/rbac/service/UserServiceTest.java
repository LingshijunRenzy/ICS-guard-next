package guard.ics.backend.rbac.service;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.CreateUserRequest;
import guard.ics.backend.rbac.dto.UserResponse;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.entity.UserEntity;
import guard.ics.backend.rbac.repository.RoleRepository;
import guard.ics.backend.rbac.repository.UserProfileRepository;
import guard.ics.backend.rbac.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldListUsers() {
        UserEntity user = UserEntity.builder().id(1L).username("admin")
                .passwordHash("hash").enabled(true).roles(Set.of()).build();
        Page<UserEntity> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PageDTO<UserResponse> result = userService.list(PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("admin");
    }

    @Test
    void shouldGetUserById() {
        UserEntity user = UserEntity.builder().id(1L).username("admin")
                .passwordHash("hash").enabled(true).roles(Set.of()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getById(1L);

        assertThat(result.username()).isEqualTo("admin");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCreateUser() {
        CreateUserRequest request = new CreateUserRequest("newuser", "password123",
                "new@test.com", "New User", Set.of(1L));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}encoded");
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(
                RoleEntity.builder().id(1L).name("ROLE_OPERATOR").build()
        ));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            e.setId(10L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UserResponse result = userService.create(request);

        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.roles()).contains("ROLE_OPERATOR");
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldCreateUserWithoutRoles() {
        CreateUserRequest request = new CreateUserRequest("minimal", "password123",
                null, null, null);
        when(userRepository.existsByUsername("minimal")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        UserResponse result = userService.create(request);

        assertThat(result.username()).isEqualTo("minimal");
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void shouldRejectDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest("admin", "password123",
                null, null, null);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest("newuser", "password123",
                "taken@test.com", null, null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void shouldRejectInvalidRoleIds() {
        CreateUserRequest request = new CreateUserRequest("newuser", "password123",
                null, null, Set.of(1L, 999L));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}encoded");
        when(roleRepository.findAllById(Set.of(1L, 999L))).thenReturn(List.of(
                RoleEntity.builder().id(1L).name("ROLE_OPERATOR").build()
        ));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("role IDs");
    }

    @Test
    void shouldUpdateUser() {
        UserEntity existing = UserEntity.builder().id(1L).username("olduser")
                .passwordHash("oldhash").enabled(true).roles(Set.of()).build();
        CreateUserRequest request = new CreateUserRequest(null, "newpass",
                "new@test.com", "New Name", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass")).thenReturn("{bcrypt}newhash");
        when(userRepository.save(any())).thenReturn(existing);

        UserResponse result = userService.update(1L, request);

        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.displayName()).isEqualTo("New Name");
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    void shouldDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeleteNonexistentUser() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldSetEnabled() {
        UserEntity user = UserEntity.builder().id(1L).username("admin")
                .passwordHash("hash").enabled(true).roles(Set.of()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.setEnabled(1L, false);

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldIncludeRoleNamesInResponse() {
        RoleEntity role1 = RoleEntity.builder().id(1L).name("ROLE_ADMIN").build();
        RoleEntity role2 = RoleEntity.builder().id(2L).name("ROLE_OPERATOR").build();
        UserEntity user = UserEntity.builder().id(1L).username("admin")
                .passwordHash("hash").enabled(true)
                .roles(Set.of(role1, role2)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getById(1L);

        assertThat(result.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR");
    }
}
