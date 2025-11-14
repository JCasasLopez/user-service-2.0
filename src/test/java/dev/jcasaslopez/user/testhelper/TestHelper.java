package dev.jcasaslopez.user.testhelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;

@Component
public class TestHelper {
	
	@Autowired private TokenServiceImpl tokenService;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private ObjectMapper mapper;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private EmailService emailService;
	
	public User createUser(UserTestBuilder builder) {
        User user = builder.build();

		Set<Role> roles = new HashSet<>();
		roles = builder.getRoleNames()
				.stream()
				.map(roleName -> new Role(roleName))
				.collect(Collectors.toSet());
        user.setRoles(roles);
        return user;
    }
	
	public User createAndPersistUser(UserTestBuilder builder) {
        User user = builder.build();

		Set<Role> roles = new HashSet<>();
		roles = builder.getRoleNames()
				.stream()
				.map(roleName -> roleRepository.findByRoleName(roleName).get())
				.collect(Collectors.toSet());
        user.setPassword(passwordEncoder.encode(user.getPassword())); 
        user.setRoles(roles);
        
        userRepository.save(user);
    	userRepository.flush();
        return user;
    }
	
    @Transactional
	public String returnUserAsJson(User user) throws JsonProcessingException {		
		// Jackson cannot serialize or deserialize java.time.LocalDate by default
		mapper.registerModule(new JavaTimeModule());

		return mapper.writeValueAsString(user);
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