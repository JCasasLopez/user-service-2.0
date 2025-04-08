package dev.jcasaslopez.user.service;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;

@Service
public class PasswordServiceImpl implements PasswordService {
	
	private static final Logger logger = LoggerFactory.getLogger(PasswordServiceImpl.class);
	// Requisitos: Al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un símbolo
	//
	// requirements: At least 8 characters, one capital letter, one lowercase letter, one number and one symbol.
	String PASSWORD_PATTERN =
		"^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$";
	Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private UserAccountService userAccountService;
	
	public PasswordServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
			UserAccountService userAccountService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userAccountService = userAccountService;
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
		User user = userAccountService.findUser(username);
		logger.info("User {} found in the database", username);
	
		boolean oldPasswordMatchProvidedOne = passwordEncoder.matches(oldPassword, user.getPassword());
		if (!oldPasswordMatchProvidedOne) {
			logger.info("Provided old password does not match the one in the database");
			throw new IllegalArgumentException
				("The provided password does not match the one in the database");
		}
		
		boolean newPasswordSameAsOldOne = passwordEncoder.matches(newPassword, user.getPassword());
		if(newPasswordSameAsOldOne) {
			throw new IllegalArgumentException("The new password has to be different from the old one");
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
	public void resetPassword(String newPassword, User user) {
		passwordIsValid(newPassword);
		
		boolean newPasswordSameAsOldOne = passwordEncoder.matches(newPassword, user.getPassword());
		if(newPasswordSameAsOldOne) {
			throw new IllegalArgumentException("The new password has to be different from the old one");
		}
		
		userRepository.updatePassword(user.getUsername(), passwordEncoder.encode(newPassword));
		logger.info("Password updated successfully");
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
