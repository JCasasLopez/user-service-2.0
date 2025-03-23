package dev.jcasaslopez.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.jcasaslopez.user.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
	
	// Estos métodos sirven para nuestra implementación personalizada de UserDetailsManager (Spring Security):
	//
	// These methods are part of our custom UserDetailsManager implementation (Spring Security):
	//
	// Repository 		-> 	UserDetailsManager
	// ---------------------------------------
	// findByUsername() -> loadUserByUsername() 
	// save() -> createUser(), updateUser() 
	// deleteByUsername() -> deleteUser() 
	// updatePassword() -> changePassword() 
	// existsByUsername() -> userExists()
	
	Optional<User> findByUsername(String username);
	
	Optional<User> findByEmail(String email);
	
	void deleteByUsername(String username);
	
	@Modifying
    @Query("UPDATE User u SET u.password = ?2 WHERE u.username = ?1")
    void updatePassword(String username, String newPassword);
	
	boolean existsByUsername(String username);

}
