package dev.jcasaslopez.user.service;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;

@Service
public class UserDetailsManagerImpl implements UserDetailsManager {
	
	private static final Logger logger = LoggerFactory.getLogger(UserDetailsManagerImpl.class);
	
	private UserRepository userRepository;
	private UserMapper userMapper;
	private UserAccountService accountService;
	private PasswordService passwordService;
	private RoleRepository roleRepository;
	
	public UserDetailsManagerImpl(UserRepository userRepository, UserMapper userMapper, 
			UserAccountService accountService, PasswordService passwordService, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.accountService = accountService;
		this.passwordService = passwordService;
		this.roleRepository = roleRepository;
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
			
			// Ya se ha codificado la contraseña en AccountOrchestrationServiceImpl.initiateRegistration(),
			// al establecer el objeto 'user' como valor de la entrada de Redis, así que no 
			// hace falta hacerlo otra vez.
			//
			// The password has already been encoded in AccountOrchestrationServiceImpl.initiateRegistration(),
			// when setting the 'user' object as the value in the Redis entry, 
			// so there's no need to do it again.
			CustomUserDetails customUser = (CustomUserDetails) user;
			User userJPA = customUser.getUser();
	
			// Según las reglas de negocio, se establece el role USER por defecto.
			//
			// Assign default role ROLE_USER as per business rules.
			Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
				    .orElseThrow(() -> new IllegalStateException("Role ROLE_USER not found in database"));
			Set<Role> roles = new HashSet<>();
			roles.add(userRole);
			userJPA.setRoles(roles);
			
			userJPA.setAccountStatus(AccountStatus.ACTIVE);
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
	public void deleteUser(String username) {
		accountService.findUser(username);
		userRepository.deleteByUsername(username);
		logger.info("User {} deleted", username);
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
		return userMapper.userToCustomUserDetailsMapper(accountService.findUser(username));
	}

	// Este método se define aquí porque forma parte de la interfaz UserDetailsManager, que  
	// Spring Security requiere. Sin embargo, la lógica real del cambio de contraseña se 
	// delega a PasswordService para mantener la separación de responsabilidades y facilitar 
	// las pruebas y el mantenimiento.
	//
	// This method is defined here because it is part of the UserDetailsManager interface, 
	// which is required by Spring Security. However, the actual password change logic is 
	// delegated to PasswordService to preserve separation of concerns 
	// and improve testability and maintainability.
	@Override
	public void changePassword(String oldPassword, String newPassword) {
		passwordService.changePassword(oldPassword, newPassword);
	}
}
