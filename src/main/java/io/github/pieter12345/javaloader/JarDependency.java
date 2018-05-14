package io.github.pieter12345.javaloader;

import java.io.File;
import java.net.URL;

import io.github.pieter12345.javaloader.utils.Utils;

/**
 * Represents a .jar file dependency for a JavaLoader project.
 * @author P.J.S. Kools
 */
public class JarDependency implements FileDependency {

	private final File jarFile;
	private final DependencyScope scope;
	
	public JarDependency(File jarFile, DependencyScope scope) {
		if(jarFile == null) {
			throw new NullPointerException("jarFile may not be null.");
		}
		if(scope == null) {
			throw new NullPointerException("scope may not be null.");
		}
		this.jarFile = jarFile;
		this.scope = scope;
	}
	
	@Override
	public DependencyScope getScope() {
		return this.scope;
	}
	
	@Override
	public URL getURL() {
		return Utils.fileToURL(this.jarFile);
	}
	
	@Override
	public File getFile() {
		return this.jarFile;
	}
	
}
