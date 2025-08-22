package dev.jcasaslopez.user.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.NotificationType;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.event.NotifyingEvent;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.model.TokensLifetimes;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AccountOrchestrationServiceImpl implements AccountOrchestrationService {
	
	private static final Logger logger = LoggerFactory.getLogger(AccountOrchestrationServiceImpl.class);
	
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

	// A Redis entry is added to temporarily store the user's data needed in the next step—after email verification—
	// for the actual account creation. The key is the token's JTI, and the value is the user data serialized as JSON.
	// In the next step, we retrieve the user's data using the token received via email.
	@Override
	public void initiateRegistration(UserDto user) throws JsonProcessingException {
		String username = user.getUsername();
		String verifyEmailToken = tokenService.createVerificationToken(username);
		String tokenJti = tokenService.getJtiFromToken(verifyEmailToken);
	
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;
		int expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFICATION) * 60;
		
		// We encode the password BEFORE saving all the info in Redis.
		user.setPassword(passwordEncoder.encode(user.getPassword())); 
		String userJson = objectMapper.writeValueAsString(user);
		logger.debug("Redis key: {}. Password encoded before trying to upload to Redis ", redisKey);
		redisTemplate.opsForValue().set(redisKey, userJson, expirationInSeconds, TimeUnit.SECONDS);
		logger.info("Redis entry uploaded. RedisKey:{}", redisKey);
		
		NotifyingEvent verifyEmailEvent = new NotifyingEvent(userMapper.userDtoToUserMapper(user), 
				verifyEmailToken, NotificationType.VERIFY_EMAIL);
		eventPublisher.publishEvent(verifyEmailEvent);
		logger.debug("Verify email event published for user: {}", user.getEmail());
	}
	
	@Override
	@Transactional
	public void userRegistration(HttpServletRequest request) throws JsonMappingException, JsonProcessingException {
		
		// We have set the token as an attribute in AuthenticationFilter.
		String token = (String) request.getAttribute("token");
		
		// From the token, we can retrieve the corresponding Redis entry, whose value 
		// contains the user (as a string).
		String tokenJti = tokenService.getJtiFromToken(token);
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;
		String userJson = redisTemplate.opsForValue().get(redisKey);
		User user = objectMapper.readValue(userJson, User.class);
		logger.info("User {} obtained from Redis entry with Redis key {}", user.getUsername(), redisKey);
		 
	 	// Attributes already validated when calling "initiateRegistration" endpoint.
	    CustomUserDetails userAsCustomUserDetails = userMapper.userToCustomUserDetailsMapper(user);
		userDetailsManager.createUser(userAsCustomUserDetails);
		logger.info("User account persisted successfully for {}", user.getUsername());
		
		NotifyingEvent createAccountEvent = new NotifyingEvent(user, NotificationType.CREATE_ACCOUNT);
		eventPublisher.publishEvent(createAccountEvent);
	    logger.debug("Create account event published for user: {}", user.getUsername());
	}
	
	@Override
	@Transactional
	public void deleteAccount() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		userDetailsManager.deleteUser(username);
		logger.info("Account deleted successfully for user {}", username);;
	}
	
	@Override
	public void forgotPassword(String email) {
		UserDto user = userMapper.userToUserDtoMapper(userAccountService.findUserByEmail(email));
		String resetPasswordToken = tokenService.createVerificationToken(user.getUsername());
		logger.info("Verification token created for user {}", user.getUsername());
		
		NotifyingEvent forgotPasswordEvent = new NotifyingEvent(userMapper.userDtoToUserMapper(user), 
				resetPasswordToken, NotificationType.FORGOT_PASSWORD);
		eventPublisher.publishEvent(forgotPasswordEvent);
	    logger.debug("Forgot password event published for user: {}", user.getUsername());
	}
	
	@Override
	@Transactional
	public void resetPassword(String newPassword, HttpServletRequest request) {
	
		// We have set the token as an attribute in AuthenticationFilter.
		String token = (String) request.getAttribute("token");
		String username = tokenService.parseClaims(token).getSubject();
		User user = userAccountService.findUser(username);
		logger.info("User {} found in the database", username);
		
		logger.debug("Calling resetPassword() in Password Service...");
		passwordService.resetPassword(newPassword, user);
		
		NotifyingEvent resetPasswordEvent = new NotifyingEvent(user, NotificationType.RESET_PASSWORD);
		eventPublisher.publishEvent(resetPasswordEvent);
		logger.debug("Change password event published for user: {}", user.getUsername());
	}
	
	@Override
	@Transactional
	public void changePassword(String oldPassword, String newPassword) {
		logger.debug("Calling changePassword() in User Details Service...");
		userDetailsManager.changePassword(oldPassword, newPassword);
		
		// userDetailsManager.changePassword() could in theory return the user,
		// but since the UserDetailsManager contract enforces a void return type,
		// we must retrieve the authenticated user again from the SecurityContextHolder.
		User user = userAccountService.getAuthenticatedUser();
		
		NotifyingEvent changePasswordEvent = new NotifyingEvent(user, NotificationType.CHANGE_PASSWORD);
		eventPublisher.publishEvent(changePasswordEvent);
		logger.debug("Change password event published for user: {}", user.getUsername());
	}
	
	@Override
	@Transactional
	@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
	public void upgradeUser(String email) {
		logger.debug("Calling upgradeUser() in User Account Service...");
		userAccountService.upgradeUser(email);
	}
	
	@Override
	@Transactional
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPERADMIN')")
	public void updateAccountStatus(String email, AccountStatus newAccountStatus) {
		User user = userAccountService.findUserByEmail(email);
		
		logger.debug("Calling updateAccountStatus() in User Account Service...");
		userAccountService.updateAccountStatus(user, newAccountStatus);
		
		NotifyingEvent changeAccountStatusEvent = new NotifyingEvent(user, newAccountStatus, 
				NotificationType.UPDATE_ACCOUNT_STATUS);
		eventPublisher.publishEvent(changeAccountStatusEvent);
		logger.debug("UpdateAccountStatusEvent published for user: {}", user.getUsername());
	}
	
	@Override
	public void sendNotification(Map<String, String> messageAsMap) {
		logger.debug("Calling processMessageDetails() in Email Service...");
		emailService.processMessageDetails(messageAsMap);
	}
	
	@Override
	public List<String> refreshToken(){
		logger.debug("Creating refresh token...");
		String refreshToken = tokenService.createAuthToken(TokenType.REFRESH);
		logger.debug("Creating access token...");
		String accessToken = tokenService.createAuthToken(TokenType.ACCESS);
		return List.of(refreshToken, accessToken);
	}
}