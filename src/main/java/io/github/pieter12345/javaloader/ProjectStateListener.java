package io.github.pieter12345.javaloader;

import io.github.pieter12345.javaloader.JavaProject.LoadException;
import io.github.pieter12345.javaloader.JavaProject.UnloadException;

/**
 * ProjectStateListener interface.
 * @author P.J.S. Kools
 */
public interface ProjectStateListener {
	
	/**
	 * onLoad method.
	 * This method is called when a project is loaded.
	 * @param project - The JavaProject that loaded.
	 * @throws LoadException When an exception occurred invoking a method on the project instance.
	 */
	public void onLoad(JavaProject project) throws LoadException;
	
	/**
	 * onUnload method.
	 * This method is called when a project is unloaded.
	 * @param project - The JavaProject that unloaded.
	 * @throws UnloadException When an exception occurred invoking a method on the project instance.
	 */
	public void onUnload(JavaProject project) throws UnloadException;
}
