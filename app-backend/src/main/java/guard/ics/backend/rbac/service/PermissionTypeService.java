package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.PermissionTypeResponse;
import guard.ics.backend.rbac.entity.PermissionTypeEntity;
import guard.ics.backend.rbac.repository.PermissionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionTypeService {

    private final PermissionTypeRepository permissionTypeRepository;

    public PermissionTypeService(PermissionTypeRepository permissionTypeRepository) {
        this.permissionTypeRepository = permissionTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionTypeResponse> list() {
        return permissionTypeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionTypeResponse getById(Long id) {
        return toResponse(permissionTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionType", id)));
    }

    @Transactional
    public PermissionTypeResponse create(String name, String description) {
        if (permissionTypeRepository.existsByName(name)) {
            throw new ConflictException("Permission type already exists: " + name);
        }
        PermissionTypeEntity entity = PermissionTypeEntity.builder()
                .name(name)
                .description(description)
                .build();
        return toResponse(permissionTypeRepository.save(entity));
    }

    @Transactional
    public PermissionTypeResponse update(Long id, String name, String description) {
        PermissionTypeEntity entity = permissionTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionType", id));
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        return toResponse(permissionTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("PermissionType", id);
        }
        permissionTypeRepository.deleteById(id);
    }

    private PermissionTypeResponse toResponse(PermissionTypeEntity entity) {
        return new PermissionTypeResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
