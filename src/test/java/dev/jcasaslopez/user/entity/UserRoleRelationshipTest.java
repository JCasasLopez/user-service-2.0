package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

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
		Role roleUser = new Role(RoleName.ROLE_USER);
		Role roleAdmin = new Role(RoleName.ROLE_ADMIN);
		
		roleRepository.save(roleUser);
		roleRepository.save(roleAdmin);
		
		User user1 = new User(
				"Yorch22",
			    "Password123!",
			    "Jorge Garcia",
			    "jc90@gmail.com",
			    LocalDate.of(1990, 5, 15)
			    );
		
		// No se debe usar Set.of() porque crea una colección inmutable, y Hibernate 
		// necesita modificarla internamente.
		//
		// Set.of() should not be used because it creates an immutable collection, and 
		// Hibernate needs to modify it internally.
		Set<Role> user1Roles = new HashSet<>(Arrays.asList(roleUser, roleAdmin));
		user1.setRoles(user1Roles);
		user1.setAccountStatus(AccountStatus.ACTIVE);
			    		
		User user2 = new User(
			    "Laura",
			    "Password456!",
			    "Laura Smith",
			    "laura92@example.com",
			    LocalDate.of(1992, 6, 11)
			);
		
		Set<Role> user2Roles = new HashSet<>(Arrays.asList(roleUser));
		user2.setRoles(user2Roles);
		user2.setAccountStatus(AccountStatus.ACTIVE);
		
		persistedUser1 = userRepository.save(user1);
		persistedUser2 = userRepository.save(user2);
		
		userRepository.save(persistedUser1);
		userRepository.save(persistedUser2);
		
		entityManager.flush();
		entityManager.clear();
	}
	
	@Test
	@DisplayName("Relationship User to Role correctly configured")
	void givenUserWithRoles_whenSaved_thenRolesLinkedToUser() {
		// Arrange
		
		// Act
		// Necesitamos recargar el usuario desde la base de datos.
		//
		// We need to reload the user from the database.
		User userReloaded1 = userRepository.findById(persistedUser1.getIdUser()).get();
		User userReloaded2 = userRepository.findById(persistedUser2.getIdUser()).get();

		// Assert
		assertAll(
				() -> assertEquals(2, userReloaded1.getRoles().size(), "User 1 has " + 
										userReloaded1.getRoles().size() + " but should have 2"),
				() -> assertEquals(1, userReloaded2.getRoles().size(), "User 2 has " + 					
										userReloaded2.getRoles().size() + " but should have 1")
				);
	}
	
	@Test
	@DisplayName("Relationship Role to User correctly configured")
	void givenRoles_whenRetrieved_thenUserIsLinkedCorrectly() {
		// Arrange
		
		// Act
		List<Role> roles = roleRepository.findAll();

		// Assert
	    assertEquals(2, roles.size(), "Should retrieve 2 roles from the database");

	    for (Role role : roles) {
	        Set<User> users = role.getUsers();
	        assertNotNull(users, "Role should have a set of users");
	        assertFalse(users.isEmpty(), "Role should be assigned to at least one user");

	        for (User user : users) {
	            assertTrue(
	                user.getIdUser() == persistedUser1.getIdUser() || user.getIdUser() == persistedUser2.getIdUser(),
	                "User linked to role should be one of the expected persisted users"
	            );
	        }
	    }
	}
}
