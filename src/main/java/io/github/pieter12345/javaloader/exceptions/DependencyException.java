package io.github.pieter12345.javaloader.exceptions;

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
