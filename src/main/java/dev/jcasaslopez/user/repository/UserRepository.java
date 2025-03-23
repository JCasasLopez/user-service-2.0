package dev.jcasaslopez.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.user.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

}
