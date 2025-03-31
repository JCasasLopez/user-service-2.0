package dev.jcasaslopez.user.event;

public class ResetPasswordEvent {
	
	private String newPassword;
	private String token;
	
	public ResetPasswordEvent(String newPassword, String token) {
		this.newPassword = newPassword;
		this.token = token;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public String getToken() {
		return token;
	}

}
