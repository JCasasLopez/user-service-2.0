package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;

public class UpdateAccountStatusEvent {
	
	private User user;
	private AccountStatus newAccountStatus;
	
	public UpdateAccountStatusEvent(User user, AccountStatus newAccountStatus) {
		this.user = user;
		this.newAccountStatus = newAccountStatus;
	}

	public User getUser() {
		return user;
	}
	
	public AccountStatus getNewAccountStatus() {
		return newAccountStatus;
	}

}
