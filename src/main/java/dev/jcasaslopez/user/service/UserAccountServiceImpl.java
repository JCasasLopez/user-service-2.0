package dev.jcasaslopez.user.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class UserAccountServiceImpl implements UserAccountService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);
	
	private UserRepository userRepository;

	public UserAccountServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public User findUser(String username) {
		if (username == null || username.trim().isEmpty()) {
			logger.warn("Invalid username received: '{}'", username); 
		    throw new IllegalArgumentException("Username cannot be null or empty");
		}
		
		Optional<User> userOptional = userRepository.findByUsername(username);
		if (userOptional.isEmpty()) {
		    throw new UsernameNotFoundException("User " + username + " not found in the database");
		}
		
		User foundUser = userOptional.get();
		logger.info("User {} retrieved from database successfully", username);
		return foundUser;
	}
	
	@Override
	public User findUserByEmail(String email) {
		Optional<User> optionalUser = userRepository.findByEmail(email);
		if(optionalUser.isEmpty()) {
			throw new UsernameNotFoundException("User not found in the database");
		}
		return optionalUser.get();
	}

	@Override
	@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
	public void upgradeUser(String email) {
		// Se asigna ROLE_USER por defecto; si hay otro rol, tiene que ser ROLE_ADMIN.
		// 
		// ROLE_USER is assigned by default; if there's another role, it must be ROLE_ADMIN.
		User user = findUserByEmail(email);
		if (user.getRoles().size() > 1) {
			logger.warn("User {} is already admin; upgrade user ignored.", user.getUsername());
			throw new IllegalArgumentException("User is already ADMIN");
		}
		// Validamos que exista el usuario.
		//
		// We validate there is such user.
		user.getRoles().add(new Role(RoleName.ROLE_ADMIN));
		userRepository.save(user);
		logger.info("User {} upgraded to ADMIN", user.getUsername());
	}
	
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public void updateAccountStatus(User user, AccountStatus newAccountStatus) {
		String username = user.getUsername();
		
		if (newAccountStatus == null) {
			logger.warn("Received null account status for user '{}'", username);
		    throw new IllegalArgumentException("Account status cannot be null");
		}
		
		if(user.getAccountStatus() == AccountStatus.PERMANENTLY_SUSPENDED) {
	        logger.info("User '{}' has a permanently suspended account; status change ignored", username);
			throw new AccountStatusException("Cannot change status: the account is permanently suspended");
		}
		
		if(user.getAccountStatus() == newAccountStatus) {
			logger.debug("No status change needed for user '{}': status already '{}'", 
                    username, newAccountStatus);
			throw new AccountStatusException("The account already has the specified status");
		}
		
		userRepository.updateAccountStatus(username, newAccountStatus);
		logger.info("Account status updated from {} to {} for user {} ", user.getAccountStatus(), 
				newAccountStatus, username);
	}
}
