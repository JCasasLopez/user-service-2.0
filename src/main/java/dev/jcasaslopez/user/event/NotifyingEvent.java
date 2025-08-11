package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;

public class NotifyingEvent {
	
	private User user;
	private String token;
	private AccountStatus accountStatus;
	
	// Overloaded constructors accommodate varying data needs for events:
	// - User-only events: ChangePassword, CreateAccount, ResetPassword
	// - User + AccountStatus: UpdateAccountStatus
	// - User + token: ForgotPassword, VerifyEmail

	public NotifyingEvent(User user) {
		this.user = user;
	}

	public NotifyingEvent(User user, AccountStatus accountStatus) {
		this.user = user;
		this.accountStatus = accountStatus;
	}

	public NotifyingEvent(User user, String token) {
		this.user = user;
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}
	
}
