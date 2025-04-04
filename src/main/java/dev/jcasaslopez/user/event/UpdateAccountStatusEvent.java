package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.enums.AccountStatus;

public class UpdateAccountStatusEvent {
	
	private String email;
	private String username;
	private AccountStatus newAccountStatus;
	
	public UpdateAccountStatusEvent(String email, String username, AccountStatus newAccountStatus) {
		this.email = email;
		this.username = username;
		this.newAccountStatus = newAccountStatus;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public AccountStatus getNewAccountStatus() {
		return newAccountStatus;
	}

}
