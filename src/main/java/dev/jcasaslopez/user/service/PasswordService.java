package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.entity.User;

public interface PasswordService {

	void changePassword(String oldPassword, String newPassword);
	void resetPassword(String newPassword, User user);
	boolean passwordIsValid(String newPassword);

}