package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.dto.UserDto;

public class VerifyEmailEvent {
	
	private final UserDto user;
	private final String token;
	
	public VerifyEmailEvent(UserDto user, String token) {
		this.user = user;
		this.token = token;
	}

	public UserDto getUser() {
		return user;
	}

	public String getToken() {
		return token;
	}
}