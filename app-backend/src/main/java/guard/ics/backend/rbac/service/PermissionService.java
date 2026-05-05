package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getName(), p.getDescription()))
                .toList();
    }

    @Transactional
    public PermissionResponse create(String name, String description) {
        if (permissionRepository.findByName(name).isPresent()) {
            throw new ConflictException("Permission already exists: " + name);
        }
        PermissionEntity entity = PermissionEntity.builder()
                .name(name)
                .description(description)
                .build();
        PermissionEntity saved = permissionRepository.save(entity);
        return new PermissionResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public PermissionResponse update(Long id, String name, String description) {
        PermissionEntity entity = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        PermissionEntity saved = permissionRepository.save(entity);
        return new PermissionResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", id);
        }
        permissionRepository.deleteById(id);
    }
}
