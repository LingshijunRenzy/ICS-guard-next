package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.PermissionTypeResponse;
import guard.ics.backend.rbac.service.PermissionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permission-types")
@Tag(name = "Permission Types", description = "Permission type CRUD")
public class PermissionTypeController {

    private final PermissionTypeService permissionTypeService;

    public PermissionTypeController(PermissionTypeService permissionTypeService) {
        this.permissionTypeService = permissionTypeService;
    }

    @Operation(summary = "List all permission types")
    @GetMapping
    @PreAuthorize("hasAuthority('permissions:read')")
    public ApiResponse<List<PermissionTypeResponse>> list() {
        return ApiResponse.success(permissionTypeService.list());
    }

    @Operation(summary = "Get permission type by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions:read')")
    public ApiResponse<PermissionTypeResponse> get(@Parameter(description = "Type ID") @PathVariable Long id) {
        return ApiResponse.success(permissionTypeService.getById(id));
    }

    @Operation(summary = "Create new permission type")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409")})
    @PostMapping
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<PermissionTypeResponse> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ApiResponse.created(permissionTypeService.create(name, description));
    }

    @Operation(summary = "Update permission type")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<PermissionTypeResponse> update(
            @Parameter(description = "Type ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ApiResponse.success(permissionTypeService.update(id, name, description));
    }

    @Operation(summary = "Delete permission type")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<Void> delete(@Parameter(description = "Type ID") @PathVariable Long id) {
        permissionTypeService.delete(id);
        return ApiResponse.success();
    }
}
