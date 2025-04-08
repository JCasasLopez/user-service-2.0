package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;

public class ChangePasswordEvent {
	
	private User user;

	public ChangePasswordEvent(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

}
