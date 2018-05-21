package io.github.pieter12345.javaloader.exceptions;

/**
 * This exception can be thrown when a dependency could not be found/loaded due to for example its descriptor being
 * invalid.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class DependencyException extends Exception {
	
	public DependencyException() {
		super();
	}
	
	public DependencyException(String message) {
		super(message);
	}
	
	public DependencyException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DependencyException(Throwable cause) {
		super(cause);
	}
}
