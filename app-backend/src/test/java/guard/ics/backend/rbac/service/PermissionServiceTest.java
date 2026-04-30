package guard.ics.backend.rbac.service;

import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
