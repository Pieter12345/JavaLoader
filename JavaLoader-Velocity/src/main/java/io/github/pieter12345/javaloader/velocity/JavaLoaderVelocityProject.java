package io.github.pieter12345.javaloader.velocity;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;

import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.JavaProject;

/**
 * This class represents a JavaLoader Velocity project.
 * A JavaLoader project on a Velocity proxy should have a main class which extends from this class.
 * @author P.J.S. Kools
 */
public abstract class JavaLoaderVelocityProject extends JavaLoaderProject implements PluginContainer {
	
	private ProxyServer proxy = null;
	private Logger logger = null;
	
	/**
	 * Initializes the {@link JavaLoaderVelocityProject} with the given {@link JavaProject} and {@link ProxyServer}.
	 * @param project - The JavaProject.
	 * @param proxy - The {@link ProxyServer} for this project.
	 * @param logger - The {@link Logger} for this project.
	 */
	protected final void initialize(JavaProject project, ProxyServer proxy, Logger logger) {
		super.initialize(project);
		this.proxy = proxy;
		this.logger = logger;
	}
	
	/**
	 * {@inheritDoc}
	 * By default, this returns {@code null}.
	 */
	@Override
	public PluginDescription getDescription() {
		return null;
	}
	
	/**
	 * Returns this instance. Used internally by Velocity.
	 */
	@Override
	public final Optional<?> getInstance() {
		return Optional.of(this);
	}
	
	/**
	 * {@inheritDoc}
	 * By default, this returns {@code null}.
	 */
	@Override
	public ExecutorService getExecutorService() {
		return null;
	}
	
	/**
	 * Gets the {@link ProxyServer} for this project.
	 * @return The {@link ProxyServer} for this project.
	 */
	public final ProxyServer getProxyServer() {
		return this.proxy;
	}
	
	/**
	 * Gets the {@link Logger} for this project.
	 * @return The {@link Logger} for this project.
	 */
	public final Logger getLogger() {
		return this.logger;
	}
}
