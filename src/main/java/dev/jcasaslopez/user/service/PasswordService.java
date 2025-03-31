package dev.jcasaslopez.user.service;

public interface PasswordService {

	void changePassword(String oldPassword, String newPassword);

	// Aunque resetPassword() y changePassword() son superficialmente similares,
	// este último opera con el usuario ya autenticado. Además, sus firmas son distintas:
	// resetPassword() no necesita verificar si la contraseña antigua coincide.
	//
	// While resetPassword() and changePassword() are superficially similar, the
	// latter operates with the user already authenticated. Also, their method signatures differ:
	// resetPassword() does not need to verify if the old password matches.
	void resetPassword(String newPassword, String token);

	boolean passwordIsValid(String newPassword);

}