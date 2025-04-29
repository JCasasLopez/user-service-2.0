package dev.jcasaslopez.user.utilities;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.TokenServiceImpl;

@Component
public class TestHelper {
	
	@Autowired private UserRepository userRepository;	
	@Autowired private UserMapper userMapper;
	@Autowired private TokenServiceImpl tokenServiceImpl;	
	
	public User createUser() {
		// El usuario solo tiene el rol "USER", que se asigna automáticamente.
		//
		// User only has the role "USER", which is assigned automatically.
		User plainUser = new User ("Yorch123", "Jorge22!", "Jorge García", "jorgecasas22@hotmail.com", 
				LocalDate.of(1978, 11, 26));
		userRepository.save(plainUser);
		return plainUser;
	}
	
	public String loginUser(User userJpa) {
		CustomUserDetails user = userMapper.userToCustomUserDetailsMapper(userJpa);
    	Authentication authentication = new UsernamePasswordAuthenticationToken
										(user, user.getPassword(), user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return tokenServiceImpl.createAuthToken(TokenType.ACCESS);
	}
}