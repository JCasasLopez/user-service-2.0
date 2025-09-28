package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.exception.MissingCredentialException;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.AccountLockingService;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.service.UserAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
	int accountLockDuration;
	
	private final UserAccountService userAccountService;
	private final StandardResponseHandler standardResponseHandler;
	private final LoginAttemptService loginAttemptService;
	private final AccountLockingService accountLockingService;

	public CustomAuthenticationFailureHandler(UserAccountService userAccountService,
			StandardResponseHandler standardResponseHandler, LoginAttemptService loginAttemptService,
			AccountLockingService accountLockingService) {
		this.userAccountService = userAccountService;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
		this.accountLockingService = accountLockingService;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,AuthenticationException exception) throws IOException, ServletException {
		
	    String username = request.getParameter("username");
	    
	    // We handle missing credentials here instead of in the AuthenticationEntryPoint, 
	    // which would be their natural place (i.e., where Spring Security would route them by default),
	    // so that all login failure cases are grouped together in a single class.
	    if (exception instanceof MissingCredentialException) {
			loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.MISSING_FIELD, null);
	        logger.warn("Username or password missing during authentication");
	        standardResponseHandler.handleResponse(response, 400, "Username and password are required", null);
	        return;
	        
	    } else if (exception instanceof UsernameNotFoundException) {
	    	loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.USER_NOT_FOUND, null);
            logger.warn("Failed login attempt - User not found");
            
            // 401 with a neutral message to avoid revealing whether the failure was due to  
            // username or password.
            standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);
            return;
	    }
	      
	    // We load the user here and not earlier, for two reasons:
	    // 1. We only reach this point if the username is valid and the user exists.
	    // 2. If we called userAccountService.findUser() earlier and the user didn't exist, it would 
	    // throw an exception outside this flow, preventing it from being properly handled here.	    
        User user = userAccountService.findUser(username);
        int accountLockDurationInHours = accountLockDuration/3600 >= 1 ? accountLockDuration/3600 : 1;
	    
	    if (exception instanceof LockedException) {
	        loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.ACCOUNT_LOCKED, user);
	        
	        if(user.getAccountStatus() == AccountStatus.BLOCKED) {
		        standardResponseHandler.handleResponse(response, 403, "Your account has been locked by an administrator. Please contact support if you believe this is a mistake", null);
		        
	        } else if (user.getAccountStatus() == AccountStatus.TEMPORARILY_BLOCKED) {
		        standardResponseHandler.handleResponse(response, 403, "Your account is locked due to too many failed login attempts. It will be reactivated automatically in a few hours", null);
	        }
	        return;
	        
	    } else if (exception instanceof BadCredentialsException) {
	    	
	    	int failedLoginAttempts = accountLockingService.getLoginAttemptsRedisEntry(username);
	    	failedLoginAttempts++;
    		accountLockingService.setLoginAttemptsRedisEntry(username, failedLoginAttempts, accountLockDuration);
    		logger.warn("Number of failed login attempts for user {}: {}", username, failedLoginAttempts);
	    	
	    	if (failedLoginAttempts < maxNumberFailedAttempts) {
	    		// "Bad credentials" instead of "Incorrect password" for the reasons discussed above.
	    		standardResponseHandler.handleResponse(response, 401, "Bad credentials", null);	
	    		
	    	// Redundant, but improves legibility.
	    	} else if (failedLoginAttempts >= maxNumberFailedAttempts) {
	    		accountLockingService.blockAccount(user);
	    		logger.warn("Account blocked for user {} due to too many login failed attempts", username);
	    		standardResponseHandler.handleResponse(response, 403, "Your account has been locked due to too many failed "
	    				+ "login attempts. It will be reactivated automatically in " + accountLockDurationInHours + " hours", null);	
	    	}

	    	loginAttemptService.recordAttempt(false, request.getRemoteAddr(), LoginFailureReason.INCORRECT_PASSWORD,user);
	    }
	}
}