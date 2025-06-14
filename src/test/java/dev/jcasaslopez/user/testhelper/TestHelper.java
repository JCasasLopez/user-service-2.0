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
	
	public User createUser(String username, String password) {
		
		// Prepares the table 'roles' in the DB by persisting all possible roles.
		Role userRole = new Role(RoleName.ROLE_USER);
		roleRepository.save(userRole);
		Role adminRole = new Role(RoleName.ROLE_ADMIN);
		roleRepository.save(adminRole);
		Role superAdminRole = new Role(RoleName.ROLE_SUPERADMIN);
		roleRepository.save(superAdminRole);
		
		User user = new User (	username, 
								password, 
								"Jorge García", 
								"jorgecasas22@hotmail.com", 
								LocalDate.of(1978, 11, 26)
							  );
		
		// It would be less cumbersome to use UserDetailsManagerImpl.createUser(), but this method 
		// does not return the user.
		Set<Role> roles = new HashSet<>();
		roles.add(userRole);
		
		user.setRoles(roles);
		user.setAccountStatus(AccountStatus.ACTIVE);
		user.setPassword(passwordEncoder.encode("Jorge22!"));	
		
		userRepository.save(user);
		userRepository.flush();
		return user;
	}
	
	@SuppressWarnings("deprecation")
	public void cleanDataBaseAndRedis() {
		loginAttemptRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
	}
	
	public String loginUser(User userJpa, TokenType tokenType) {
		CustomUserDetails user = userMapper.userToCustomUserDetailsMapper(userJpa);
    	Authentication authentication = new UsernamePasswordAuthenticationToken
										(user, user.getPassword(), user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return tokenService.createAuthToken(tokenType);
	}
}