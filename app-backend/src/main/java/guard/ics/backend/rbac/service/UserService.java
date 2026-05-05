package guard.ics.backend.rbac.service;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ConflictException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rbac.dto.CreateUserRequest;
import guard.ics.backend.rbac.dto.UpdateProfileRequest;
import guard.ics.backend.rbac.dto.UserProfileResponse;
import guard.ics.backend.rbac.dto.UserResponse;
import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.entity.UserEntity;
import guard.ics.backend.rbac.entity.UserProfileEntity;
import guard.ics.backend.rbac.repository.RoleRepository;
import guard.ics.backend.rbac.repository.UserProfileRepository;
import guard.ics.backend.rbac.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       UserProfileRepository profileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageDTO<UserResponse> list(Pageable pageable) {
        Page<UserEntity> page = userRepository.findAll(pageable);
        return PageDTO.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .displayName(request.displayName())
                .enabled(true)
                .build();
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<RoleEntity> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new BadRequestException("One or more role IDs are invalid");
            }
            user.setRoles(roles);
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, CreateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.email() != null) user.setEmail(request.email());
        if (request.displayName() != null) user.setDisplayName(request.displayName());
        if (request.roleIds() != null) {
            Set<RoleEntity> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            user.setRoles(roles);
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfileEntity profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    return UserProfileEntity.builder().user(user).build();
                });
        if (request.language() != null) profile.setLanguage(request.language());
        if (request.timezone() != null) profile.setTimezone(request.timezone());
        if (request.theme() != null) profile.setTheme(request.theme());
        profile.setUpdatedAt(java.time.Instant.now());
        return UserProfileResponse.from(profileRepository.save(profile));
    }

    private UserResponse toResponse(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
        UserProfileResponse profile = profileRepository.findById(user.getId())
                .map(UserProfileResponse::from).orElse(null);
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.isEnabled(), roleNames, permissionNames,
                user.getCreatedAt(), user.getUpdatedAt(), profile);
    }
}
