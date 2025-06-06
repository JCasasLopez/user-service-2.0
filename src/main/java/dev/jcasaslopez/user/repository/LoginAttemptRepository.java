package dev.jcasaslopez.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.user.entity.LoginAttempt;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

}