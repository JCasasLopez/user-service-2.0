package dev.jcasaslopez.user.event;

public class UpdateAccountStatusEvent {
	
	private String email;
	private String username;
	private String newAccountStatus;
	
	public UpdateAccountStatusEvent(String email, String username, String newAccountStatus) {
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

	public String getNewAccountStatus() {
		return newAccountStatus;
	}

}
