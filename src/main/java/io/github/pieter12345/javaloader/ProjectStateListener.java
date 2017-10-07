package io.github.pieter12345.javaloader;

/**
 * ProjectStateListener interface.
 * @author P.J.S. Kools
 */
public interface ProjectStateListener {
	
	/**
	 * onLoad method.
	 * This method is called when a project is loaded.
	 * @param project - The JavaProject that loaded.
	 */
	public void onLoad(JavaProject project);
	
	/**
	 * onUnload method.
	 * This method is called when a project is unloaded.
	 * @param project - The JavaProject that unloaded.
	 */
	public void onUnload(JavaProject project);
}
