package dev.jcasaslopez.user.security.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.exception.MissingCredentialException;
import dev.jcasaslopez.user.security.handler.CustomAuthenticationFailureHandler;
import dev.jcasaslopez.user.service.AccountLockingService;
import dev.jcasaslopez.user.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    private final UserAccountService userAccountService;
    private final AccountLockingService accountLockingService;
    
	public CustomUsernamePasswordAuthenticationFilter(UserAccountService userAccountService, AccountLockingService accountLockingService) {
		this.userAccountService = userAccountService;
		this.accountLockingService = accountLockingService;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
	        throws AuthenticationException {		
		String username = request.getParameter("username");
	    String password = request.getParameter("password");

	    if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
	        throw new MissingCredentialException("Username and password are required");
	    }
	    
		User user = userAccountService.findUser(username);
		
		// If there is no Redis entry for this user (getLoginAttemptsRedisEntry returns 0) and his account is blocked,
		// then the lock period has expired and the account can be automatically reactivated.
		if (user.getAccountStatus() == AccountStatus.TEMPORARILY_BLOCKED && 
						accountLockingService.getLoginAttemptsRedisEntry(username) == 0) {
			logger.info("User {} reactivated after lock expiration", username);
			accountLockingService.unBlockAccount(user);
		}
		return super.attemptAuthentication(request, response);
	}
	
}