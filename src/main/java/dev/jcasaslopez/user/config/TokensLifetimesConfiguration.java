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

@Configuration
public class TokensLifetimesConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(TokensLifetimesConfiguration.class);
	
	@Value("${jwt.lifetimes.verifyEmailToken}") int verifyEmailToken;
    @Value("${jwt.lifetimes.passwordResetToken}") int passwordResetToken;
    @Value("${jwt.lifetimes.accessToken}") int accessToken;
    @Value("${jwt.lifetimes.refreshToken}") int refreshToken;
	
	private Map<TokenType, Integer> tokensLifetimes;

	@PostConstruct
    private void init() {
    // Se inicializa aquí porque los valores @Value se asignan después de la inyección de dependencias
    //
    // It is initialized here because the @Value values are assigned after dependency injection.
		tokensLifetimes = Map.of(
			    TokenType.VERIFY_EMAIL, verifyEmailToken,
			    TokenType.PASSWORD_RESET, passwordResetToken,
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
