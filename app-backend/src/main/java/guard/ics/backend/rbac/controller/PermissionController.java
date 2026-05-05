package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @PreAuthorize("hasAuthority('permissions:read')")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.success(permissionService.list());
    }

    @Operation(summary = "Create new permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409")})
    @PostMapping
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<PermissionResponse> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Map<String, String> metadata = extractMetadata(body);
        Long typeId = body.get("typeId") != null ? ((Number) body.get("typeId")).longValue() : null;
        return ApiResponse.created(permissionService.create(name, description, metadata, typeId));
    }

    @Operation(summary = "Update permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<PermissionResponse> update(
            @Parameter(description = "Permission ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Map<String, String> metadata = extractMetadata(body);
        Long typeId = body.get("typeId") != null ? ((Number) body.get("typeId")).longValue() : null;
        return ApiResponse.success(permissionService.update(id, name, description, metadata, typeId));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractMetadata(Map<String, Object> body) {
        Object metaObj = body.get("metadata");
        if (metaObj instanceof Map) {
            return ((Map<String, Object>) metaObj).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        }
        return null;
    }

    @Operation(summary = "Delete permission")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions:manage')")
    public ApiResponse<Void> delete(@Parameter(description = "Permission ID") @PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }
}
