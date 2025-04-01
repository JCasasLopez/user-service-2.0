package dev.jcasaslopez.user.service;

public interface PasswordService {

	void changePassword(String oldPassword, String newPassword);
	void resetPassword(String newPassword, String token);
	boolean passwordIsValid(String newPassword);

}