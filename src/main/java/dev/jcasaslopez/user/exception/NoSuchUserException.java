package dev.jcasaslopez.user.exception;

public class NoSuchUserException extends RuntimeException {
	public NoSuchUserException(String message) {
		super(message);
	}
}