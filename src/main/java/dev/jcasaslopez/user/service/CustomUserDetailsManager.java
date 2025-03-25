package dev.jcasaslopez.user.service;

import org.springframework.security.provisioning.UserDetailsManager;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.enums.AccountStatus;

public interface CustomUserDetailsManager extends UserDetailsManager {

	public UserDto findUser(String username);
	
	// Asciende a un usuario con el rol ROLE_USER al rol ROLE_ADMIN.
	// Esta operación solo puede ser realizada por usuarios con el rol SUPER_ADMIN.
	//
	// Promotes a user with the ROLE_USER role to ROLE_ADMIN.
	// This action can only be performed by users with the SUPER_ADMIN role.
	public void upgradeUser(UserDto user);
	
	// Implementa la funcionalidad de "recuperación de contraseña olvidada".
	//
	// Implements the "forgot password" functionality.
	public void forgotPassword(String email);
	
	// Permite cambiar el estado de una cuenta, ya sea para suspenderla temporalmente o 
	// desactivarla de forma definitiva. Este método se utiliza en situaciones como problemas 
	// administrativos, detección de actividad sospechosa, etc.
	// 
	// Allows changing the status of an account, either by temporarily suspending it or 
	// permanently deactivating it. This method is typically used in cases such as administrative 
	// issues, suspicious activity, etc.
	public void changeAccountStatus(String username, AccountStatus accountStatus);

}
