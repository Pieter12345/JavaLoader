package io.github.pieter12345.javaloader.velocity;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
	private volatile ExecutorService executorService;
	
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
	 * Deinitializes the {@link JavaLoaderVelocityProject}.
	 */
	protected final void deinitialize() {
		if(this.executorService != null) {
			this.executorService.shutdown();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * By default, this returns a {@link PluginDescription} with the project name as ID.
	 */
	@Override
	public PluginDescription getDescription() {
		return new PluginDescription() {
			@Override
			public String getId() {
				return JavaLoaderVelocityProject.this.getName();
			}
		};
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
	 */
	@Override
	public ExecutorService getExecutorService() {
		if(this.executorService == null) {
			synchronized(this) {
				if(this.executorService == null) {
					this.executorService = Executors.unconfigurableExecutorService(Executors.newCachedThreadPool(
							new ThreadFactoryBuilder().setDaemon(true)
									.setNameFormat(this.getName() + " - Task Executor #%d").setDaemon(true).build()));
				}
			}
		}
		return this.executorService;
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
