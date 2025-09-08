package dev.jcasaslopez.user.service;

import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.NotificationType;
import dev.jcasaslopez.user.event.NotifyingEvent;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.utilities.Constants;

@Service
public class AccountLockingServiceImpl implements AccountLockingService {
	
	private final StringRedisTemplate redisTemplate;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	
	public AccountLockingServiceImpl(StringRedisTemplate redisTemplate, UserRepository userRepository,
			ApplicationEventPublisher eventPublisher) {
		this.redisTemplate = redisTemplate;
		this.userRepository = userRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public int getLoginAttemptsRedisEntry(String username) {
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		if(!redisTemplate.hasKey(redisKey)) {
			return 0;
		}
		return Integer.parseInt(redisTemplate.opsForValue().get(redisKey));	
	}
	
	@Override
	public void deleteLoginAttemptsRedisEntry(String username) {
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.delete(redisKey);
	}

	@Override
	public void setLoginAttemptsRedisEntry(String username, int loginAttempts, int accountLockDurationInSeconds) {
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.opsForValue().set(redisKey, String.valueOf(loginAttempts), accountLockDurationInSeconds, TimeUnit.SECONDS);
	}

	@Override
	public void blockAccount(User user) {
		NotifyingEvent changeAccountStatusEvent = new NotifyingEvent(user, AccountStatus.TEMPORARILY_BLOCKED, NotificationType.UPDATE_ACCOUNT_STATUS);
		eventPublisher.publishEvent(changeAccountStatusEvent);
    	user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
        userRepository.save(user);
	}
	
	@Override
	public void unBlockAccount(User user) {
		NotifyingEvent changeAccountStatusEvent = new NotifyingEvent(user, AccountStatus.ACTIVE, NotificationType.UPDATE_ACCOUNT_STATUS);
		eventPublisher.publishEvent(changeAccountStatusEvent);
		user.setAccountStatus(AccountStatus.ACTIVE);
		userRepository.save(user);
	}
}
