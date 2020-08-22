package io.github.pieter12345.javaloader.core;

/**
 * Provides options to indicate how dependents of a project should be handled during a recompile of that project.
 * @author P.J.S. Kools
 */
public enum DependentsHandling {
	
	/**
	 * This option indicates that compilation should fail if any dependent of a recompiling project is loaded.
	 */
	NONE,
	
	/**
	 * This option indicates that loaded dependents of a recompiling project should reload, but not recompile.
	 * Note that with this option, it is possible that dependents become binary incompatible with the recompiling
	 * project.
	 */
	RELOAD,
	
	/**
	 * This option indicates that loaded dependents of a recompiling project should also recompile.
	 * This option is safe in terms that it causes a recompile to fail when any binary incompatibility is introduced.
	 */
	RECOMPILE;
}
