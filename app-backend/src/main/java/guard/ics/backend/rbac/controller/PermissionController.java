package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Permissions", description = "Permission point CRUD")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "List all permissions")
    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.success(permissionService.list());
    }

    @Operation(summary = "Create new permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409")})
    @PostMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<PermissionResponse> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ApiResponse.created(permissionService.create(name, description));
    }

    @Operation(summary = "Update permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<PermissionResponse> update(
            @Parameter(description = "Permission ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ApiResponse.success(permissionService.update(id, name, description));
    }

    @Operation(summary = "Delete permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<Void> delete(@Parameter(description = "Permission ID") @PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }
}
