package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import dev.jcasaslopez.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
class UniquenessUserFieldsTest {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EntityManager entityManager;
	
	@BeforeEach
	private void setUp() {
		User user1 = new User(
			    "Johnny",
			    "securePassword123",
			    "John Doe",
			    "123@example.com",
			    LocalDate.of(1990, 5, 15)
			);
		
		userRepository.save(user1);
		entityManager.flush();
	}

	@Test
	@DisplayName("User entity throws exception when 2 users have the same username")
	void userEntity_WhenUsernameUnicityViolated_ShouldThrowException() {
		// Arrange
		User user2 = new User(
			    "Johnny",
			    "anotherPassword456",
			    "Jane Doe",
			    "laura92@example.com",
			    LocalDate.of(1992, 8, 22)
			);
		
		// Act & Assert
		assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user2),
				"DataIntegrityViolationException was expected, but was not thrown");
	}
	
	@Test
	@DisplayName("User entity throws exception when 2 users have the same email")
	void userEntity_WhenEmailUnicityViolated_ShouldThrowException() {
		// Arrange
		User user2 = new User(
				"Laura", 
				"anotherPassword456", 
				"Jane Doe", 
				"123@example.com",
				LocalDate.of(1992, 8, 22)
			    );

		// Act & Assert
		assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user2),
				"DataIntegrityViolationException was expected, but was not thrown");
	}

}
