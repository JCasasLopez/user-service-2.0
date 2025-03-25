package dev.jcasaslopez.user.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;

@Service
public class CustomUserDetailsManagerImpl implements CustomUserDetailsManager {
	
	// Requisitos: Al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un símbolo
	//
	// requirements: At least 8 characters, one capital letter, one lowercase letter, one number and one symbol.
	String PASSWORD_PATTERN =
			"^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$";
	Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsManagerImpl.class);
	
	private UserRepository userRepository;
	private UserMapper userMapper;
	private PasswordEncoder passwordEncoder;
	
	public CustomUserDetailsManagerImpl(UserRepository userRepository, UserMapper userMapper,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	// Las excepciones por violaciones de restricciones de base de datos (como valores duplicados)
	// no se capturan aquí. Se propagan hasta el controlador, donde son manejadas por el 
	// GlobalExceptionHandler.
	//
	// Exceptions caused by database constraint violations (e.g., duplicate values) 
	// are not caught here. They propagate to the controller level, where they are 
	// handled by the GlobalExceptionHandler.
	@Override
	public void createUser(UserDetails user) {
		if (user instanceof CustomUserDetails) {
			CustomUserDetails customUser = (CustomUserDetails) user;
			User userJPA = customUser.getUser();
			userJPA.setPassword(passwordEncoder.encode(userJPA.getPassword()));
			
			// Según las reglas de negocio, se establece el role USER por defecto.
			//
			// Assign default role ROLE_USER as per business rules.
			userJPA.getRoles().add(new Role(RoleName.ROLE_USER));
			userRepository.save(userJPA);
		    logger.info("New user created with username: {}", userJPA.getUsername());
		} else {
		    logger.warn("UserDetails implementation not supported: {}", user.getClass().getName());
		    throw new IllegalArgumentException("Unsupported UserDetails implementation");
		}
	}

	// Este método es requerido por la interfaz UserDetailsManager, pero su uso está 
	// deshabilitado por diseño.
	//
	// This method is required by the UserDetailsManager interface but is intentionally not supported.
	@Override
	public void updateUser(UserDetails user) {
		logger.warn("User update attempted but is disabled by design.");
		throw new UnsupportedOperationException("User update is not allowed.");
	}

	@Override
	@PreAuthorize("#username == authentication.principal.username")
	public void deleteUser(String username) {
		findUser(username);
		userRepository.deleteByUsername(username);
		logger.info("User {} deleted", username);
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		passwordIsValid(newPassword);
		
		// Obtenemos el objeto user de Security Context, sabemos tiene que estar en ahí porque 
		// este método sólo es accesible para usuarios autenticados.
		// 
		// We retrieve the user object from the Security Context. We know it must be there because 
		// this method is only accessible to authenticated users.
		Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
		String username = currentUser.getName();
		logger.info("User {} found in Security Context", username);
		if (!passwordEncoder.matches(oldPassword, findUser(username).getPassword())) {
			logger.info("Provided old password does not match the one in the database");
			throw new IllegalArgumentException
				("The provided password does not match the one in the database");
		}
		userRepository.updatePassword(username, passwordEncoder.encode(newPassword));
		logger.info("Password updated successfully");
	}

	@Override
	public boolean userExists(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	// Método interno usado por Spring Security durante el proceso de autenticación
	// No debe exponerse directamente a los usuarios.
	//
	// Internal method used by Spring Security during the authentication process
	// Should not be exposed directly to users.
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		logger.debug("Attempting to load user details for username: {}", username);
		return userMapper.userDtoToCustomUserDetailsMapper(findUser(username));
	}

	@Override
	public UserDto findUser(String username) {
		if (username == null || username.trim().isEmpty()) {
			logger.warn("Invalid username received: '{}'", username); 
		    throw new IllegalArgumentException("Username cannot be null or empty");
		}
		
		Optional<User> userOptional = userRepository.findByUsername(username);
		if (userOptional.isEmpty()) {
		    throw new UsernameNotFoundException("User " + username + " not found in the database");
		}
		
		UserDto foundUser = userMapper.userToUserDtoMapper(userOptional.get());
		logger.info("User {} retrieved from database successfully", username);
		return foundUser;
	}

	// Este método, solo accesible para usuarios con ROLE_SUPERADMIN, añade el rol 
	// ROLE_ADMIN a los roles de un usuario.
	//
	// This method, accessible only to users with ROLE_SUPERADMIN, adds the ROLE_ADMIN 
	// role to a user's existing roles.
	@Override
	@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
	public void upgradeUser(UserDto user) {
		// Se asigna ROLE_USER por defecto; si hay otro rol, tiene que ser ROLE_ADMIN.
		// 
		// ROLE_USER is assigned by default; if there's another role, it must be ROLE_ADMIN.
		if (user.getRoles().size() > 1) {
			logger.warn("User {} is already admin; upgrade user ignored.", user.getUsername());
			throw new IllegalArgumentException("User is already ADMIN");
		}
		// Validamos que exista el usuario.
		//
		// We validate there is such user.
		findUser(user.getUsername());
		User userJPA = userRepository.findByUsername(user.getUsername()).get();
		userJPA.getRoles().add(new Role(RoleName.ROLE_ADMIN));
		userRepository.save(userJPA);
		logger.info("User {} upgraded to ADMIN", user.getUsername());
	}

	@Override
	public void forgotPassword(String email) {
		// ****************************************************************
		// A implementar cuando el sistema de gestión de tokens esté listo.
		//
		// To be implemented when the tokens manager is ready.
		// ****************************************************************
		throw new UnsupportedOperationException("Method not yet implemented");
	}

	@Override
	public void updateAccountStatus(String username, AccountStatus accountStatus) {
		if (accountStatus == null) {
			logger.warn("Received null account status for user '{}'", username);
		    throw new IllegalArgumentException("Account status cannot be null");
		}
		
		UserDto foundUser = findUser(username);
		if(foundUser.getAccountStatus() == AccountStatus.PERMANENTLY_SUSPENDED) {
	        logger.info("User '{}' has a permanently suspended account; status change ignored", username);
			throw new AccountStatusException("Cannot change status: the account is permanently suspended");
		}
		
		if(foundUser.getAccountStatus() == accountStatus) {
			logger.debug("No status change needed for user '{}': status already '{}'", 
                    username, accountStatus);
			throw new AccountStatusException("The account already has the specified status");
		}
		
		userRepository.updateAccountStatus(username, accountStatus);
		logger.info("Account status updated from {} to {} for user {} ", foundUser.getAccountStatus(), 
				accountStatus, username);
	}
	
	public boolean passwordIsValid(String newPassword) {
		boolean isValid = pattern.matcher(newPassword).matches();
        if(!isValid) {
        	logger.warn("The provided password does not meet the requirements");
        	throw new IllegalArgumentException("The provided password does not meet the requirements");
        }
        logger.debug("The provided password meets the requirements");
        return isValid;
	}
}
