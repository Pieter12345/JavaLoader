package io.github.pieter12345.javaloader.core;

import java.io.File;

/**
 * JavaLoaderProject class.
 * This class represents a JavaLoader project.
 * A JavaLoader project should have a main class which extends directly or indirectly from this class.
 * @author P.J.S. Kools
 */
public abstract class JavaLoaderProject {
	
	// Variables & Constants.
	private JavaProject project = null;
	
	/**
	 * initialize method.
	 * This method should never be called from a JavaLoader project.
	 * @param project
	 */
	public final void initialize(JavaProject project) {
		if(this.project != null) {
			throw new RuntimeException("Initialize method may not be called more than once.");
		}
		this.project = project;
	}
	
	/**
	 * getName method.
	 * @return The name of the project.
	 */
	public final String getName() {
		return this.project.getName();
	}
	
	/**
	 * getProjectFolder method.
	 * @return The directory containing the projects sourcecode.
	 */
	public final File getProjectFolder() {
		return this.project.getProjectDir();
	}
	
	/**
	 * Gets the {@link ProjectManager} containing this project.
	 * @return The {@link ProjectManager}.
	 */
	public final ProjectManager getProjectManager() {
		return this.project.getProjectManager();
	}
	
	/**
	 * isLoaded method.
	 * @return {@code true} if the project is loaded, {@code false} otherwise.
	 */
	public final boolean isLoaded() {
		return this.project.isLoaded();
	}
	
	/**
	 * onLoad method.
	 * This method is called when the project is loaded.
	 */
	public void onLoad() {
	}
	
	/**
	 * onUnload method.
	 * This method is called when the project is unloaded.
	 */
	public void onUnload() {
	}
	
	// Abstract methods.
	
	/**
	 * getVersion method.
	 * @return The version. Example: "0.0.1-SNAPSHOT".
	 */
	public abstract String getVersion();
}
