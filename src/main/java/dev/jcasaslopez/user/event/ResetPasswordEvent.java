package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.dto.UserDto;

public class ResetPasswordEvent {
	
	private UserDto user;

	public ResetPasswordEvent(UserDto user) {
		this.user = user;
	}

	public UserDto getUser() {
		return user;
	}

}
