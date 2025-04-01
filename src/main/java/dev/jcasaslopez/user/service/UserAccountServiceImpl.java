package dev.jcasaslopez.user.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class UserAccountServiceImpl implements UserAccountService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);
	
	private UserRepository userRepository;
	private UserDetailsManager userDetailsManager;
	private ObjectMapper objectMapper;
	private TokenService tokenService;
    private StringRedisTemplate redisTemplate;
	private ApplicationEventPublisher eventPublisher;

	public UserAccountServiceImpl(UserRepository userRepository, UserDetailsManager userDetailsManager,
			ObjectMapper objectMapper, TokenService tokenService, StringRedisTemplate redisTemplate,
			ApplicationEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.userDetailsManager = userDetailsManager;
		this.objectMapper = objectMapper;
		this.tokenService = tokenService;
		this.redisTemplate = redisTemplate;
		this.eventPublisher = eventPublisher;
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
	public void updateAccountStatus(String email, AccountStatus newAccountStatus) {
		User foundUser = findUserByEmail(email);
		String username = foundUser.getUsername();
		
		if (newAccountStatus == null) {
			logger.warn("Received null account status for user '{}'", username);
		    throw new IllegalArgumentException("Account status cannot be null");
		}
		
		if(foundUser.getAccountStatus() == AccountStatus.PERMANENTLY_SUSPENDED) {
	        logger.info("User '{}' has a permanently suspended account; status change ignored", username);
			throw new AccountStatusException("Cannot change status: the account is permanently suspended");
		}
		
		if(foundUser.getAccountStatus() == newAccountStatus) {
			logger.debug("No status change needed for user '{}': status already '{}'", 
                    username, newAccountStatus);
			throw new AccountStatusException("The account already has the specified status");
		}
		
		userRepository.updateAccountStatus(username, newAccountStatus);
		logger.info("Account status updated from {} to {} for user {} ", foundUser.getAccountStatus(), 
				newAccountStatus, username);
		
		eventPublisher.publishEvent(new UpdateAccountStatusEvent(email, username,
				newAccountStatus.getDisplayName()));
		logger.debug("UpdateAccountStatusEvent published for user: {}", username);
	}
	
	@Override
	public void createAccount(String token) throws JsonMappingException, JsonProcessingException {
		String userJson = redisTemplate.opsForValue().get(tokenService.getJtiFromToken(token));
	    UserDto user = objectMapper.readValue(userJson, UserDto.class);
	    eventPublisher.publishEvent(new CreateAccountEvent(user));
	    // Los atributos ya se han validado con la llamada al endpoint "initiateRegistration".
	 	//
	 	// The attributes have already been validated during the call to the "initiateRegistration" endpoint.
		userDetailsManager.createUser((UserDetails) user);
	}
	
}
