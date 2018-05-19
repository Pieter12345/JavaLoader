package io.github.pieter12345.javaloader.exceptions;

import io.github.pieter12345.javaloader.JavaProject;

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
