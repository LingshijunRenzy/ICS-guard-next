package guard.ics.backend.security;

import guard.ics.backend.rbac.entity.PermissionEntity;
import guard.ics.backend.rbac.entity.RoleEntity;
import guard.ics.backend.rbac.entity.UserEntity;
import guard.ics.backend.rbac.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        PermissionEntity perm = PermissionEntity.builder().id(1L).name("alerts:read").description("View alerts").build();
        RoleEntity role = new RoleEntity(1L, "ROLE_ADMIN", null, Set.of(perm));
        UserEntity user = UserEntity.builder()
                .id(1L).username("admin").passwordHash("{bcrypt}hash").enabled(true)
                .roles(Set.of(role)).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).extracting("authority").contains("alerts:read");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody");
    }

    @Test
    void shouldLoadDisabledUser() {
        UserEntity user = UserEntity.builder()
                .id(2L).username("disabled_user").passwordHash("hash").enabled(false)
                .roles(Set.of()).build();

        when(userRepository.findByUsername("disabled_user")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("disabled_user");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void shouldMapMultiplePermissions() {
        PermissionEntity p1 = PermissionEntity.builder().id(1L).name("alerts:read").build();
        PermissionEntity p2 = PermissionEntity.builder().id(2L).name("alerts:manage").build();
        RoleEntity role = new RoleEntity(1L, "ROLE_OPERATOR", null, Set.of(p1, p2));
        UserEntity user = UserEntity.builder()
                .id(1L).username("operator").passwordHash("hash").enabled(true)
                .roles(Set.of(role)).build();

        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("operator");

        assertThat(result.getAuthorities()).hasSize(2);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("alerts:read", "alerts:manage");
    }
}
