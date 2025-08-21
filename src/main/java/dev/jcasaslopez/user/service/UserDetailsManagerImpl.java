package dev.jcasaslopez.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.mapper.UserMapper;

@Service
public class UserDetailsManagerImpl implements UserDetailsManager {
	
	private static final Logger logger = LoggerFactory.getLogger(UserDetailsManagerImpl.class);
	
	private UserMapper userMapper;
	private UserAccountService accountService;
	private PasswordService passwordService;
	
	public UserDetailsManagerImpl(UserMapper userMapper, UserAccountService accountService,
			PasswordService passwordService) {
		this.userMapper = userMapper;
		this.accountService = accountService;
		this.passwordService = passwordService;
	}
	
	// This class contains no business logic. 
	// All operations are delegated to UserAccountService in order to decouple 
	// Spring Security concerns from domain logic.

	@Override
	public void createUser(UserDetails user) {
		accountService.createUser(user);
	}

	// This method is required by UserDetailsManager interface but is intentionally not supported.
	@Override
	public void updateUser(UserDetails user) {
		logger.warn("User update attempted but is disabled by design.");
		throw new UnsupportedOperationException("User update is not allowed.");
	}

	@Override
	public void deleteUser(String username) {
		accountService.deleteUser(username);
	}

	@Override
	public boolean userExists(String username) {
		return accountService.userExists(username);
	}

	// Internal method used by Spring Security during the authentication process, should not be exposed directly to users.
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userMapper.userToCustomUserDetailsMapper(accountService.findUser(username));
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		passwordService.changePassword(oldPassword, newPassword);
	}
}