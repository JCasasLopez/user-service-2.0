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
	
	private static final String VALID_USERNAME = "Yorch22";
    private static final String VALID_PASSWORD = "Password123!";
    private static final String VALID_FULLNAME = "Jorge García";
    private static final String VALID_EMAIL = "123@test.com";
    private static final LocalDate VALID_DATE_OF_BIRTH = LocalDate.of(1990, 5, 15);

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    @DisplayName("Valid UserDto should pass validation")
    void userDto_WithValidData_ShouldReturnNoViolations() {
    	// Arrange
    	UserDto userDto = new UserDto(VALID_USERNAME, VALID_PASSWORD, VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH);
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
    	// Assert
    	assertTrue(violations.isEmpty(), "There should be no violations");
    }
    
    // This test verifies the UserDto validation. There is also a service-level password validation 
    // that does NOT use Hibernate Validator, but a custom-made service method (PasswordService's passwordIsValid()). 
    // This second method is tested in the service tests package (PasswordServiceUnitTest), where the reason for
    // this method's exitence is clarified as well.
    @ParameterizedTest
    @MethodSource("provideUserDtoViolations")
    @DisplayName("UserDto should fail validation for boundary, size, and format errors")
    void userDto_WithBoundaryAndFormatViolations_ShouldFailViolation(UserDto invalidUserDto, String violationMessage) {
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(invalidUserDto);

    	// Assert
    	assertAll(() -> assertEquals(1, violations.size(), "Expected exactly 1 violation"),
    			() -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(violationMessage))));
    }
    
 // ************** DATA PROVIDER METHOD **************
    
    static Stream<Arguments> provideUserDtoViolations() {
    	final String INVALID_PASSWORD_MESSAGE = "Password must have at least 8 characters, including one upper-case letter, one lower-case letter, a number and a symbol";
    	final String REQUIRED_USERNAME_MESSAGE = "Username field is required";
    	final String USERNAME_LENGTH_MESSAGE = "Username must be between 6 and 20 characters";
    	final String REQUIRED_PASSWORD_MESSAGE = "Password field is required";
    	final String REQUIRED_FULLNAME_MESSAGE = "Full name field is required";
    	final String FULLNAME_LENGTH_MESSAGE = "Fullname must be shorter than 30 characters";
    	final String REQUIRED_EMAIL_MESSAGE = "Email field is required";
    	final String EMAIL_FORMAT_MESSAGE = "Email must have a correct email format";
    	final String REQUIRED_DOB_MESSAGE = "Date of birth field is required";
		
    	return Stream.of(
    			// Username is null
    			Arguments.of(
    					new UserDto(null, VALID_PASSWORD, VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					REQUIRED_USERNAME_MESSAGE), 
    			
    			// Username Too Short (min=6)
    			Arguments.of(
    					new UserDto("Yorch", VALID_PASSWORD, VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					USERNAME_LENGTH_MESSAGE), 

    			// Username Too Long (max=20)
    			Arguments.of(
    					new UserDto("This username is longer than 20 characters", VALID_PASSWORD, VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					USERNAME_LENGTH_MESSAGE), 
    			
    			// Password is null
    			Arguments.of(
    					new UserDto(VALID_USERNAME, null, VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					REQUIRED_PASSWORD_MESSAGE), 
    			
    			// Password misses a sign
    			Arguments.of(
    					new UserDto(VALID_USERNAME, "Password123", VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					INVALID_PASSWORD_MESSAGE), 
    			
    			// Password misses a capital letter
    			Arguments.of(
    					new UserDto(VALID_USERNAME, "password123!", VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					INVALID_PASSWORD_MESSAGE), 
    			
    			// Password misses a number
    			Arguments.of(
    					new UserDto(VALID_USERNAME, "Password!", VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					INVALID_PASSWORD_MESSAGE), 
    			
    			// Password misses a lower-case letter
    			Arguments.of(
    					new UserDto(VALID_USERNAME, "PASSWORD123!", VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					INVALID_PASSWORD_MESSAGE), 
    			
    			// Password is too short
    			Arguments.of(
    					new UserDto(VALID_USERNAME, "Pass12%", VALID_FULLNAME, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					INVALID_PASSWORD_MESSAGE), 
    			
    			// Full name is null
    			Arguments.of(
    					new UserDto(VALID_USERNAME, VALID_PASSWORD, null, VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					REQUIRED_FULLNAME_MESSAGE), 
    			
    			// Full name Too Long (max=30)
    			Arguments.of(
    					new UserDto(VALID_USERNAME, VALID_PASSWORD, "Jorge Garcíaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", VALID_EMAIL, VALID_DATE_OF_BIRTH), 
    					FULLNAME_LENGTH_MESSAGE), 
    			
    			// Email is null
    			Arguments.of(
    					new UserDto(VALID_USERNAME, VALID_PASSWORD, VALID_FULLNAME, null, VALID_DATE_OF_BIRTH), 
    					REQUIRED_EMAIL_MESSAGE), 

    			// Email must have a correct format (@Email)
    			Arguments.of(
    					new UserDto(VALID_USERNAME, VALID_PASSWORD, VALID_FULLNAME, "123_test.com", VALID_DATE_OF_BIRTH), 
    					EMAIL_FORMAT_MESSAGE),
    			
    			// Date of birth is null
    			Arguments.of(
    					new UserDto(VALID_USERNAME, VALID_PASSWORD, VALID_FULLNAME, VALID_EMAIL, null), 
    					REQUIRED_DOB_MESSAGE)
    			);
    }
}
