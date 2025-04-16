package dev.jcasaslopez.user.security.handler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
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

// Manejador personalizado de errores de autenticación que centraliza todos los fallos de login.
// Distingue entre: Credenciales faltantes - Username incorrecto - Cuenta bloqueada - Contraseña incorrecta.
// Las respuestas son neutras para no revelar información sensible al cliente.
// Registra todos los intentos con su causa mediante LoginAttemptService.
// Integra bloqueo de cuenta tras varios intentos fallidos usando Redis y eventos.
// Evita el uso de AuthenticationEntryPoint para username incorrecto y campos vacíos 
// mediante excepciones personalizadas.
//
// Custom authentication failure handler that centralizes all login failure cases.
// Distinguishes between: Missing credentials - Invalid username - Locked account - Incorrect password.
// Responses are neutral to avoid exposing sensitive information to the client.
// All attempts are logged with their cause via LoginAttemptService.
// Integrates account locking after multiple failed attempts using Redis and events.
// Avoids using AuthenticationEntryPoint for invalid usernames and missing fields
// by leveraging custom exceptions.
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
	        
	    } else if (exception instanceof UsernameNotFoundException) {
	    	loginAttemptService.recordAttempt(false, request.getRemoteAddr(),
                    LoginFailureReason.USER_NOT_FOUND, null);
            logger.info("Failed login attempt - User not found");
            
            // 401 con un mensaje neutro para no revelar si el fallo fue por username o contraseña, 
            //
            // 401 with a neutral message to avoid revealing whether the failure was due to  
            // username or password.
            standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
            return;
	    }
	      
	    // Cargamos el usuario aquí y no antes, por dos razones:
	    // 1. Solo llegamos a este punto si el username es válido y el usuario existe.
	    // 2. Si llamáramos a userAccountService.findUser() antes, y el usuario no existiera, se
	    // lanzaría una excepción fuera de este flujo, impidiendo manejarla correctamente aquí.
	    //
	    // We load the user here and not earlier, for two reasons:
	    // 1. We only reach this point if the username is valid and the user exists.
	    // 2. If we called userAccountService.findUser() earlier and the user didn't exist, it would 
	    // throw an exception outside this flow, preventing it from being properly handled here.
	    User user;
        user = userAccountService.findUser(username);
	    
	    if (exception instanceof LockedException) {
	        loginAttemptService.recordAttempt(false, request.getRemoteAddr(), 
	        		LoginFailureReason.ACCOUNT_LOCKED, user);
	        standardResponseHandler.handleResponse(response, 403, "Account is locked", null);
	        return;
	        
	    } else if (exception instanceof BadCredentialsException) {
	    	// "Bad Credentials" en vez de "Incorrect password" por la razón ya mencionada
	    	//
	    	// "Bad credentials" instead of "Incorrect password" for the reason mentioned above.
	    	String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		    int failedAttempts=0;
		    
		    if (!redisTemplate.hasKey(redisKey)) {
		        redisTemplate.opsForValue().set(redisKey, "1", accountLockedDuration, TimeUnit.SECONDS);
		        logger.info("First failed login attempt for user {}", username);
		        standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
		        
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
		            standardResponseHandler.handleResponse(response, 401,
							"Bad credentials. Your account has been locked due to too many attempts", null);
		            
		        } else {
					logger.info("Failed login attempt {} for user {}", failedAttempts, username);
					standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
				}
		    }
			loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.INCORRECT_PASSWORD,
					user);
		}
	}
}