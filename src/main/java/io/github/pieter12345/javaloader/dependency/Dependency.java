package io.github.pieter12345.javaloader.dependency;

import java.net.URL;

/**
 * Represents a dependency for a JavaLoader project.
 * @author P.J.S. Kools
 */
public interface Dependency {
	
	/**
	 * Gets the scope of this dependency (include dependency or provided by execution environment).
	 * @return The scope of this dependency.
	 */
	DependencyScope getScope();
	
	/**
	 * Gets the Unified Resource Locator of the dependency.
	 * @return The URL of the dependency or null if no resource location could be determined (in indirect dependencies).
	 */
	URL getURL();
	
}
