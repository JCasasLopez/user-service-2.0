package dev.jcasaslopez.user.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
	
	private UserRepository userRepository;
	private UserMapper userMapper;
	
	public AccountServiceImpl(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
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
	
	@Override
	public UserDto findUserByEmail(String email) {
		Optional<User> optionalUser = userRepository.findByEmail(email);
		if(optionalUser.isEmpty()) {
			throw new UsernameNotFoundException("User not found in the database");
		}
		return userMapper.userToUserDtoMapper(optionalUser.get());
	}

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
	
}
