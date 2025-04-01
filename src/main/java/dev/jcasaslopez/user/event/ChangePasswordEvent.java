package dev.jcasaslopez.user.event;

public class ChangePasswordEvent {
	
	private String email;
	private String username;
	
	public ChangePasswordEvent(String email, String username) {
		this.email = email;
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

}
