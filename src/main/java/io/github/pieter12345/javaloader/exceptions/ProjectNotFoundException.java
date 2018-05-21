package io.github.pieter12345.javaloader.exceptions;

/**
 * This exception can be thrown when a project was not found.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class ProjectNotFoundException extends Exception {
	
	public ProjectNotFoundException() {
		super();
	}
	
	public ProjectNotFoundException(String message) {
		super(message);
	}
	
	public ProjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ProjectNotFoundException(Throwable cause) {
		super(cause);
	}
}

