package dev.jcasaslopez.user.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;

// The happy path is verified in "FullRestAndChangePasswordIntegrationTest".
@ExtendWith(MockitoExtension.class)
public class PasswordServiceTest {
	
	@Mock UserRepository userRepository;
	@Mock PasswordEncoder passwordEncoder;
	@Mock UserAccountService userAccountService;
	@InjectMocks PasswordServiceImpl passwordServiceImpl;
	
	private static final String VALID_OLD_PASSWORD_IN_DB = "Jorge22!";
	
	private User mockUser(String passwordInDatabase) {
	    User user = new User();
	    user.setUsername("testuser");
	    user.setPassword(passwordInDatabase);
	    return user;
	}
	
	// These tests verify passwordIsValid() behaviour, which is a completely different functionality that the DTO
	// validations (tested in UserDtoValidationTest). 
	// Both functionalities do essentially the same, but passwordIsValid() is called when we use the corresponding 
	// endpoints to either reset or change the password, whereas the DTO validations are invoked when a user is registered.
	@ParameterizedTest
	@DisplayName("When password format is valid, should return true")
	@CsvSource({
	    "Jorge22!",      
	    "Ana1234#",      
	    "ValidPass1@",   
	    "Strong1$"       
	})
	public void passwordIsValid_WhenPasswordIsValid_ShouldReturnTrue(String password) {
	    boolean result = passwordServiceImpl.passwordIsValid(password);
	    assertTrue(result);
	}

	
	@ParameterizedTest
	@DisplayName("When password format is not valid, should throw exception")
	@CsvSource({"Jorg22!", 			// too short (at least 8 characters)
				"jorge22!", 		// no capital letter
				"JORGE22!", 		// no lower case letter
				"JorgeGarcia!!", 	// no number
				"JorgeGecias22" 	// no symbol
		})	
	public void passwordIsValid_WhenPasswordIsNotValid_ShouldThrowException(String password) {
		// Arrange
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> passwordServiceImpl.passwordIsValid(password));
	}
	
	@Test
    void resetPassword_WhenNewPasswordMatchesOld_ThrowsException() {
		// Arrange
		String newPassword = VALID_OLD_PASSWORD_IN_DB;
		String passwordInDatabase = VALID_OLD_PASSWORD_IN_DB;
        User user = mockUser(passwordInDatabase);
        
        // Act
        when(passwordEncoder.matches(passwordInDatabase, newPassword)).thenReturn(true);

        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
        	passwordServiceImpl.resetPassword(newPassword, user);
        });
    }
	
	@Test
    void changePassword_WhenProvidedPasswordDoesNotMatchOldOne_ThrowsException() {
		// Arrange
		String providedPassword = "newPass1!";
	    String newPassword = "Qwerty22!";
		String passwordInDatabase = VALID_OLD_PASSWORD_IN_DB;
	    
        when(userAccountService.getAuthenticatedUser()).thenReturn(mockUser(passwordInDatabase));
        when(passwordEncoder.matches(providedPassword, passwordInDatabase)).thenReturn(false);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
        	passwordServiceImpl.changePassword(providedPassword, newPassword);
        });
    }
	
	@Test
    void changePassword_WhenNewPasswordMatchesOldOne_ThrowsException() {
		// Arrange
		String providedPassword = VALID_OLD_PASSWORD_IN_DB;
		String passwordInDatabase = VALID_OLD_PASSWORD_IN_DB;
	    String newPassword = "Jorge22!";
	    
	    when(userAccountService.getAuthenticatedUser()).thenReturn(mockUser(passwordInDatabase));
        when(passwordEncoder.matches(providedPassword, passwordInDatabase)).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
        	passwordServiceImpl.changePassword(providedPassword, newPassword);
        });
	}
}