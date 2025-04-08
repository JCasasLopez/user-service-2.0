package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;

public interface UserAccountService {

	User findUser(String username);

	User findUserByEmail(String email);

	// Asciende a un usuario con el rol ROLE_USER al rol ROLE_ADMIN.
	// Esta operación solo puede ser realizada por usuarios con el rol SUPER_ADMIN.
	//
	// Promotes a user with the ROLE_USER role to ROLE_ADMIN.
	// This action can only be performed by users with the SUPER_ADMIN role.
	void upgradeUser(String email);

	// Permite cambiar el estado de una cuenta, ya sea para suspenderla temporalmente o 
	// desactivarla de forma definitiva. Este método se utiliza en situaciones como problemas 
	// administrativos, detección de actividad sospechosa, etc.
	// 
	// Allows changing the status of an account, either by temporarily suspending it or 
	// permanently deactivating it. This method is typically used in cases such as administrative 
	// issues, suspicious activity, etc.
	void updateAccountStatus(String email, AccountStatus accountStatus);

}