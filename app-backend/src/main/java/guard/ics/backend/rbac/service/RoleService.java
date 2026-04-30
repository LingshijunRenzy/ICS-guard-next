package guard.ics.backend.rbac.service;

import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.RoleResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.repository.PermissionRepository;
import guard.ics.backend.rbac.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return toResponse(roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id)));
    }

    @Transactional
    public RoleResponse create(String name, String description, Set<Long> permissionIds) {
        if (roleRepository.existsByName(name)) {
            throw new ConflictException("Role already exists: " + name);
        }
        RoleEntity role = RoleEntity.builder().name(name).description(description).build();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            role.setPermissions(new java.util.HashSet<>(permissionRepository.findAllById(permissionIds)));
        }
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, String name, String description, Set<Long> permissionIds) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        if (name != null) role.setName(name);
        if (description != null) role.setDescription(description);
        if (permissionIds != null) {
            role.setPermissions(new java.util.HashSet<>(permissionRepository.findAllById(permissionIds)));
        }
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", id);
        }
        roleRepository.deleteById(id);
    }

    private RoleResponse toResponse(RoleEntity role) {
        Set<String> permNames = role.getPermissions().stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permNames);
    }
}
