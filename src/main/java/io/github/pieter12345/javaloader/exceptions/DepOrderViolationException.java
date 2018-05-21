package io.github.pieter12345.javaloader.exceptions;

import io.github.pieter12345.javaloader.JavaProject;

/**
 * This exception can be thrown when a load, unload or compile operation performed on a JavaProject violates the
 * dependency order (e.g. a project is loaded before a project it depends on or a dependency is unloaded before a
 * project that depends on it).
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class DepOrderViolationException extends JavaProjectException {
	
	public DepOrderViolationException(JavaProject project) {
		super(project);
	}
	
	public DepOrderViolationException(JavaProject project, String message) {
		super(project, message);
	}
	
	public DepOrderViolationException(JavaProject project, String message, Throwable cause) {
		super(project, message, cause);
	}
	
	public DepOrderViolationException(JavaProject project, Throwable cause) {
		super(project, cause);
	}
}
