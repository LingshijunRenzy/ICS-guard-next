package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.rbac.dto.LoginRequest;
import guard.ics.backend.rbac.dto.UserResponse;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.entity.UserEntity;
import guard.ics.backend.rbac.repository.UserRepository;
import guard.ics.backend.rbac.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticationManager, userService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        UserEntity user = UserEntity.builder().id(1L).username("admin")
                .passwordHash("hash").email("admin@test.com").displayName("Admin")
                .enabled(true).roles(Set.of(RoleEntity.builder().id(1L).name("ROLE_ADMIN").build()))
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userService.getById(1L)).thenReturn(new UserResponse(1L, "admin", "admin@test.com",
                "Admin", true, Set.of("ROLE_ADMIN"), Set.of("alerts:read"), Instant.now(), Instant.now(), null));

        MockHttpServletRequest req = new MockHttpServletRequest();

        ApiResponse<UserResponse> result = authController.login(request, req);

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().username()).isEqualTo("admin");
        assertThat(result.data().roles()).contains("ROLE_ADMIN");
        assertThat(req.getSession(false)).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(auth);
    }

    @Test
    void shouldFailLoginWithBadCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        MockHttpServletRequest req = new MockHttpServletRequest();

        assertThatThrownBy(() -> authController.login(request, req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldLogoutSuccessfully() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        HttpSession session = req.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        ApiResponse<Void> result = authController.logout(req);

        assertThat(result.code()).isEqualTo(200);
        assertThat(req.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldLogoutWhenNoSession() {
        MockHttpServletRequest req = new MockHttpServletRequest();

        ApiResponse<Void> result = authController.logout(req);

        assertThat(result.code()).isEqualTo(200);
    }

    @Test
    void shouldReturnCurrentUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext ctx = SecurityContextHolder.getContext();
        ctx.setAuthentication(auth);

        UserResponse expectedResponse = new UserResponse(1L, "admin", "admin@test.com",
                "Admin", true, Set.of("ROLE_ADMIN"), Set.of("alerts:read"), Instant.now(), Instant.now(), null);
        when(userService.getById(1L)).thenReturn(expectedResponse);
        UserEntity user = UserEntity.builder().id(1L).username("admin").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        ApiResponse<UserResponse> result = authController.me();

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().username()).isEqualTo("admin");
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        ApiResponse<UserResponse> result = authController.me();

        assertThat(result.code()).isEqualTo(401);
        assertThat(result.msg()).isEqualTo("Not authenticated");
    }

    @Test
    void shouldReturn401WhenAuthIsNull() {
        SecurityContextHolder.getContext().setAuthentication(null);

        ApiResponse<UserResponse> result = authController.me();

        assertThat(result.code()).isEqualTo(401);
    }
}
