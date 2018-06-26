package io.github.pieter12345.javaloader.dependency;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import io.github.pieter12345.javaloader.utils.Utils;

/**
 * Represents a .jar file dependency for a JavaLoader project.
 * @author P.J.S. Kools
 */
public class JarDependency implements FileDependency {

	private final File jarFile;
	private final DependencyScope scope;
	
	public JarDependency(File jarFile, DependencyScope scope) {
		Objects.requireNonNull(jarFile, "Jar file may not be null.");
		Objects.requireNonNull(scope, "Dependency scope may not be null.");
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
	
	@Override
	public boolean equals(Object obj) {
		try {
			return obj instanceof JarDependency
					&& ((JarDependency) obj).scope.equals(this.scope)
					&& (((JarDependency) obj).jarFile.getAbsolutePath().equals(this.jarFile.getAbsolutePath())
					|| ((JarDependency) obj).jarFile.getCanonicalFile().equals(this.jarFile.getCanonicalFile()));
		} catch (IOException e) {
			// There's not much we can do. Throw an exception so that something crashes, rather than works unexpectedly.
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString() {
		return this.getClass().getName()
				+ "{jarFile=\"" + this.jarFile.getAbsolutePath() + "\", scope=" + this.scope.toString() + "}";
	}
	
}
