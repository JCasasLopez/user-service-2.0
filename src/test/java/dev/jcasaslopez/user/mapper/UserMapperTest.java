package dev.jcasaslopez.user.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import dev.jcasaslopez.user.dto.RoleDto;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;

// Estas anotaciones permiten evitar la carga completa del contexto con @SpringBootTest.  
// En su lugar, inicializamos solo UserMapper y la configuración mínima necesaria.  
//
// Instead of loading the full context with @SpringBootTest, we initialize only  
// UserMapper and the minimal required configuration.
@ExtendWith(SpringExtension.class)
@Import({UserMapper.class, RoleMapper.class})
public class UserMapperTest {
	
	@Autowired
	private UserMapper userMapper;
	
	@Test
	@DisplayName("userToUserDtoMapper() should map correctly from User to UserDto")
	void userToUserDtoMapper_ShoudMapCorrectlyToUserDto() {
		// Arrange
		Role role = new Role(RoleName.ROLE_USER);
		User user = new User(
			    "Johnny",
			    "Qwerty123!",
			    "John Doe",
			    "123@example.com",
			    LocalDate.of(1990, 5, 15), 
			    Set.of(role), 
			    AccountStatus.ACTIVE
			);
		
		// Act
		UserDto mappedUserDto = userMapper.userToUserDtoMapper(user);
		
		// Assert
		assertAll(
		        () -> assertEquals(user.getUsername(), mappedUserDto.getUsername(), 
		        		"USernames should match"),
		        () -> assertEquals(user.getPassword(), mappedUserDto.getPassword(), 
		        		"Passwords should match"),
		        () -> assertEquals(user.getFullName(), mappedUserDto.getFullName(), 
		        		"Full names should match"),
		        () -> assertEquals(user.getEmail(), mappedUserDto.getEmail(), 
		        		"Emails should match"),
		        () -> assertEquals(user.getDateOfBirth(), mappedUserDto.getDateOfBirth(), 
		        		"Dates of birth times should match"),
		        () -> {
		            Set<RoleName> expectedRoleNames = user.getRoles().stream()
		                .map(r -> r.getRoleName())
		                .collect(Collectors.toSet());

		            Set<RoleName> actualRoleNames = mappedUserDto.getRoles().stream()
		                .map(r -> r.getRoleName())
		                .collect(Collectors.toSet());

		            assertEquals(expectedRoleNames, actualRoleNames, "Role names should match");
		        },
		        
		        () -> assertEquals(user.getAccountStatus(), mappedUserDto.getAccountStatus(), 
		        		"Account status should match")
		    );
	}
	
	@Test
	@DisplayName("userDtoToUserMapper() should map correctly from UserDto to User")
	void userDtoToUserMapper_ShoudMapCorrectlyToUser() {
		// Arrange
		RoleDto roleDto = new RoleDto(RoleName.ROLE_USER);
		UserDto userDto = new UserDto(
			    "Johnny",
			    "Qwerty123!",
			    "John Doe",
			    "123@example.com",
			    LocalDate.of(1990, 5, 15), 
			    Set.of(roleDto), 
			    AccountStatus.ACTIVE
			);
		
		// Act
		User mappedUser = userMapper.userDtoToUserMapper(userDto);
		
		// Assert
		assertAll(
		        () -> assertEquals(userDto.getUsername(), mappedUser.getUsername(), 
		        		"USernames should match"),
		        () -> assertEquals(userDto.getPassword(), mappedUser.getPassword(), 
		        		"Passwords should match"),
		        () -> assertEquals(userDto.getFullName(), mappedUser.getFullName(), 
		        		"Full names should match"),
		        () -> assertEquals(userDto.getEmail(), mappedUser.getEmail(), 
		        		"Emails should match"),
		        () -> assertEquals(userDto.getDateOfBirth(), mappedUser.getDateOfBirth(), 
		        		"Dates of birth times should match"),
		        () -> {
		            Set<RoleName> expectedRoleNames = userDto.getRoles().stream()
		                .map(r -> r.getRoleName())
		                .collect(Collectors.toSet());

		            Set<RoleName> actualRoleNames = mappedUser.getRoles().stream()
		                .map(r -> r.getRoleName())
		                .collect(Collectors.toSet());

		            assertEquals(expectedRoleNames, actualRoleNames, "Role names should match");
		        },
		        
		        () -> assertEquals(userDto.getAccountStatus(), mappedUser.getAccountStatus(), 
		        		"Account status should match")
		    );
	}

}
