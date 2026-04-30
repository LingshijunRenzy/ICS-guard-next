package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.RoleResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import guard.ics.backend.rbac.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void shouldListRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(
                new RoleEntity(1L, "ROLE_ADMIN", "Admin", Set.of()),
                new RoleEntity(2L, "ROLE_OPERATOR", "Operator", Set.of())
        ));

        List<RoleResponse> result = roleService.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("ROLE_ADMIN", "ROLE_OPERATOR");
    }

    @Test
    void shouldGetRoleById() {
        PermissionEntity perm = new PermissionEntity(1L, "alerts:read", null);
        RoleEntity role = new RoleEntity(1L, "ROLE_ADMIN", "Admin", Set.of(perm));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        RoleResponse result = roleService.getById(1L);

        assertThat(result.name()).isEqualTo("ROLE_ADMIN");
        assertThat(result.permissions()).contains("alerts:read");
    }

    @Test
    void shouldThrowWhenRoleNotFound() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCreateRole() {
        when(roleRepository.existsByName("ROLE_TEST")).thenReturn(false);
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> {
            RoleEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        RoleResponse result = roleService.create("ROLE_TEST", "Test role", Set.of());

        assertThat(result.name()).isEqualTo("ROLE_TEST");
        assertThat(result.description()).isEqualTo("Test role");
    }

    @Test
    void shouldCreateRoleWithPermissions() {
        when(roleRepository.existsByName("ROLE_WITH_PERMS")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(
                new PermissionEntity(1L, "alerts:read", null),
                new PermissionEntity(2L, "metrics:read", null)
        ));
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> {
            RoleEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        RoleResponse result = roleService.create("ROLE_WITH_PERMS", null, Set.of(1L, 2L));

        assertThat(result.permissions()).containsExactlyInAnyOrder("alerts:read", "metrics:read");
    }

    @Test
    void shouldRejectDuplicateRoleName() {
        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> roleService.create("ROLE_ADMIN", null, Set.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ROLE_ADMIN");
    }

    @Test
    void shouldUpdateRole() {
        RoleEntity existing = RoleEntity.builder().id(1L).name("ROLE_OLD")
                .description("Old").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any())).thenReturn(existing);

        RoleResponse result = roleService.update(1L, "ROLE_NEW", "New desc", null);

        assertThat(result.name()).isEqualTo("ROLE_NEW");
        assertThat(result.description()).isEqualTo("New desc");
    }

    @Test
    void shouldUpdateRolePermissions() {
        RoleEntity existing = RoleEntity.builder().id(1L).name("ROLE_X").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(permissionRepository.findAllById(Set.of(3L))).thenReturn(List.of(
                new PermissionEntity(3L, "audit:read", null)
        ));
        when(roleRepository.save(any())).thenReturn(existing);

        RoleResponse result = roleService.update(1L, null, null, Set.of(3L));

        assertThat(result.permissions()).contains("audit:read");
    }

    @Test
    void shouldDeleteRole() {
        when(roleRepository.existsById(1L)).thenReturn(true);

        roleService.delete(1L);

        verify(roleRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeleteNonexistentRole() {
        when(roleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> roleService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
