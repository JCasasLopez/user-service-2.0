package dev.jcasaslopez.user.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

// Our intention is not to test standard validations like @NotNull, @Past or @Email, as we assume that Hibernate 
// Validator handles them correctly. Instead, we focus on verifying the DTO object is correctly configured, that is, 
// the validations, and the messages launched when one of them is violated, are correct.
public class UserDtoValidationTest {
	
	private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    @DisplayName("Valid UserDto should pass validation")
    void userDto_WithValidData_ShouldReturnNoViolations() {
    	// Arrange
    	UserDto userDto = new UserDto("Yorch22", "Password123!", "Jorge García", "123@test.com", LocalDate.of(1990, 5, 15));
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
    	// Assert
    	assertTrue(violations.isEmpty(), "There should be no violations");
    }
    
    @Test
	@DisplayName("UserDto with multiple invalid fields should fail validation")
    void userDto_WithAllPossibleViolations_ShouldReturnMultipleViolations() {
    	// Arrange
    	UserDto userDto = new UserDto(null, null, null, null, null);
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
    	// Assert
    	assertAll(
                () -> assertEquals(5, violations.size(), "Expected exactly 5 violations"),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Username field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Password field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Full name field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Email field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Date of birth field is required")))
            );
    }
    
    @ParameterizedTest
    // Test cases: each password is missing one requirement (symbol, capital, number, lowercase, or min length)
    // This test verifies the UserDto validation. There is also a service-level password validation 
    // that does NOT use Hibernate Validator, but a custom-made service method (PasswordService's passwordIsValid()). 
    // This second method is tested in the service tests package (PasswordServiceUnitTest), where the reason for
    // this method's exitence is clarified as well.
    @CsvSource({"Qwerty123", "qwerty123!", "Qwerty!%&", "QWERTY123!", "Qwe12%"})
    @DisplayName("UserDto with password that does not meet requirement should fail validation")
    void userDto_WithInvalidPassword_ShouldReturnPasswordViolation(String password) {
    	// Arrange
    	UserDto userDto = new UserDto("Yorch22", password, "Jorge García", "123@test.com", LocalDate.of(1990, 5, 15));

    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);

    	// Assert
    	assertAll(() -> assertEquals(1, violations.size(), "Expected exactly 1 violation"),
    			() -> assertTrue(violations.stream().anyMatch(v -> v.getMessage()
    					.equals("Password must have at least 8 characters, including one upper case letter, one lower case letter, a number and a symbol"))));
    }
    
    @ParameterizedTest
    @MethodSource("provideUserDtoViolations")
    @DisplayName("UserDto should fail validation for boundary, size, and format errors")
    void userDto_WithBoundaryAndFormatViolations_ShouldFailViolation(UserDto userDto, String violationMessage) {
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);

    	// Assert
    	assertAll(() -> assertEquals(1, violations.size(), "Expected exactly 1 violation"),
    			() -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(violationMessage))));
    }
    
 // ************** DATA PROVIDER METHOD **************
    
    static Stream<Arguments> provideUserDtoViolations() {
    	return Stream.of(
    			
    			// Username Too Short (min=6)
    			Arguments.of(
    					new UserDto("Yorch", "Password123!", "Jorge García", "123@test.com", LocalDate.of(1990, 5, 15)), 
    					"Username must be between 6 and 20 characters"), 

    			// Username Too Long (max=20)
    			Arguments.of(
    					new UserDto("This username is longer than 20 characters", "Password123!", "Jorge García", "123@test.com", LocalDate.of(1990, 5, 15)), 
    					"Username must be between 6 and 20 characters"), 
    			
    			// Full name Too Long (max=30)
    			Arguments.of(
    					new UserDto("Yorch22", "Password123!", "Jorge Garcíaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "123@test.com", LocalDate.of(1990, 5, 15)), 
    					"Fullname must be shorter than 30 characters"), 

    			// Email must have a correct format (@Email)
    			Arguments.of(
    					new UserDto("Yorch22", "Password123!", "Jorge García", "123_test.com", LocalDate.of(1990, 5, 15)), 
    					"Email must have a correct email format")
    			);
    }
}
