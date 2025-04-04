package dev.jcasaslopez.user.security.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.handler.CustomAuthenticationFailureHandler;
import dev.jcasaslopez.user.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    private StringRedisTemplate redisTemplate;
	private UserAccountService userAccountService;
    private UserRepository userRepository;
    
	public CustomUsernamePasswordAuthenticationFilter(StringRedisTemplate redisTemplate,
			UserAccountService userAccountService, UserRepository userRepository) {
		this.redisTemplate = redisTemplate;
		this.userAccountService = userAccountService;
		this.userRepository = userRepository;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
	        throws AuthenticationException {

	    String username = obtainUsername(request);
	    
	    // Necesitamos el username en los handlers para usarlo como clave en Redis,
	    // donde llevamos el control de los intentos fallidos de autenticación.
	    //
	    // We need the username in the handlers to use it as the key in Redis,
	    // where we track failed authentication attempts.
	    request.setAttribute("attemptedUsername", username);
	    
	    if (username == null || username.trim().isEmpty()) {
	        logger.warn("Empty username received in login request");
	        
	    } else {
	        String redisKey = "login_attempts:" + username;

	        // Si no existe una entrada en Redis para este usuario, significa que ha expirado
			// el periodo de bloqueo y la cuenta puede ser reactivada automáticamente.
			//
			// If there is no Redis entry for this user, it means the lock period has expired
			// and the account can be automatically reactivated.
	        if (!redisTemplate.hasKey(redisKey)) {
	            try {
	                User user = userAccountService.findUser(username);
	                
	                if (user.getAccountStatus() == AccountStatus.TEMPORARILY_BLOCKED) {
	                    user.setAccountStatus(AccountStatus.ACTIVE);
	                    userRepository.save(user);
	                    logger.info("User {} reactivated after lock expiration", username);
	                }
	                
	            } catch (UsernameNotFoundException e) {
	                logger.warn("User {} not found during attempted reactivation", username);
	            }
	        }
	    }
	    return super.attemptAuthentication(request, response);
	}
}
