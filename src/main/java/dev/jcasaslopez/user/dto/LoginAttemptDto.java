package dev.jcasaslopez.user.dto;

import java.time.LocalDateTime;

import dev.jcasaslopez.user.enums.LoginFailureReason;
import jakarta.validation.constraints.NotNull;

public class LoginAttemptDto {
	
	private Long idLoginAttempt;
	
	@NotNull
	private LocalDateTime timestamp;
	
	@NotNull
	private Boolean successful;
	
	@NotNull
	private String ipAddress;
	
	@NotNull
	private LoginFailureReason loginFailureReason;
	
	@NotNull
	private UserDto user;

	// LoginAttemptDto -> LoginAttempt. Sirve para crear un nuevo LoginAttempt. No tiene idLoginAttempt
	// puesto que no se ha creado aún.
	//
	// LoginAttemptDto -> LoginAttemp. To create a new LoginAttempt. No idLoginAttempt since it has 
	// been created yet.
	public LoginAttemptDto(LocalDateTime timestamp, Boolean successful, String ipAddress,
			LoginFailureReason loginFailureReason, UserDto user) {
		this.timestamp = timestamp;
		this.successful = successful;
		this.ipAddress = ipAddress;
		this.loginFailureReason = loginFailureReason;
		this.user = user;
	}
	
	// LoginAttempt -> LoginAttemptDto. Sirve para enviar información sobre LoginAttempt al front-end.
	//
	// LoginAttempt -> LoginAttemptDto. To send LoginAttempt info to the front-end.
	public LoginAttemptDto(Long idLoginAttempt, LocalDateTime timestamp, Boolean successful, String ipAddress,
			LoginFailureReason loginFailureReason, UserDto user) {
		this.idLoginAttempt = idLoginAttempt;
		this.timestamp = timestamp;
		this.successful = successful;
		this.ipAddress = ipAddress;
		this.loginFailureReason = loginFailureReason;
		this.user = user;
	}

	public LoginAttemptDto() {
	}

	public Long getIdLoginAttempt() {
		return idLoginAttempt;
	}

	public void setIdLoginAttempt(Long idLoginAttempt) {
		this.idLoginAttempt = idLoginAttempt;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Boolean getSuccessful() {
		return successful;
	}

	public void setSuccessful(Boolean successful) {
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

	public UserDto getUser() {
		return user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}
	
}
