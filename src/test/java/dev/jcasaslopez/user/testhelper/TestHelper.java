package dev.jcasaslopez.user.testhelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;

@Component
public class TestHelper {
	
	@Autowired private UserMapper userMapper;
	@Autowired private TokenServiceImpl tokenService;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private ObjectMapper mapper;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private EmailService emailService;
	
    @Transactional
	public String returnUserAsJson(User user) throws JsonProcessingException {		
		// Jackson cannot serialize or deserialize java.time.LocalDate by default
		mapper.registerModule(new JavaTimeModule());

		return mapper.writeValueAsString(user);
	}
	
    // It would be more convenient to use UserDetailsManagerImpl.createUser(), but this method does not return the user
    public User createUser(String username, String password) {
    	User user = new User (username, password, "Jorge García", "jorgecasas22@hotmail.com", LocalDate.of(1978, 11, 26));

    	Set<Role> roles = new HashSet<>();
    	Role userRole = new Role (RoleName.ROLE_USER);
    	roles.add(userRole);

    	user.setRoles(roles);
    	user.setAccountStatus(AccountStatus.ACTIVE);
    	return user;
    }
    
    public User createAndPersistUser(String username, String password) {
    	String encodedPassword = passwordEncoder.encode(password);
    	User user = new User (username, encodedPassword, "Jorge García", "jorgecasas22@hotmail.com", LocalDate.of(1978, 11, 26));

    	Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER).get();
    	Set<Role> roles = new HashSet<>();
    	roles.add(userRole);

    	user.setRoles(roles);
    	user.setAccountStatus(AccountStatus.ACTIVE);
    	
    	userRepository.save(user);
    	userRepository.flush();
    	return user;
    }
    
    public User createAndPersistUser(String username, String password, RoleName roleName) {
    	String encodedPassword = passwordEncoder.encode(password);
    	User user = new User (username, encodedPassword, "Jorge García", "jorgecasas22@hotmail.com", LocalDate.of(1978, 11, 26));

    	Role userRole = roleRepository.findByRoleName(roleName).get();
    	Set<Role> roles = new HashSet<>();
    	roles.add(userRole);

    	user.setRoles(roles);
    	user.setAccountStatus(AccountStatus.ACTIVE);
    	
    	userRepository.save(user);
    	userRepository.flush();
    	return user;
    }
	
	public void cleanDataBaseAndRedis() {
		loginAttemptRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
	}
	
	public String buildRedisKey(String token, String RedisConstant) {
		String tokenJti = tokenService.getJtiFromToken(token);
		return RedisConstant + tokenJti;
	}
	
	// Captures the email body sent by the EmailService mock and extracts the token using regex pattern matching.
	public String extractTokenFromEmail() {
		// Capture the email body that was sent to the mock EmailService
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

		// Verify that sendEmail was called and capture the arguments. We only care about the body content
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());

		String emailBody = bodyCaptor.getValue();

		// JWT token pattern: three base64url-encoded segments separated by dots. Pattern: "token=header.payload.signature"
		Pattern pattern = Pattern.compile("token=([\\w-]+\\.[\\w-]+\\.[\\w-]+)");
		Matcher matcher = pattern.matcher(emailBody);

		// Verify the token was actually included in the email. This is a precondition for the test to continue validly.
		assertTrue(matcher.find(), "Token not found in email body");

		// Extract and return the JWT token (group 1 captures the token without "token=" prefix).
		String token =  matcher.group(1);
		return token;
	}
}