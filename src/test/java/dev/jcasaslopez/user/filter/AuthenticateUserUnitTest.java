package dev.jcasaslopez.user.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.security.filter.AuthenticationFilter;

@ExtendWith(MockitoExtension.class)
public class AuthenticateUserUnitTest {
	
	@Mock UserRepository userRepository;
	@Mock UserMapper userMapper;
	@InjectMocks AuthenticationFilter authenticationFilter;
	
	private static final String USERNAME = "Yorch";
	private static final String TOKEN = "valid_token";

	@AfterEach
	void clearSecurityContext() {
	    SecurityContextHolder.clearContext();
	}
	
	@Test
	@DisplayName("It authenticates the user if found in the DB")
	void authenticaUser_WhenUserFoundInDatabase_ShouldAuthenticateUser() {
		// Arrange
		User user = new User();
        CustomUserDetails userDetails = new CustomUserDetails(user); 
        
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userMapper.userToCustomUserDetailsMapper(user)).thenReturn(userDetails);
		
		// Act
        authenticationFilter.authenticateUser(TOKEN, USERNAME);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
		// Assert
        assertAll("Authentication should be properly set in the SecurityContext",
        	    () -> assertNotNull(auth, "Authentication should not be null"),
        	    () -> assertEquals(userDetails, auth.getPrincipal(), "Principal should match the expected user details"),
        	    () -> assertEquals(TOKEN, auth.getCredentials(), "Credentials should match the provided token"),
        	    () -> assertTrue(auth.isAuthenticated(), "Authentication should be marked as authenticated")
        	);	
        }
	
	@Test
	@DisplayName("It should throw an exception if the user is not found in the DB")
	void authenticaUser_WhenUserNotFoundInDatabase_ShouldThrowException() {
		// Arrange
		when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(UsernameNotFoundException.class, 
				() -> authenticationFilter.authenticateUser(TOKEN, USERNAME));
		       
	}
}