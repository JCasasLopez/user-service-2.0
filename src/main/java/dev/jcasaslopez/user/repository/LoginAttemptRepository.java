package dev.jcasaslopez.user.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.user.entity.LoginAttempt;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
	
	// Esta consulta únicamente se usa en el test de integración de CustomAuthenticationSuccessHandler
	//
	// This query is only used for CustomAuthenticationSuccessHandler integration test.
	List<LoginAttempt> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}