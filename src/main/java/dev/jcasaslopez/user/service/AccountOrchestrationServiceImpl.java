package dev.jcasaslopez.user.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.event.ChangePasswordEvent;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.model.TokensLifetimes;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AccountOrchestrationServiceImpl implements AccountOrchestrationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
	
	private UserDetailsManager userDetailsManager;
	private TokenService tokenService;
	private ApplicationEventPublisher eventPublisher;
	private StringRedisTemplate redisTemplate;
	private TokensLifetimes tokensLifetimes;
	private ObjectMapper objectMapper;
	private PasswordEncoder passwordEncoder;
	private UserMapper userMapper;
	private UserAccountService userAccountService;
	private PasswordService passwordService;
	private EmailService emailService;
	
	public AccountOrchestrationServiceImpl(UserDetailsManager userDetailsManager, TokenService tokenService,
			ApplicationEventPublisher eventPublisher, StringRedisTemplate redisTemplate,
			TokensLifetimes tokensLifetimes, ObjectMapper objectMapper, PasswordEncoder passwordEncoder,
			UserMapper userMapper, UserAccountService userAccountService, PasswordService passwordService,
			EmailService emailService) {
		this.userDetailsManager = userDetailsManager;
		this.tokenService = tokenService;
		this.eventPublisher = eventPublisher;
		this.redisTemplate = redisTemplate;
		this.tokensLifetimes = tokensLifetimes;
		this.objectMapper = objectMapper;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.userAccountService = userAccountService;
		this.passwordService = passwordService;
		this.emailService = emailService;
	}

	@Override
	public void initiateRegistration(UserDto user) throws JsonProcessingException {
		String verifyEmailToken = tokenService.createVerificationToken();
		String tokenJti = tokenService.getJtiFromToken(verifyEmailToken);
		logger.info("Verification token created with jti {}", tokenJti);
		eventPublisher.publishEvent(new VerifyEmailEvent(user, verifyEmailToken));
		logger.debug("Verify email event published for user: {}", user.getEmail());

		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;
		int expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFICATION) * 60;
		
		// Codificamos la contraseña ANTES de subir todo a Redis.
		//
		// We encode the password BEFORE saving all the info in Redis.
		user.setPassword(passwordEncoder.encode(user.getPassword())); 
		String userJson = objectMapper.writeValueAsString(user);
		
		// Establecemos la cadena userJson como valor de la entrada de Redis, porque vamos 
		// a necesitarlo en el siguiente paso (creación de la cuenta como tal).
		//
		// We set the string userJson as the Redis entry value, since we will need it in 
		// next step (creating the account).
		redisTemplate.opsForValue().set(redisKey, userJson, expirationInSeconds, TimeUnit.SECONDS);
		logger.info("Redis entry uploaded. RedisKey:{}", redisKey);
	}
	
	@Override
	public void userRegistration(HttpServletRequest request) throws JsonMappingException, JsonProcessingException {
		// Hemos establecido el token como stributo en AuthenticationFilter.
		//
		// We have set the token as an attribute in AuthenticationFilter.
		String token = (String) request.getAttribute("token");
		
		// A partir del token, podemos obtener la entrada de Redis correspondiente, cuyo valor 
		// contiene el usuario (como cadena).
		//
		// From the token, we can retrieve the corresponding Redis entry, whose value 
		// contains the user (as a string).
		String tokenJti = tokenService.getJtiFromToken(token);
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;
		String userJson = redisTemplate.opsForValue().get(redisKey);
		UserDto user = objectMapper.readValue(userJson, UserDto.class);
		logger.info("User {} obtained from Redis entry with Redis key {}", user.getUsername(), redisKey);
		
	    eventPublisher.publishEvent(new CreateAccountEvent(user));
	    logger.debug("Create account event published for user: {}", user.getUsername());
	    
	    // Los atributos ya se habían validado con la llamada al endpoint "initiateRegistration".
	 	//
	 	// Attributes already validated when calling "initiateRegistration" endpoint.
		userDetailsManager.createUser((UserDetails) user);
		logger.info("User account persisted successfully for {}", user.getUsername());
	}
	
	@Override
	@PreAuthorize("#username == authentication.principal.username")
	public void deleteAccount() {
		// Endpoint solo accesible a usuarios autenticados, así que Security Context tiene que 
		// estar poblado.
		//
		// This enspoint is only accesible to authenticated user, so the Security Context has to be 
		// populated.
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		userDetailsManager.deleteUser(username);
		logger.info("Account deleted successfully for user {}", username);;
	}
	
	@Override
	public void forgotPassword(String email) {
		UserDto user = userMapper.userToUserDtoMapper(userAccountService.findUserByEmail(email));
		String resetPasswordToken = tokenService.createVerificationToken();
		logger.info("Verification token created for user {}", user.getUsername());
		
		eventPublisher.publishEvent(new ForgotPasswordEvent(user, resetPasswordToken));
	    logger.debug("Forgot password event published for user: {}", user.getUsername());
	}
	
	@Override
	public void resetPassword(String newPassword, HttpServletRequest request) {
		// Hemos establecido el token como stributo en AuthenticationFilter.
		//
		// We have set the token as an attribute in AuthenticationFilter.
		String token = (String) request.getAttribute("token");
		String username = tokenService.parseClaims(token).getSubject();
		User user = userAccountService.findUser(username);
		logger.info("User {} found in the database", username);
		
		logger.debug("Calling resetPassword() in Password Service...");
		passwordService.resetPassword(newPassword, user);
		
		eventPublisher.publishEvent(new ResetPasswordEvent(user));
		logger.debug("Change password event published for user: {}", user.getUsername());
	}
	
	@Override
	@PreAuthorize("#username == authentication.principal.username")
	public void changePassword(String newPassword, String oldPassword) {
		logger.debug("Calling changePassword() in User Details Service...");
		userDetailsManager.changePassword(oldPassword, newPassword);
		
		Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
		String username = currentUser.getName();
		User user = userAccountService.findUser(username);
		
		eventPublisher.publishEvent(new ChangePasswordEvent(user));
		logger.debug("Change password event published for user: {}", user.getUsername());
	}
	
	@Override
	@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
	public void upgradeUser(String email) {
		logger.debug("Calling upgradeUser() in User Account Service...");
		userAccountService.upgradeUser(email);
	}
	
	@Override
	@PreAuthorize("hasRole(' ADMIN')")
	public void updateAccountStatus(String email, AccountStatus newAccountStatus) {
		User user = userAccountService.findUserByEmail(email);
		String username = user.getUsername();
		logger.info("User {} found in the database", username);
		
		logger.debug("Calling updateAccountStatus() in User Account Service...");
		userAccountService.updateAccountStatus(user, newAccountStatus);
		
		eventPublisher.publishEvent(new UpdateAccountStatusEvent(user, newAccountStatus));
		logger.debug("UpdateAccountStatusEvent published for user: {}", username);
	}
	
	@Override
	@PreAuthorize("#username == authentication.principal.username")
	public void sendNotification(Map<String, String> messageAsMap) {
		logger.debug("Calling processMessageDetails() in Email Service...");
		emailService.processMessageDetails(messageAsMap);
	}
	
	@Override
	@PreAuthorize("#username == authentication.principal.username")
	public List<String> refreshToken(){
		logger.debug("Creating access token...");
		String accessToken = tokenService.createAuthToken(TokenType.ACCESS);
		logger.debug("Creating refresh token...");
		String refreshToken = tokenService.createAuthToken(TokenType.REFRESH);
		
		String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenService.getJtiFromToken(refreshToken);
		int expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.REFRESH) * 60;		
		redisTemplate.opsForValue().set(redisKey, TokenType.REFRESH.prefix(), expirationInSeconds, 
				TimeUnit.SECONDS);
		logger.info("Refresh token uploaded in Redis with key {}", redisKey);
		
		return List.of(accessToken, refreshToken);
	}
}