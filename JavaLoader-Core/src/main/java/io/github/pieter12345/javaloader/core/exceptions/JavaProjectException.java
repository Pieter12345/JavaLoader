package io.github.pieter12345.javaloader.core.exceptions;

import io.github.pieter12345.javaloader.core.JavaProject;

/**
 * This exception or one of its more specific subclasses can be thrown when some operation performed on a JavaProject
 * has an unexpected result.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class JavaProjectException extends Exception {
	
	private final JavaProject project;
	
	public JavaProjectException(JavaProject project) {
		super();
		this.project = project;
	}
	
	public JavaProjectException(JavaProject project, String message) {
		super(message);
		this.project = project;
	}
	
	public JavaProjectException(JavaProject project, String message, Throwable cause) {
		super(message, cause);
		this.project = project;
	}
	
	public JavaProjectException(JavaProject project, Throwable cause) {
		super(cause);
		this.project = project;
	}
	
	public JavaProject getProject() {
		return this.project;
	}
}
