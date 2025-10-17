package dev.jcasaslopez.user.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
public class UserAccountServiceTest {
	
	@Mock UserRepository userRepository;
	@Mock RoleRepository roleRepository;
	@InjectMocks UserAccountServiceImpl userAccountServiceImpl;
	@InjectMocks UserMapper userMapper;
	
	@Test
	@DisplayName("When user is null, it should throw IllegalArgumentException")
	public void findUser_WhenUsernameNull_ShouldThrowIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> {userAccountServiceImpl.findUser(null);
		});
	}
	
	@Test
	@DisplayName("When userOptional is empty, it should throw UsernameNotFoundException")
	public void findUser_WhenUserOptionalEmpty_ShouldThrowUsernameNotFoundException() {
		// Arrange
		String username = "Yorch22";
		when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(UsernameNotFoundException.class, () -> {userAccountServiceImpl.findUser(username);
		});
	}
		
	@Test
	@DisplayName("Search by username - When user exists, it should return a user")
	public void findUser_WhenUserOptionalNotEmpty_ShouldReturnUser() {
		// Arrange
		String username = "Yorch22";
		User user = new User(username, "Password123!", "Jorge Garcia", "jc90@gmail.com", LocalDate.of(1990, 5, 15));
		
		when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

		// Act 
		User returnedUser = userAccountServiceImpl.findUser(username);
		
		// Assert
		assertAll(
				() -> assertEquals(username, returnedUser.getUsername()),
				() -> assertEquals(user.getPassword(), returnedUser.getPassword()),
				() -> assertEquals(user.getFullName(), returnedUser.getFullName()),
				() -> assertEquals(user.getEmail(), returnedUser.getEmail()),
				() -> assertEquals(user.getDateOfBirth(), returnedUser.getDateOfBirth())
				);		
	}
	
	@Test
	@DisplayName("When userOptional is empty, it should throw UsernameNotFoundException")
	public void findUserByEmail_WhenUserOptionalEmpty_ShouldThrowUsernameNotFoundException() {
		// Arrange
		String email = "jc90@gmail.com";
		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(UsernameNotFoundException.class, () -> {userAccountServiceImpl.findUserByEmail(email);
		});
	}
	
	@Test
	@DisplayName("Search by email - When user exists, it should return a user")
	public void findUserByEmail_WhenUserOptionalNotEmpty_ShouldReturnUser() {
		// Arrange
		String email = "jc90@gmail.com";
		User user = new User("Yorch22", "Password123!", "Jorge Garcia", email, LocalDate.of(1990, 5, 15));
		
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		// Act 
		User returnedUser = userAccountServiceImpl.findUserByEmail(email);
		
		// Assert
		assertAll(
				() -> assertEquals(user.getUsername(), returnedUser.getUsername()),
				() -> assertEquals(user.getPassword(), returnedUser.getPassword()),
				() -> assertEquals(user.getFullName(), returnedUser.getFullName()),
				() -> assertEquals(email, returnedUser.getEmail()),
				() -> assertEquals(user.getDateOfBirth(), returnedUser.getDateOfBirth())
				);		
	}
	
	@Test
	@DisplayName("When user is CustomUserDetails, it should create a user with ROLE_USER and ACTIVE account")
	public void createUser_WhenUserIsCustomUserDetails_ShouldCreateUserCorrectly() {
		// Arrange
		User user = new User("Yorch22", "Password123!", "Jorge Garcia", "jc90@gmail.com", LocalDate.of(1990, 5, 15));
		CustomUserDetails customUser = userMapper.userToCustomUserDetailsMapper(user);
		ArgumentCaptor<User> captorUser = ArgumentCaptor.forClass(User.class);
		when(roleRepository.findByRoleName(RoleName.ROLE_USER)).thenReturn(Optional.of(new Role(RoleName.ROLE_USER)));
		
		// Act
		userAccountServiceImpl.createUser(customUser);
		
		// Assert
		verify(userRepository).save(captorUser.capture());
		assertAll(
				() -> assertTrue(captorUser.getValue().getRoles().stream()
					    				.anyMatch(role -> role.getRoleName() == RoleName.ROLE_USER)),
				() -> assertEquals(1, captorUser.getValue().getRoles().size()),
				() -> assertEquals(AccountStatus.ACTIVE, captorUser.getValue().getAccountStatus())
				);		
	}
	
	// *******************************************************************************************************
	// upgradeUser() y updateAccountStatus() are already covered by integration tests (see controller package).
	// *******************************************************************************************************

}