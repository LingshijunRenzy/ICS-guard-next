package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.LoginRequest;
import guard.ics.backend.rbac.dto.UpdateProfileRequest;
import guard.ics.backend.rbac.dto.UserProfileResponse;
import guard.ics.backend.rbac.dto.UserResponse;
import guard.ics.backend.rbac.entity.UserEntity;
import guard.ics.backend.rbac.repository.UserRepository;
import guard.ics.backend.rbac.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, logout, and current user")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Authenticate and create session")
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        HttpSession session = req.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        UserEntity user = userRepository.findByUsername(request.username()).orElseThrow();
        return ApiResponse.success(userService.getById(user.getId()));
    }

    @Operation(summary = "Invalidate current session")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ApiResponse.success();
    }

    @Operation(summary = "Get current user info")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ApiResponse.error(401, "Not authenticated");
        UserEntity user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(userService.getById(user.getId()));
    }

    @Operation(summary = "Update current user profile preferences")
    @PatchMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ApiResponse.error(401, "Not authenticated");
        UserEntity user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(userService.updateProfile(user.getId(), request));
    }
}
