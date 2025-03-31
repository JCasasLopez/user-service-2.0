package dev.jcasaslopez.user.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class PasswordServiceImpl implements PasswordService {
	
	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsManagerImpl.class);
	// Requisitos: Al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un símbolo
	//
	// requirements: At least 8 characters, one capital letter, one lowercase letter, one number and one symbol.
	String PASSWORD_PATTERN =
		"^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$";
	Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

	private UserRepository userRepository;
	private UserMapper userMapper;
	private PasswordEncoder passwordEncoder;
	private TokenService tokenService;
	private ApplicationEventPublisher eventPublisher;
	private AccountService accountService;
	
	public PasswordServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
			TokenService tokenService, ApplicationEventPublisher eventPublisher, AccountService accountService) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.eventPublisher = eventPublisher;
		this.accountService = accountService;
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
		if (!passwordEncoder.matches(oldPassword, accountService.findUser(username).getPassword())) {
			logger.info("Provided old password does not match the one in the database");
			throw new IllegalArgumentException
				("The provided password does not match the one in the database");
		}
		userRepository.updatePassword(username, passwordEncoder.encode(newPassword));
		logger.info("Password updated successfully");
	}

	// Aunque resetPassword() y changePassword() son superficialmente similares,
	// este último opera con el usuario ya autenticado. Además, sus firmas son distintas:
	// resetPassword() no necesita verificar si la contraseña antigua coincide.
	//
	// While resetPassword() and changePassword() are superficially similar, the
	// latter operates with the user already authenticated. Also, their method signatures differ:
	// resetPassword() does not need to verify if the old password matches.
	@Override
	public void resetPassword(String newPassword, String token) {
		String username = tokenService.parseClaims(token).getSubject();
		Optional<User> optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isEmpty()) {
			throw new UsernameNotFoundException("User " + username + " not found in the database");
		}
		logger.debug("User {} found. Encoding new password...", username);
		User user = optionalUser.get();
		String encodedPassword = passwordEncoder.encode(newPassword);
		user.setPassword(encodedPassword);
		userRepository.save(user);
		logger.info("Password reset successfully");
		eventPublisher.publishEvent(new ResetPasswordEvent(userMapper.userToUserDtoMapper(user)));
		logger.debug("ResetPasswordEvent published for user: {}", username);
	}
	
	@Override
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
