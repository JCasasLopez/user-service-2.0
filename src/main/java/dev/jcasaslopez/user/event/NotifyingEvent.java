package dev.jcasaslopez.user.event;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.NotificationType;

public class NotifyingEvent {
	
	private User user;
	private String token;
	private AccountStatus accountStatus;
	private NotificationType notificationType;
	
	// Overloaded constructors accommodate varying data needs for events:
	// - User-only events: ChangePassword, CreateAccount, ResetPassword
	// - User + AccountStatus: UpdateAccountStatus
	// - User + token: ForgotPassword, VerifyEmail
	
	public NotifyingEvent(User user, NotificationType notificationType) {
		this.user = user;
		this.notificationType = notificationType;
	}

	public NotifyingEvent(User user, String token, NotificationType notificationType) {
		this.user = user;
		this.token = token;
		this.notificationType = notificationType;
	}

	public NotifyingEvent(User user, AccountStatus accountStatus, NotificationType notificationType) {
		this.user = user;
		this.accountStatus = accountStatus;
		this.notificationType = notificationType;
	}

	public User getUser() {
		return user;
	}

	public String getToken() {
		return token;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public NotificationType getNotificationType() {
		return notificationType;
	}
	
}