package dev.jcasaslopez.user.service;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class UserAccountServiceImpl implements UserAccountService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);
	
	private UserRepository userRepository;
	private RoleRepository roleRepository;

	public UserAccountServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
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
	public void upgradeUser(String email) {
		User user = findUserByEmail(email);
		if (user.getRoles().contains(roleRepository.findByRoleName(RoleName.ROLE_ADMIN).get())) {
			logger.warn("User {} is already admin; upgrade user ignored.", user.getUsername());
			throw new IllegalArgumentException("User is already ADMIN");
		}
		
		Set<Role> roles = user.getRoles();
		Role userRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
			    .orElseThrow(() -> new IllegalStateException("Role ROLE_ADMIN not found in database"));
		roles.add(userRole);
		user.setRoles(roles);
		userRepository.save(user);
		logger.info("User {} upgraded to ADMIN", user.getUsername());
	}
	
	@Override
	public void updateAccountStatus(User user, AccountStatus newAccountStatus) {
		String username = user.getUsername();
		
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
