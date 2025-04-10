package dev.jcasaslopez.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	
	Optional<Role> findByRoleName(RoleName roleName);

}
