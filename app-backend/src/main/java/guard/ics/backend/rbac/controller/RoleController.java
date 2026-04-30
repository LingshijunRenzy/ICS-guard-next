package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.RoleResponse;
import guard.ics.backend.rbac.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Role and permission assignment")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "List all roles")
    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(roleService.list());
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    public ApiResponse<RoleResponse> get(@Parameter(description = "Role ID") @PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @Operation(summary = "Create new role")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409")})
    @PostMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<RoleResponse> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        Set<Long> permissionIds = body.get("permissionIds") != null
                ? Set.copyOf(((List<Integer>) body.get("permissionIds")).stream().map(Long::valueOf).toList())
                : null;
        return ApiResponse.created(roleService.create(name, description, permissionIds));
    }

    @Operation(summary = "Update role")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        Set<Long> permissionIds = body.get("permissionIds") != null
                ? Set.copyOf(((List<Integer>) body.get("permissionIds")).stream().map(Long::valueOf).toList())
                : null;
        return ApiResponse.success(roleService.update(id, name, description, permissionIds));
    }

    @Operation(summary = "Delete role")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }
}
