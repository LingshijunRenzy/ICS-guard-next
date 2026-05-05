package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.PermissionTypeResponse;
import guard.ics.backend.rbac.entity.PermissionTypeEntity;
import guard.ics.backend.rbac.repository.PermissionTypeRepository;
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
class PermissionTypeServiceTest {

    @Mock
    private PermissionTypeRepository permissionTypeRepository;

    @InjectMocks
    private PermissionTypeService permissionTypeService;

    private static PermissionTypeEntity typeEntity(Long id, String name, String desc) {
        return PermissionTypeEntity.builder().id(id).name(name).description(desc).build();
    }

    @Test
    void shouldListTypes() {
        when(permissionTypeRepository.findAll()).thenReturn(List.of(
                typeEntity(1L, "admin", "Admin scope"),
                typeEntity(2L, "operator", "Operator scope")
        ));

        List<PermissionTypeResponse> result = permissionTypeService.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("admin", "operator");
    }

    @Test
    void shouldGetTypeById() {
        when(permissionTypeRepository.findById(1L))
                .thenReturn(Optional.of(typeEntity(1L, "admin", "Admin scope")));

        PermissionTypeResponse result = permissionTypeService.getById(1L);

        assertThat(result.name()).isEqualTo("admin");
        assertThat(result.description()).isEqualTo("Admin scope");
    }

    @Test
    void shouldThrowWhenTypeNotFound() {
        when(permissionTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionTypeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCreateType() {
        when(permissionTypeRepository.existsByName("viewer")).thenReturn(false);
        when(permissionTypeRepository.save(any(PermissionTypeEntity.class)))
                .thenReturn(typeEntity(3L, "viewer", "Viewer scope"));

        PermissionTypeResponse result = permissionTypeService.create("viewer", "Viewer scope");

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.name()).isEqualTo("viewer");
    }

    @Test
    void shouldRejectDuplicateTypeName() {
        when(permissionTypeRepository.existsByName("admin")).thenReturn(true);

        assertThatThrownBy(() -> permissionTypeService.create("admin", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Permission type already exists");
    }

    @Test
    void shouldUpdateType() {
        PermissionTypeEntity existing = typeEntity(1L, "old", "Old desc");
        when(permissionTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(permissionTypeRepository.save(any())).thenReturn(existing);

        PermissionTypeResponse result = permissionTypeService.update(1L, "new", "New desc");

        assertThat(result.name()).isEqualTo("new");
        assertThat(result.description()).isEqualTo("New desc");
    }

    @Test
    void shouldDeleteType() {
        when(permissionTypeRepository.existsById(1L)).thenReturn(true);

        permissionTypeService.delete(1L);

        verify(permissionTypeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeleteNonexistentType() {
        when(permissionTypeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> permissionTypeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
