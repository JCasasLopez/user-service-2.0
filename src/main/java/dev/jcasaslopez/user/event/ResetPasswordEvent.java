package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;

public class ResetPasswordEvent {
	
	private User user;

	public ResetPasswordEvent(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

}
