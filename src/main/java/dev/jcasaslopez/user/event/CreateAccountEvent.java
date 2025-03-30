package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.dto.UserDto;

public class CreateAccountEvent {
	
	private final UserDto user;

	public CreateAccountEvent(UserDto user) {
		this.user = user;
	}

	public UserDto getUser() {
		return user;
	}
	
}
