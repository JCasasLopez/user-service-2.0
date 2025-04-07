package dev.jcasaslopez.user.security.handler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);
	
	@Value ("${auth.maxFailedAttempts}")
	int maxNumberFailedAttempts;
	
	@Value ("${security.auth.account-lock-duration-seconds}")
	int accountLockedDuration;
	
	private StringRedisTemplate redisTemplate;
	private UserAccountService userAccountService;
	private UserRepository userRepository;
	private StandardResponseHandler standardResponseHandler;
	private LoginAttemptService loginAttemptService;
	private ApplicationEventPublisher eventPublisher;

	public CustomAuthenticationFailureHandler(StringRedisTemplate redisTemplate, UserAccountService userAccountService,
			UserRepository userRepository, StandardResponseHandler standardResponseHandler,
			LoginAttemptService loginAttemptService, ApplicationEventPublisher eventPublisher) {
		this.redisTemplate = redisTemplate;
		this.userAccountService = userAccountService;
		this.userRepository = userRepository;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
	        AuthenticationException exception) throws IOException, ServletException {
		// Establecido en en la implementación de UsernamePasswordAuthenticationFilter.
		//
		// Set by custom UsernamePasswordAuthenticationFilter
	    String username = (String) request.getAttribute("attemptedUsername");
	    if (username == null || username.trim().isEmpty()) {
			loginAttemptService.recordAttempt(true, request.getRemoteAddr(), LoginFailureReason.MISSING_FIELD);
	        logger.warn("Username not found in request attributes during auth failure");
	        standardResponseHandler.handleResponse(response, 400, "Username not provided", null);
	        return;
	    }
	    
	    if (exception instanceof LockedException) {
	        // La cuenta ya estaba bloqueada: registramos ACCOUNT_LOCKED como causa.
	    	//
	        // The account was already locked: log ACCOUNT_LOCKED as the reason.
	        loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.ACCOUNT_LOCKED);
	        standardResponseHandler.handleResponse(response, 403, "Account is locked", null);
	        return;
	    }

	    User user;
	    try {
	        user = userAccountService.findUser(username);
            
	    } catch (UsernameNotFoundException ex) {
        	// Si findUser() lanza una excepción, se ha usado un username equivocado para la autenticación.
            //
        	// If findUser() throws an exception, an incorrect username was used for authentication.
			loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.USER_NOT_FOUND);
	        logger.warn("Username not found in the database");
	        standardResponseHandler.handleResponse(response, 400, "Wrong username", null);
            return;
        }

	    // Si el usuario ha proporcionado un username y este está en la base de datos, 
	    // y la cuenta está activa, entonces el problema es que la contraseña es incorrecta.
	    //
	    // If the user has provided a username that is in the database, and that account 
	    // is active, then the authentication has failed because the password was incorrect.
	    String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;

	    if (!redisTemplate.hasKey(redisKey)) {
	        redisTemplate.opsForValue().set(redisKey, "1", accountLockedDuration, TimeUnit.SECONDS);
	        logger.info("First failed login attempt for user {}", username);
	        
	    } else {
	        int failedAttempts = Integer.parseInt(redisTemplate.opsForValue().get(redisKey)) + 1;
	        
	        if (failedAttempts >= maxNumberFailedAttempts) {
	        	// Si se ha superado el número máximo de intentos fallidos, se bloquea la cuenta.
	        	//
	        	// If the maximum number of failed attempts is exceeded, the account is locked.
	        	eventPublisher.publishEvent(new UpdateAccountStatusEvent
            			(user.getEmail(), username, AccountStatus.TEMPORARILY_BLOCKED));
	        	user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
	            userRepository.save(user);
	            logger.warn("User {} account blocked due to too many failed attempts", username);
	        } else {
	            redisTemplate.opsForValue().set(redisKey, String.valueOf(failedAttempts),
	                    accountLockedDuration, TimeUnit.SECONDS);
	            logger.info("Failed login attempt {} for user {}", failedAttempts, username);
	        }
	    }
		loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.INCORRECT_PASSWORD);
	}
}