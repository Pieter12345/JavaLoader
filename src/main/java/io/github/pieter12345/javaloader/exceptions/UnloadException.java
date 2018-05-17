package io.github.pieter12345.javaloader.exceptions;

import io.github.pieter12345.javaloader.JavaProject;

@SuppressWarnings("serial")
public class UnloadException extends JavaProjectException {
	
	public UnloadException(JavaProject project) {
		super(project);
	}
	
	public UnloadException(JavaProject project, String message) {
		super(project, message);
	}
	
	public UnloadException(JavaProject project, String message, Throwable cause) {
		super(project, message, cause);
	}
	
	public UnloadException(JavaProject project, Throwable cause) {
		super(project, cause);
	}
}
