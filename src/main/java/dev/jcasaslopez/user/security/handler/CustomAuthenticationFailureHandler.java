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
import dev.jcasaslopez.user.exception.MissingCredentialException;
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
		
		// Atributo establecido en CustomUsernamePasswordAuthenticationFilter.
		//
		// Attribute set by custom UsernamePasswordAuthenticationFilter
	    String username = (String) request.getAttribute("attemptedUsername");
	    
	    // Tratamos las credenciales faltantes aquí en lugar de AuthenticationEntryPoint, que es su 
	    // lugar natural, es decir, adonde te lleva el flujo de Spring Security por defecto,
	    // porque así tenemos todos los casos de fallo de login reunidos bajo una misma clase.
	    //
	    // We handle missing credentials here instead of in the AuthenticationEntryPoint, 
	    // which would be their natural place (i.e., where Spring Security would route them by default),
	    // so that all login failure cases are grouped together in a single class.
	    if (exception instanceof MissingCredentialException) {
			loginAttemptService.recordAttempt(false, request.getRemoteAddr(), 
					LoginFailureReason.MISSING_FIELD, null);
	        logger.warn("Username or password not found during auth failure");
	        standardResponseHandler.handleResponse(response, 400, "Username and password are required", null);
	        return;
	    }
	      
	    if (exception instanceof UsernameNotFoundException) {
	    	loginAttemptService.recordAttempt(false, request.getRemoteAddr(),
                    LoginFailureReason.USER_NOT_FOUND, null);
            logger.info("Failed login attempt - User not found");
            
            // Aunque el usuario no existe, devolvemos 401 con un mensaje neutro 
            // ("Bad credentials") para no revelar si el fallo fue por username o contraseña, 
            // evitando posibles ataques de enumeración.
            //
            // Even if the user does not exist, we return 401 with a neutral message 
            // ("Bad credentials") to avoid revealing whether the failure was due to username 
            // or password, preventing user enumeration attacks.
            standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
            return;
	    }
	    
	    User user;
        user = userAccountService.findUser(username);
	    
	    if (exception instanceof LockedException) {
	        // La cuenta ya estaba bloqueada: registramos ACCOUNT_LOCKED como causa.
	    	//
	        // The account was already locked: log ACCOUNT_LOCKED as the reason.
	        loginAttemptService.recordAttempt(false, request.getRemoteAddr(), 
	        		LoginFailureReason.ACCOUNT_LOCKED, user);
	        standardResponseHandler.handleResponse(response, 403, "Account is locked", null);
	        return;
	    }
	    
	    // Si el usuario ha proporcionado un username y este está en la base de datos, y la cuenta 
	    // está activa, entonces el problema es que la contraseña es incorrecta.
	    //
	    // If the user has provided a username that is in the database, and that account is active
	    // , then the authentication has failed because the password was incorrect.
	    String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
	    int failedAttempts=0;

	    if (!redisTemplate.hasKey(redisKey)) {
	        redisTemplate.opsForValue().set(redisKey, "1", accountLockedDuration, TimeUnit.SECONDS);
	        logger.info("First failed login attempt for user {}", username);
	        
	    } else {
	        failedAttempts = Integer.parseInt(redisTemplate.opsForValue().get(redisKey)) + 1;
	        redisTemplate.opsForValue().set(redisKey, String.valueOf(failedAttempts),
                    accountLockedDuration, TimeUnit.SECONDS);
	        
	        if (failedAttempts >= maxNumberFailedAttempts) {
	        	// Si se ha superado el número máximo de intentos fallidos, se bloquea la cuenta.
	        	//
	        	// If the maximum number of failed attempts is exceeded, the account is locked.
	        	eventPublisher.publishEvent(new UpdateAccountStatusEvent
            			(user, AccountStatus.TEMPORARILY_BLOCKED));
	        	user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
	            userRepository.save(user);
	            logger.warn("User {} account blocked due to too many failed attempts", username);
	        } else {
	            logger.info("Failed login attempt {} for user {}", failedAttempts, username);
	        }
	    }
	    
	    // Usamos "Bad Credentials" en vez de "Incorrect password" por la razón ya mencionada 
	    // más arriba (prevenir ataques de enumeración).
	    //
	    // We use "Bad credentials" instead of "Incorrect password" for the reason mentioned above 
	    // (to prevent enumeration attacks).
	    if(failedAttempts >= maxNumberFailedAttempts) {
	        standardResponseHandler.handleResponse(response, 401, "Bad credentials. Your account has been "
	        		+ "locked due to too many attempts", null);
	    } else {
	        standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
	    }
	    
		loginAttemptService.recordAttempt(false, request.getRemoteAddr(), 
				LoginFailureReason.INCORRECT_PASSWORD, user);
	}
}