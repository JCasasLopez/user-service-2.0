package dev.jcasaslopez.user.security.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.NotificationType;
import dev.jcasaslopez.user.event.NotifyingEvent;
import dev.jcasaslopez.user.exception.MissingCredentialException;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.handler.CustomAuthenticationFailureHandler;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    private final StringRedisTemplate redisTemplate;
    private final UserAccountService userAccountService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CustomUsernamePasswordAuthenticationFilter(
            StringRedisTemplate redisTemplate,
            UserAccountService userAccountService,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.userAccountService = userAccountService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }
    
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
	        throws AuthenticationException {		
		String username = request.getParameter("username");
	    String password = request.getParameter("password");

	    if (username == null || username.trim().isEmpty() ||
	        password == null || password.trim().isEmpty()) {
	        throw new MissingCredentialException("Username and password are required");
	    }
	    
	    // We need the username in the handlers to use it as the key in Redis,
	    // where we track failed authentication attempts.
		request.setAttribute("attemptedUsername", username);
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		User user = userAccountService.findUser(username);
		
		// The account was locked by an admin due to administrative or security reasons,
		// and only an admin can unblock it.
		if (user.getAccountStatus() == AccountStatus.BLOCKED) {
			logger.warn("User {} account remains locked", username);
			throw new LockedException("Account is locked");
		}

		// If there is no Redis entry for this user (and his account is blocked)
		// it means the lock period has expired and the account can be automatically reactivated.
		if (user.getAccountStatus() == AccountStatus.TEMPORARILY_BLOCKED && !redisTemplate.hasKey(redisKey)) {
			NotifyingEvent changeAccountStatusEvent = new NotifyingEvent(user, AccountStatus.ACTIVE,
					NotificationType.UPDATE_ACCOUNT_STATUS);
			eventPublisher.publishEvent(changeAccountStatusEvent);
			user.setAccountStatus(AccountStatus.ACTIVE);
			userRepository.save(user);
			logger.info("User {} reactivated after lock expiration", username);
		}
		return super.attemptAuthentication(request, response);
	}
}