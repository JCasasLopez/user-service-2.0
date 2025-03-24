package dev.jcasaslopez.user.dto;

import java.time.LocalDateTime;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import jakarta.validation.constraints.NotNull;

public class LoginAttemptDto {
	
	@NotNull
	private LocalDateTime timestamp;
	
	@NotNull
	private boolean successful;
	
	@NotNull
	private String ipAddress;
	
	@NotNull
	private LoginFailureReason loginFailureReason;
	
	@NotNull
	private User user;

	public LoginAttemptDto(LocalDateTime timestamp, boolean successful, String ipAddress,
			LoginFailureReason loginFailureReason, User user) {
		this.timestamp = timestamp;
		this.successful = successful;
		this.ipAddress = ipAddress;
		this.loginFailureReason = loginFailureReason;
		this.user = user;
	}

	public LoginAttemptDto() {
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public LoginFailureReason getLoginFailureReason() {
		return loginFailureReason;
	}

	public void setLoginFailureReason(LoginFailureReason loginFailureReason) {
		this.loginFailureReason = loginFailureReason;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
}
