package dev.jcasaslopez.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.user.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

}
