package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;

// These tests do not use the TestHelper user creation methods. The reason is that TestHelper has multiple
// dependencies, so using it here would require either mocking all of them (which is complex and error-prone),
// or loading the entire application context, which goes against the purpose of these tests. 
// These tests focus solely on the repository layer and therefore only require beans related to it (@DataJpaTest).

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UniquenessUserFieldsTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	
	private static final String REPEATED_USERNAME = "Yorch22";
	private static final String REPEATED_EMAIL = "jc90@gmail.com";

	@BeforeEach
	private void setUp() {
		Role roleUser =  roleRepository.findByRoleName(RoleName.ROLE_USER)
					.orElseThrow(() -> new IllegalStateException("ROLE_USER should exist in database"));
		User user1 = new User("Yorch22", "Password123!", "Jorge Garcia", "jc90@gmail.com", LocalDate.of(1990, 5, 15));
		user1.setAccountStatus(AccountStatus.ACTIVE);
		user1.setRoles(Set.of(roleUser));
		userRepository.save(user1);
	}

	@Test
	@DisplayName("User entity throws exception when 2 users have the same username")
	void userEntity_WhenUsernameUnicityViolated_ShouldThrowException() {
		// Arrange
		User user2 = new User(REPEATED_USERNAME, "Password456!", "Jorge Lopez", "jl92@example.com", LocalDate.of(1992, 8, 22));
		
		// Act & Assert
		assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user2),
				"DataIntegrityViolationException was expected, but was not thrown");
	}
	
	@Test
	@DisplayName("User entity throws exception when 2 users have the same email")
	void userEntity_WhenEmailUnicityViolated_ShouldThrowException() {
		// Arrange
		User user2 = new User("Jorge92", "Password456!", "Jorge Lopez", REPEATED_EMAIL, LocalDate.of(1992, 8, 22));

		// Act & Assert
		assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user2),
				"DataIntegrityViolationException was expected, but was not thrown");
	}

}
