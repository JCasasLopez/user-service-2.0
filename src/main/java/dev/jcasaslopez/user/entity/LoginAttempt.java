package dev.jcasaslopez.user.entity;

import java.time.LocalDateTime;

import dev.jcasaslopez.user.enums.LoginFailureReason;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="login_attempts")
public class LoginAttempt {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idLoginAttempt;
	private LocalDateTime timestamp;
	private boolean successful;
	private String ipAddress;
	private LoginFailureReason loginFailureReason;
	
	@ManyToOne
	// Setting nullable = true allows login attempts to persist even if the associated user is deleted,
	// since they still hold statistical value.
	@JoinColumn(name = "idUser", referencedColumnName = "idUser", nullable = true)
	private User user;

	public LoginAttempt(LocalDateTime timestamp, boolean successful, String ipAddress,
			LoginFailureReason loginFailureReason, User user) {
		this.timestamp = timestamp;
		this.successful = successful;
		this.ipAddress = ipAddress;
		this.loginFailureReason = loginFailureReason;
		this.user = user;
	}

	public LoginAttempt() {
		super();
	}

	public long getIdLoginAttempt() {
		return idLoginAttempt;
	}

	public void setIdLoginAttempt(long idLoginAttempt) {
		this.idLoginAttempt = idLoginAttempt;
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
