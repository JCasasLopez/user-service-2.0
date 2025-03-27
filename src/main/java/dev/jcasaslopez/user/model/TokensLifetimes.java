package dev.jcasaslopez.user.model;

import java.util.Map;

import dev.jcasaslopez.user.enums.TokenType;

public class TokensLifetimes {
	
	private Map<TokenType, Integer> tokensLifetimes;

	public TokensLifetimes(Map<TokenType, Integer> tokensLifetimes) {
		this.tokensLifetimes = tokensLifetimes;
	}

	public Map<TokenType, Integer> getTokensLifetimes() {
		return tokensLifetimes;
	}

}
