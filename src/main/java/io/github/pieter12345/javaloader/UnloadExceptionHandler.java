package io.github.pieter12345.javaloader;

import io.github.pieter12345.javaloader.JavaProject.UnloadException;

/**
 * This interface allows an exception handler to be passed to a method which might throw multiple exceptions due to
 * performing operations on multiple projects.
 * @author P.J.S. Kools
 */
public interface UnloadExceptionHandler {
	void handleUnloadException(UnloadException e);
}
