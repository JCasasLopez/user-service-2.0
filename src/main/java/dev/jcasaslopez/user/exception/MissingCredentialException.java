package dev.jcasaslopez.user.exception;

import org.springframework.security.core.AuthenticationException;

// By extending AuthenticationException, we can handle it in the AuthenticationFailureHandler.
public class MissingCredentialException extends AuthenticationException {
	public MissingCredentialException(String msg) {
        super(msg);
    }
}