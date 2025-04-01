package dev.jcasaslopez.user.event;

public class UpdateAccountStatusEvent {
	
	private String email;
	private String username;
	
	public UpdateAccountStatusEvent(String email, String username) {
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
