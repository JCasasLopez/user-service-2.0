package dev.jcasaslopez.user.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.event.CreateAccountEvent;

@Service
public class RegistrationServiceImpl implements RegistrationService {
	
	private UserDetailsManager userDetailsManager;
	private ObjectMapper objectMapper;
	private TokenService tokenService;
    private StringRedisTemplate redisTemplate;
	private ApplicationEventPublisher eventPublisher;
	
	public RegistrationServiceImpl(UserDetailsManager userDetailsManager, ObjectMapper objectMapper,
			TokenService tokenService, StringRedisTemplate redisTemplate, ApplicationEventPublisher eventPublisher) {
		this.userDetailsManager = userDetailsManager;
		this.objectMapper = objectMapper;
		this.tokenService = tokenService;
		this.redisTemplate = redisTemplate;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void createAccount(String token) throws JsonMappingException, JsonProcessingException {
		String userJson = redisTemplate.opsForValue().get(tokenService.getJtiFromToken(token));
	    UserDto user = objectMapper.readValue(userJson, UserDto.class);
	    eventPublisher.publishEvent(new CreateAccountEvent(user));
	    // Los atributos ya se han validado con la llamada al endpoint "initiateRegistration".
	 	//
	 	// The attributes have already been validated during the call to the "initiateRegistration" endpoint.
		userDetailsManager.createUser((UserDetails) user);
	}

}
