package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.entity.PermissionMetadataEntity;
import guard.ics.backend.rbac.entity.PermissionTypeEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import guard.ics.backend.rbac.repository.PermissionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionTypeRepository permissionTypeRepository;

    public PermissionService(PermissionRepository permissionRepository, PermissionTypeRepository permissionTypeRepository) {
        this.permissionRepository = permissionRepository;
        this.permissionTypeRepository = permissionTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PermissionResponse create(String name, String description, Map<String, String> metadata, Long typeId) {
        if (permissionRepository.findByName(name).isPresent()) {
            throw new ConflictException("Permission already exists: " + name);
        }
        PermissionEntity entity = PermissionEntity.builder()
                .name(name)
                .description(description)
                .build();
        if (metadata != null) {
            entity.setMetadata(buildMetadata(entity, metadata));
        }
        if (typeId != null) {
            entity.setType(resolveType(typeId));
        }
        return toResponse(permissionRepository.save(entity));
    }

    @Transactional
    public PermissionResponse update(Long id, String name, String description, Map<String, String> metadata, Long typeId) {
        PermissionEntity entity = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (metadata != null) {
            entity.getMetadata().clear();
            entity.getMetadata().addAll(buildMetadata(entity, metadata));
        }
        if (typeId != null) {
            entity.setType(resolveType(typeId));
        }
        return toResponse(permissionRepository.save(entity));
    }

    private PermissionTypeEntity resolveType(Long typeId) {
        return permissionTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionType", typeId));
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", id);
        }
        permissionRepository.deleteById(id);
    }

    private List<PermissionMetadataEntity> buildMetadata(PermissionEntity permission, Map<String, String> entries) {
        List<PermissionMetadataEntity> list = new ArrayList<>();
        for (var entry : entries.entrySet()) {
            PermissionMetadataEntity meta = new PermissionMetadataEntity();
            meta.setId(new PermissionMetadataEntity.PermissionMetadataId(permission.getId(), entry.getKey()));
            meta.setValue(entry.getValue());
            meta.setPermission(permission);
            list.add(meta);
        }
        return list;
    }

    private PermissionResponse toResponse(PermissionEntity entity) {
        Map<String, String> metadata = entity.getMetadata() != null
                ? entity.getMetadata().stream()
                    .collect(Collectors.toMap(m -> m.getId().getKey(), PermissionMetadataEntity::getValue))
                : null;
        Long typeId = entity.getType() != null ? entity.getType().getId() : null;
        String typeName = entity.getType() != null ? entity.getType().getName() : null;
        return new PermissionResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                metadata,
                typeId,
                typeName,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
