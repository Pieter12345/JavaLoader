package io.github.pieter12345.javaloader;

import java.io.File;

/**
 * Represents a file (or directory) based dependency for a JavaLoader project.
 * @author P.J.S. Kools
 */
public interface FileDependency extends Dependency {
	
	/**
	 * Gets the File of the dependency.
	 * @return The File of the dependency or null if no file location could be determined (in indirect dependencies).
	 */
	File getFile();
	
}
