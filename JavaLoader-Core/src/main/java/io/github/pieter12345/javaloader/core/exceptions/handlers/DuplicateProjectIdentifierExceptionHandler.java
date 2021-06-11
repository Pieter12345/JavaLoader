package io.github.pieter12345.javaloader.core.exceptions.handlers;

import io.github.pieter12345.javaloader.core.exceptions.DuplicateProjectIdentifierException;

/**
 * This interface allows an exception handler to be passed to a method which might throw multiple exceptions.
 * @author P.J.S. Kools
 */
public interface DuplicateProjectIdentifierExceptionHandler {
	void handleDuplicateProjectIdentifierException(DuplicateProjectIdentifierException e);
}
