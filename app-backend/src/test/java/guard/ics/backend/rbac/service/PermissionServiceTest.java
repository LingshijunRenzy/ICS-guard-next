package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void shouldListPermissions() {
        when(permissionRepository.findAll()).thenReturn(List.of(
                new PermissionEntity(1L, "alerts:read", "View alerts"),
                new PermissionEntity(2L, "alerts:manage", "Manage alerts"),
                new PermissionEntity(3L, "users:read", "View users")
        ));

        List<PermissionResponse> result = permissionService.list();

        assertThat(result).hasSize(3);
        assertThat(result).extracting("name")
                .containsExactly("alerts:read", "alerts:manage", "users:read");
    }

    @Test
    void shouldReturnEmptyListWhenNoPermissions() {
        when(permissionRepository.findAll()).thenReturn(List.of());

        List<PermissionResponse> result = permissionService.list();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreatePermission() {
        when(permissionRepository.findByName("new:perm")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(PermissionEntity.class)))
                .thenReturn(new PermissionEntity(14L, "new:perm", "New permission"));

        PermissionResponse result = permissionService.create("new:perm", "New permission");

        assertThat(result.id()).isEqualTo(14L);
        assertThat(result.name()).isEqualTo("new:perm");
        assertThat(result.description()).isEqualTo("New permission");
    }

    @Test
    void shouldRejectDuplicatePermissionName() {
        when(permissionRepository.findByName("duplicate"))
                .thenReturn(Optional.of(new PermissionEntity(1L, "duplicate", "Exists")));

        assertThatThrownBy(() -> permissionService.create("duplicate", "Desc"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Permission already exists");
    }

    @Test
    void shouldUpdatePermission() {
        PermissionEntity existing = new PermissionEntity(1L, "old:perm", "Old");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(PermissionEntity.class)))
                .thenReturn(new PermissionEntity(1L, "new:perm", "Updated"));

        PermissionResponse result = permissionService.update(1L, "new:perm", "Updated");

        assertThat(result.name()).isEqualTo("new:perm");
        assertThat(result.description()).isEqualTo("Updated");
    }

    @Test
    void shouldDeletePermission() {
        when(permissionRepository.existsById(1L)).thenReturn(true);

        permissionService.delete(1L);

        verify(permissionRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeleteNonexistentPermission() {
        when(permissionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission not found");
    }
}
