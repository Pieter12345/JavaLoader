package io.github.pieter12345.javaloader.exceptions;

import io.github.pieter12345.javaloader.JavaProject;

/**
 * This exception can be thrown when a load operation performed on a JavaProject
 * has an unexpected result.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class LoadException extends JavaProjectException {
	
	public LoadException(JavaProject project) {
		super(project);
	}
	
	public LoadException(JavaProject project, String message) {
		super(project, message);
	}
	
	public LoadException(JavaProject project, String message, Throwable cause) {
		super(project, message, cause);
	}
	
	public LoadException(JavaProject project, Throwable cause) {
		super(project, cause);
	}
}
