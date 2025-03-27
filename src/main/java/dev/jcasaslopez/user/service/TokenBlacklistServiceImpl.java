package dev.jcasaslopez.user.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

	private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
	public void blacklistToken(String jti, long expirationInSeconds) {
        logger.info("Blacklisting token with jti: {} for {} seconds", jti, expirationInSeconds);
        redisTemplate.opsForValue().set(jti, "blacklisted", expirationInSeconds, TimeUnit.SECONDS);
    }

    @Override
	public boolean isTokenBlacklisted(String jti) {
        boolean isBlacklisted = redisTemplate.hasKey(jti);
        logger.debug("Checking blacklist status for jti {}: {}", jti, isBlacklisted);
        return isBlacklisted;
    }
}