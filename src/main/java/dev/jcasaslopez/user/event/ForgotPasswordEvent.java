package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.dto.UserDto;

public class ForgotPasswordEvent {
	
	private UserDto user;
	private String token;
	
	public ForgotPasswordEvent(UserDto user, String token) {
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
