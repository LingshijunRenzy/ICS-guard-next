package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.rbac.dto.CreateUserRequest;
import guard.ics.backend.rbac.dto.UserResponse;
import guard.ics.backend.rbac.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User account management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users with pagination")
    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ApiResponse<PageDTO<UserResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.list(pageable));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    public ApiResponse<UserResponse> get(@Parameter(description = "User ID") @PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @Operation(summary = "Create new user")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists")})
    @PostMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.created(userService.create(request));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @Operation(summary = "Delete user")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "Enable user")
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<UserResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(userService.setEnabled(id, true));
    }

    @Operation(summary = "Disable user")
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('users:manage')")
    public ApiResponse<UserResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(userService.setEnabled(id, false));
    }
}
