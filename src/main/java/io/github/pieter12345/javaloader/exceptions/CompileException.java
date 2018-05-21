package io.github.pieter12345.javaloader.exceptions;

import io.github.pieter12345.javaloader.JavaProject;

/**
 * This exception can be thrown when a compile operation performed on a JavaProject
 * has an unexpected result.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class CompileException extends JavaProjectException {
	
	public CompileException(JavaProject project) {
		super(project);
	}
	
	public CompileException(JavaProject project, String message) {
		super(project, message);
	}
	
	public CompileException(JavaProject project, String message, Throwable cause) {
		super(project, message, cause);
	}
	
	public CompileException(JavaProject project, Throwable cause) {
		super(project, cause);
	}
}
