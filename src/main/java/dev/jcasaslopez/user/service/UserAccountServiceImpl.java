package dev.jcasaslopez.user.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;

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
		User user = optionalUser.get();
		logger.info("User {} retrieved from database successfully", user.getUsername());
		return user;
	}
	
	@Override
	public boolean userExists(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public void createUser(UserDetails user) {
		if (user instanceof CustomUserDetails) {
			// The password has already been encoded in AccountOrchestrationServiceImpl.initiateRegistration(),
			// when setting the 'user' object as the value in the Redis entry, so there's no need to do it again.
			CustomUserDetails customUser = (CustomUserDetails) user;
			User userJPA = customUser.getUser();

			// Assigns default role ROLE_USER as per business rules.
			Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
					.orElseThrow(() -> new IllegalStateException("Role ROLE_USER not found in database"));
			Set<Role> roles = new HashSet<>();
			roles.add(userRole);
			userJPA.setRoles(roles);

			// Assigns account status ACTIVE as per business rules.
			userJPA.setAccountStatus(AccountStatus.ACTIVE);
			
			userRepository.save(userJPA);
			logger.info("New user created with username: {}", userJPA.getUsername());
			
		} else {
			logger.warn("UserDetails implementation not supported: {}", user.getClass().getName());
			throw new IllegalArgumentException("Unsupported UserDetails implementation");
		}
	}

	@Override
	public void deleteUser(String username) {
		// Checks if the user is found in the database, but retrieving it from it is not necessary,
		// that is why the result is not assigned to any variable.
		findUser(username);
		
		// The log is recorded in AccountOrchestrationService, so we do not need to do it here again.
		userRepository.deleteByUsername(username);
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

	@Override
	public User getAuthenticatedUser() {
		Authentication authenticatedUser = SecurityContextHolder.getContext().getAuthentication();
		if(authenticatedUser == null) {
			throw new IllegalStateException("Security Context Holder not populated");
		}
		String username = authenticatedUser.getName();
		return findUser(username);
	}
	
}