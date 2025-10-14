package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

// These tests do not use the TestHelper user creation methods. The reason is that TestHelper has multiple
// dependencies, so using it here would require either mocking all of them (which is complex and error-prone),
// or loading the entire application context, which goes against the purpose of these tests. 
// These tests focus solely on the repository layer and therefore only require beans related to it (@DataJpaTest).

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRoleRelationshipTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private EntityManager entityManager;
	
	private User persistedUser1;
	private User persistedUser2;
	
	@BeforeEach
	private void setUp() {	
		User user1 = new User("Yorch22", "Password123!", "Jorge Garcia", "jc90@gmail.com", LocalDate.of(1990, 5, 15));
		Role roleUser = roleRepository.findByRoleName(RoleName.ROLE_USER).orElseThrow(() -> new IllegalStateException("ROLE_USER should exist in database"));
		Role roleAdmin = roleRepository.findByRoleName(RoleName.ROLE_ADMIN).orElseThrow(() -> new IllegalStateException("ADMIN_USER should exist in database"));
		
		// Set.of() should not be used because it creates an immutable collection, and Hibernate needs to modify it internally.
		Set<Role> user2Roles = new HashSet<>(Arrays.asList(roleUser, roleAdmin));
		user1.setRoles(user2Roles);
		user1.setAccountStatus(AccountStatus.ACTIVE);
			    		
		User user2 = new User("Laura", "Password456!", "Laura Smith", "laura92@example.com", LocalDate.of(1992, 6, 11));
		
		Set<Role> user1Role = new HashSet<>(Arrays.asList(roleUser));
		user2.setRoles(user1Role);
		user2.setAccountStatus(AccountStatus.ACTIVE);
		
		persistedUser1 = userRepository.save(user1);
		persistedUser2 = userRepository.save(user2);
		
		entityManager.flush();
		entityManager.clear();
	}
	
	@Test
	@DisplayName("Relationship User to Role correctly configured")
	void givenUserWithRoles_whenSaved_thenRolesLinkedToUser() {
		// Arrange
		
		// Act
		User userReloaded1 = userRepository.findById(persistedUser1.getIdUser()).orElseThrow(() -> new UsernameNotFoundException("User not found in the database"));
		User userReloaded2 = userRepository.findById(persistedUser2.getIdUser()).orElseThrow(() -> new UsernameNotFoundException("User not found in the database"));
		
		// Assert
		assertAll(
				() -> assertEquals(2, userReloaded1.getRoles().size(), "User 1 has " + userReloaded1.getRoles().size() + " but should have 2"),
				() -> assertEquals(1, userReloaded2.getRoles().size(), "User 2 has " + userReloaded2.getRoles().size() + " but should have 1")
				);
	}
	
	@Test
	@DisplayName("Relationship Role to User correctly configured")
	void givenRoles_whenRetrieved_thenUserIsLinkedCorrectly() {
		// Arrange
		
		// Act
		Role roleUser = roleRepository.findByRoleName(RoleName.ROLE_USER).orElseThrow(() -> new AssertionError("ROLE_USER should exist in database"));

		Role roleAdmin = roleRepository.findByRoleName(RoleName.ROLE_ADMIN).orElseThrow(() -> new AssertionError("ROLE_ADMIN should exist in database"));

		// Assert
		Set<User> usersWithRoleUser = roleUser.getUsers();
		Set<User> usersWithRoleAdmin = roleAdmin.getUsers();

		assertAll(
		    () -> assertEquals(2, usersWithRoleUser.size(), "ROLE_USER should be assigned to 2 users"),
		    () -> assertTrue(usersWithRoleUser.stream().anyMatch(u -> u.getIdUser() == persistedUser1.getIdUser()), "User 1 should have ROLE_USER"),
		    () -> assertTrue(usersWithRoleUser.stream().anyMatch(u -> u.getIdUser() == persistedUser2.getIdUser()), "User 2 should have ROLE_USER"),
		    
		    () -> assertEquals(1, usersWithRoleAdmin.size(), "ROLE_ADMIN should be assigned to 1 user"),
		    () -> assertTrue(usersWithRoleAdmin.stream().anyMatch(u -> u.getIdUser() == persistedUser1.getIdUser()), "User 1 should have ROLE_ADMIN")
		);
	}
}
