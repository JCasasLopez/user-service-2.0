package dev.jcasaslopez.user.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

// No se verifican las validaciones estándar como @NotNull, @Past o @Email, ya que 
// asumimos que Hibernate Validator las maneja correctamente. En su lugar, nos enfocamos en probar  
// los escenarios generales (objeto válido/todos los datos inválidos) y las validaciones  
// de la contraseña, donde podría haber errores en la expresión regular.
//
// We do not test standard validations like @NotNull, @Past or @Email, as we assume  
// that Hibernate Validator handles them correctly. Instead, we focus on testing  
// general scenarios (valid object/all data invalid) and the password validation,  
// where regular expressions syntax errors are more likely.
public class UserDtoPasswordValidationTest {
	
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
    	RoleDto roleDto = new RoleDto(RoleName.ROLE_USER);
    	UserDto userDto = new UserDto(
			    "Johnny",
			    "securePassword123!",
			    "John Doe",
			    "123@example.com",
			    LocalDate.of(1990, 5, 15), 
			    Set.of(roleDto),
			    AccountStatus.ACTIVE
			);
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
    	// Assert
    	assertTrue(violations.isEmpty(), "There should be no violations");
    }
    
    @Test
	@DisplayName("UserDto with multiple invalid fields should fail validation")
    void userDto_WithAllPossibleViolations_ShouldReturnMultipleViolations() {
    	// Arrange
    	UserDto userDto = new UserDto(
			    null,
			    null,
			    null,
			    null,
			    null, 
			    null,
			    null
			);
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
    	// Assert
    	assertAll(
                () -> assertEquals(7, violations.size(), "Expected exactly 7 violations"),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Username field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Password field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Full name field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Email field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Date of birth field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Roles field is required"))),
                () -> assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Account status field is required")))
            );
    }
    
    @ParameterizedTest
    // Falta: un signo - una mayúscula - un número - una minúscula - Menos de 8 caracteres.
    //
    // Missing: a symbol - capital letter - a number - a lowercase letter - Less than 8 characters.
    @CsvSource({"Qwerty123", "qwerty123!", "Qwerty!%&", "QWERTY123!", "Qwe12%"})
   	@DisplayName("UserDto with password that does not meet requirement should fail validation")
       void userDto_WithInvalidPassword_ShouldReturnPasswordViolation(String password) {
    	// Arrange
    	RoleDto roleDto = new RoleDto(RoleName.ROLE_USER);
    	UserDto userDto = new UserDto(
			    "Johnny90",
			    password,
			    "John Doe",
			    "123@example.com",
			    LocalDate.of(1990, 5, 15), 
			    Set.of(roleDto),
			    AccountStatus.ACTIVE
			);
    	
    	// Act
    	Set<ConstraintViolation<UserDto>> violations = validator.validate(userDto);
    	
		// Assert
		assertAll(() -> assertEquals(1, violations.size(), "Expected exactly 1 violation"),
				() -> assertTrue(violations.stream().anyMatch(v -> v.getMessage()
						.equals("Password must have at least 8 characters, including one upper case letter, one lower case letter, a number and a symbol"))));
    }
}
