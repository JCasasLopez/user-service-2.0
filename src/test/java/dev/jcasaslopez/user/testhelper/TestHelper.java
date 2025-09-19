package dev.jcasaslopez.user.testhelper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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
import dev.jcasaslopez.user.service.TokenService;

@Component
public class TestHelper {
	
	@Autowired private UserMapper userMapper;
	@Autowired private TokenService tokenService;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ObjectMapper mapper;
	
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
	
	@SuppressWarnings("deprecation")
	public void cleanDataBaseAndRedis() {
		loginAttemptRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
	}
	
	public String loginUser(User userJpa, TokenType tokenType) {
		CustomUserDetails user = userMapper.userToCustomUserDetailsMapper(userJpa);
		String username = user.getUsername();
		Authentication authentication = new UsernamePasswordAuthenticationToken (user, user.getPassword(), user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return tokenService.createAuthToken(tokenType, username);
	}
}