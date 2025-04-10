package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;

public class CreateAccountEvent {
	
	private final User user;

	public CreateAccountEvent(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}
}