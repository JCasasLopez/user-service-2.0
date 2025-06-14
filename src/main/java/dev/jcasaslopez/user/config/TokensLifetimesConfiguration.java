package dev.jcasaslopez.user.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.model.TokensLifetimes;
import jakarta.annotation.PostConstruct;

// This class reads the expiration time values for the different token types from application.properties
// and converts them into a Map, where the key is the token type (as defined in the TokenType enum)
// and the value is the expiration time in minutes.
// In this way, the class can be injected into others, making it easy to retrieve these time values.
@Configuration
public class TokensLifetimesConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(TokensLifetimesConfiguration.class);
	
    @Value("${jwt.lifetimes.verificationToken}") 
    private int verificationToken;
    @Value("${jwt.lifetimes.accessToken}") 
    private int accessToken;
    @Value("${jwt.lifetimes.refreshToken}") 
    private int refreshToken;
	
	private Map<TokenType, Integer> tokensLifetimes;

	@PostConstruct
    private void init() {
   
    // It is initialized here because the @Value values are assigned after dependency injection.
		tokensLifetimes = Map.of(
			    TokenType.VERIFICATION, verificationToken,
			    TokenType.ACCESS, accessToken,
			    TokenType.REFRESH, refreshToken
			);
		
        logger.info("Tokens lifetimes initialized: {}", tokensLifetimes);
    }
	
	@Bean
	TokensLifetimes tokensLifetimes() {
		return new TokensLifetimes(tokensLifetimes);
	}
	
}
