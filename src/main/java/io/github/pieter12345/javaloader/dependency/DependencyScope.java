package io.github.pieter12345.javaloader.dependency;

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
	
	/**
	 * Returns the dependency scope for the given case-insensitive name.
	 * @param name - The name to get the dependency scope for.
	 * @return The dependency scope or null if the name did not match any.
	 */
	public static DependencyScope forName(String name) {
		for(DependencyScope scope : DependencyScope.values()) {
			if(scope.toString().equalsIgnoreCase(name)) {
				return scope;
			}
		}
		return null;
	}
}
