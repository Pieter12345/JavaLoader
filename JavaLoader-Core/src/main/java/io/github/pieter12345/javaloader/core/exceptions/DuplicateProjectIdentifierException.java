package io.github.pieter12345.javaloader.core.exceptions;

import java.io.File;
import java.util.Set;

/**
 * This exception can be thrown when multiple projects with the same name were found.
 * @author P.J.S. Kools
 */
@SuppressWarnings("serial")
public class DuplicateProjectIdentifierException extends Exception {
	
	private final String projectName;
	private final Set<File> projectDirs;
	
	public DuplicateProjectIdentifierException(String projectName, Set<File> projectDirs) {
		super();
		this.projectName = projectName;
		this.projectDirs = projectDirs;
	}
	
	public DuplicateProjectIdentifierException(String message, String projectName, Set<File> projectDirs) {
		super(message);
		this.projectName = projectName;
		this.projectDirs = projectDirs;
	}
	
	public DuplicateProjectIdentifierException(
			String message, Throwable cause, String projectName, Set<File> projectDirs) {
		super(message, cause);
		this.projectName = projectName;
		this.projectDirs = projectDirs;
	}
	
	public DuplicateProjectIdentifierException(Throwable cause, String projectName, Set<File> projectDirs) {
		super(cause);
		this.projectName = projectName;
		this.projectDirs = projectDirs;
	}
	
	/**
	 * Gets the name of the duplicate projects.
	 * @return The name.
	 */
	public String getProjectName() {
		return this.projectName;
	}
	
	/**
	 * Gets all project directories that define projects with the name returned by {@link #getProjectName()}.
	 * @return The project directories.
	 */
	public Set<File> getProjectDirs() {
		return this.projectDirs;
	}
}
