package dev.jcasaslopez.user.event;

public class VerifyEmailEvent {
	
	private final String username;
	private final String email;
	private final String token;
	
	public VerifyEmailEvent(String username, String email, String token) {
		this.username = username;
		this.email = email;
		this.token = token;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getToken() {
		return token;
	}
	
}
