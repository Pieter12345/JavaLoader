package io.github.pieter12345.javaloader;

/**
 * Provides dependency scope options.
 * @author P.J.S. Kools
 */
public enum DependencyScope {
	
	/**
	 * Dependencies with this tag should be included for compilation and in runtime of the parent application.
	 */
	INCLUDE,
	
	/**
	 * Dependencies with this tag should be included for compilation, but not in runtime of the parent application.
	 * This is used when a dependency is known to be available in the environment the parent application will run in.
	 */
	PROVIDED;
}
