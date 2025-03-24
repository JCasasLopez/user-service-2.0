package dev.jcasaslopez.user.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import dev.jcasaslopez.user.dto.LoginAttemptDto;
import dev.jcasaslopez.user.dto.RoleDto;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.LoginAttempt;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.enums.RoleName;

//Estas anotaciones permiten evitar la carga completa del contexto con @SpringBootTest.  
//En su lugar, inicializamos solo LoginAttempMapper y la configuración mínima necesaria.  
//
//Instead of loading the full context with @SpringBootTest, we initialize only  
//LoginAttempMapper and the minimal required configuration.
@ExtendWith(SpringExtension.class)
@Import({LoginAttemptMapper.class, UserMapper.class, RoleMapper.class})
public class LoginAttemptMapperTest {
	
	@Autowired
	private LoginAttemptMapper loginAttemptMapper;
	
	@Autowired
	private UserMapper userMapper;

	@Test
	@DisplayName("LoginAttemptToDtoMapper() should map correctly from LoginAttempt to LoginAttemptDto")
	void LoginAttempToDtoMapper_ShouldMapCorrectlyToLoginAttemptDto() {
		// Arrange
		User user = new User(
				"johndoe",
				"Password123!",
				"John Doe",
				"john@example.com",
				LocalDate.of(1985, 10, 20),
				Set.of(new Role(RoleName.ROLE_USER)),
				AccountStatus.ACTIVE
		);

		LoginAttempt loginAttempt = new LoginAttempt(
				LocalDateTime.of(2023, 12, 1, 14, 30),
				false,
				"192.168.0.1",
				LoginFailureReason.ACCOUNT_LOCKED
		);
		
		loginAttempt.setUser(user);

		// Act
		LoginAttemptDto loginAttemptDto = loginAttemptMapper.LoginAttempToDtoMapper(loginAttempt);
		
		// Assert
		assertAll(
				() -> assertEquals(loginAttempt.getTimestamp(), loginAttemptDto.getTimestamp(),
						"Timestamps should match"),
				() -> assertEquals(loginAttempt.isSuccessful(), loginAttemptDto.getSuccessful(),
						"Success flags should match"),
				() -> assertEquals(loginAttempt.getIpAddress(), loginAttemptDto.getIpAddress(),
						"IP addresses should match"),
				() -> assertEquals(loginAttempt.getLoginFailureReason(), loginAttemptDto.getLoginFailureReason(),
						"Login failure reasons should match"),
				() -> assertEquals(loginAttempt.getUser().getUsername(), loginAttemptDto.getUser().getUsername(),
						"Users should match")
		);
	}

	@Test
	@DisplayName("LoginAttempDtoToLoginAttemptMapper() should map correctly from LoginAttemptDto to LoginAttempt")
	void LoginAttempDtoToLoginAttemptMapper_ShouldMapCorrectlyToLoginAttempt() {
		// Arrange
		UserDto user = new UserDto(
				"janedoe",
				"Secure456!",
				"Jane Doe",
				"jane@example.com",
				LocalDate.of(1992, 3, 10),
				Set.of(new RoleDto(RoleName.ROLE_ADMIN)),
				AccountStatus.ACTIVE
		);

		LoginAttemptDto loginAttemptDto = new LoginAttemptDto(
				LocalDateTime.of(2024, 1, 5, 9, 15),
				true,
				"10.0.0.5",
				LoginFailureReason.INCORRECT_PASSWORD,
				user
		);
		
		// Act
		LoginAttempt loginAttempt = loginAttemptMapper.LoginAttempDtoToLoginAttemptMapper(loginAttemptDto);
		loginAttempt.setUser(userMapper.userDtoToUserMapper(user));

		// Assert
		assertAll(
				() -> assertEquals(loginAttemptDto.getTimestamp(), loginAttempt.getTimestamp(),
						"Timestamps should match"),
				() -> assertEquals(loginAttemptDto.getSuccessful(), loginAttempt.isSuccessful(),
						"Success flags should match"),
				() -> assertEquals(loginAttemptDto.getIpAddress(), loginAttempt.getIpAddress(),
						"IP addresses should match"),
				() -> assertEquals(loginAttemptDto.getLoginFailureReason(), loginAttempt.getLoginFailureReason(),
						"Login failure reasons should match"),
				() -> assertEquals(loginAttemptDto.getUser().getUsername(), loginAttempt.getUser().getUsername(),
						"Users should match")
		);
	}
}
