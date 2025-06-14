package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserLoginAttemptRelationshipTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private EntityManager entityManager;
	@Autowired private RoleRepository roleRepository;
	
	private User persistedUser1;
	
	@BeforeEach
	private void setUp() {
		Role roleUser = new Role(RoleName.ROLE_USER);
	    roleRepository.save(roleUser);
	    
		User user1 = new User(
			    "Yorch22",
			    "Password123!",
			    "Jorge Garcia",
			    "jc90@gmail.com",
			    LocalDate.of(1990, 5, 15)
			);
		
		user1.setAccountStatus(AccountStatus.ACTIVE);
		user1.setRoles(Set.of(roleUser));
		persistedUser1 = userRepository.save(user1);
		
		LoginAttempt attempt1 = new LoginAttempt(
				LocalDateTime.of(2025, 3, 23, 9, 15), 
				false, 
				"192.168.1.10",
				LoginFailureReason.INCORRECT_PASSWORD,
				user1);

		LoginAttempt attempt2 = new LoginAttempt(
				LocalDateTime.of(2025, 3, 23, 10, 42), 
				false, 
				"192.168.1.23",
				LoginFailureReason.ACCOUNT_LOCKED,
				user1);
		
		attempt1.setUser(user1);
		attempt2.setUser(user1);
		
		loginAttemptRepository.save(attempt1);
		loginAttemptRepository.save(attempt2);
		
		entityManager.flush();
		entityManager.clear();
	}
	
	@Test
	@DisplayName("Relationship User to LoginAttempt correctly configured")
	void givenUserWithLoginAttempts_whenSaved_thenAttemptsLinkedToUser() {
		// Arrange
		
		// Act
		
		// We need to reload the user from the database.
		User userReloaded = userRepository.findById(persistedUser1.getIdUser()).get();
		
		// Assert
		assertEquals(2, userReloaded.getLoginAttempts().size(), "The list of attempts should have 2 elements, "
				+ "but had " + userReloaded.getLoginAttempts().size());
	}
	
	@Test
	@DisplayName("Relationship LoginAttempt to User correctly configured")
	void givenLoginAttempts_whenRetrieved_thenUserIsLinkedCorrectly() {
	    // Arrange

	    // Act
	    List<LoginAttempt> attempts = loginAttemptRepository.findAll();

	    // Assert
	    assertEquals(2, attempts.size(), "Should retrieve 2 login attempts from the database");

	    for (LoginAttempt attempt : attempts) {
	        assertNotNull(attempt.getUser(), "LoginAttempt should have a User assigned");
	        assertEquals(persistedUser1.getIdUser(), attempt.getUser().getIdUser(),
	                "LoginAttempt should be linked to the correct User");
	    }
	}
}
