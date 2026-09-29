package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.RoleRepositoryOut;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresRoleAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresUserAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RoleDomainAndPersistenceTest {

    @Mock
    private SpringDataRoleRepository springDataRoleRepository;

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @Test
    @DisplayName("Role record should validate non empty name and normalize to uppercase")
    void roleValidation() {
        Role role = new Role(1L, "admin", "Admin role");
        assertThat(role.name()).isEqualTo("ADMIN");
        assertThat(role.description()).isEqualTo("Admin role");

        assertThatThrownBy(() -> new Role(1L, "  ", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role name cannot be empty");
    }

    @Test
    @DisplayName("User record should support roles and hasRole helper")
    void userWithRoles() {
        Role admin = new Role(1L, "ADMIN");
        Role client = new Role(2L, "CLIENT");
        User user = new User(10L, "root@root.root", "Root Admin", Set.of(admin, client), LocalDateTime.now());

        assertThat(user.hasRole("ADMIN")).isTrue();
        assertThat(user.hasRole("admin")).isTrue();
        assertThat(user.hasRole("CLIENT")).isTrue();
        assertThat(user.hasRole("MANAGER")).isFalse();
    }

    @Test
    @DisplayName("PostgresRoleAdapter should save and find roles")
    void roleAdapterOperations() {
        RoleRepositoryOut roleRepository = new PostgresRoleAdapter(springDataRoleRepository);

        RoleEntity entity = new RoleEntity(1L, "ADMIN", "Administrator");
        given(springDataRoleRepository.save(any(RoleEntity.class))).willReturn(entity);
        given(springDataRoleRepository.findByName("ADMIN")).willReturn(Optional.of(entity));
        given(springDataRoleRepository.findAll()).willReturn(List.of(entity));

        Role saved = roleRepository.save(new Role(null, "ADMIN", "Administrator"));
        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.name()).isEqualTo("ADMIN");

        Optional<Role> found = roleRepository.findByName("admin");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("ADMIN");

        List<Role> all = roleRepository.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("PostgresUserAdapter should map roles correctly")
    void userAdapterOperations() {
        UserRepositoryOut userRepo = new PostgresUserAdapter(springDataUserRepository, springDataRoleRepository);

        RoleEntity clientRoleEntity = new RoleEntity(1L, "CLIENT", "Client");
        UserEntity userEntity = new UserEntity(10L, "john@cinema.com", "John Doe", Set.of(clientRoleEntity), LocalDateTime.now());

        given(springDataUserRepository.findById(10L)).willReturn(Optional.of(userEntity));

        Optional<User> userOpt = userRepo.findById(10L);
        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().hasRole("CLIENT")).isTrue();
        assertThat(userOpt.get().roles()).hasSize(1);
    }
}
