package dev.jcasaslopez.user.mapper;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.dto.LoginAttemptDto;
import dev.jcasaslopez.user.entity.LoginAttempt;

@Component
public class LoginAttempMapper {
	
	public LoginAttemptDto LoginAttempToDtoMapper (LoginAttempt loginAttempt) {
		return new LoginAttemptDto(loginAttempt.getIdLoginAttempt(),
				loginAttempt.getTimestamp(),
				loginAttempt.isSuccessful(),
				loginAttempt.getIpAddress(),
				loginAttempt.getLoginFailureReason(),
				loginAttempt.getUser());
	}
	
	public LoginAttempt LoginAttempDtoToLoginAttemptMapper (LoginAttemptDto loginAttempt) {
		return new LoginAttempt(
				loginAttempt.getTimestamp(),
				loginAttempt.getSuccessful(),
				loginAttempt.getIpAddress(),
				loginAttempt.getLoginFailureReason()
				);
	}
	
}
