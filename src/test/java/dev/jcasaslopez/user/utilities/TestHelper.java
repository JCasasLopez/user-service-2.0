package dev.jcasaslopez.user.utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.TokenServiceImpl;

@Component
public class TestHelper {
	
	@Autowired UserMapper userMapper;
	@Autowired TokenServiceImpl tokenServiceImpl;
	
	public String logUserIn(User user) {
		CustomUserDetails userDetails = userMapper.userToCustomUserDetailsMapper(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return tokenServiceImpl.createAuthToken(TokenType.ACCESS);
	}
	
}