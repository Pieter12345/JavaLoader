package io.github.pieter12345.javaloader.velocity.dependency;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.dependency.Dependency;
import io.github.pieter12345.javaloader.core.dependency.DependencyScope;
import io.github.pieter12345.javaloader.core.dependency.JarDependency;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.exceptions.DependencyException;

/**
 * Represents a dependency parser for JavaLoader Velocity project dependencies.
 * @author P.J.S. Kools
 */
public class VelocityProjectDependencyParser extends ProjectDependencyParser {
	
	private final ProxyServer proxy;
	
	/**
	 * Creates a new {@link VelocityProjectDependencyParser}.
	 * @param proxy - The {@link ProxyServer}.
	 */
	public VelocityProjectDependencyParser(ProxyServer proxy) {
		this.proxy = proxy;
	}
	
	@Override
	public List<Dependency> parseDependency(JavaProject project, String dependencyStr) throws DependencyException {
		
		// Handle Velocity plugin dependencies ("plugin pluginName").
		if(dependencyStr.toLowerCase().startsWith("plugin ")) {
			String pluginName = dependencyStr.substring("plugin ".length()).trim();
			PluginContainer plugin = this.proxy.getPluginManager().getPlugin(pluginName).orElse(null);
			if(plugin == null) {
				throw new DependencyException("Plugin not known by Velocity: " + pluginName);
			}
			Object pluginInstance = plugin.getInstance().orElse(null);
			if(pluginInstance == null) {
				throw new DependencyException("Plugin not loaded in Velocity: " + pluginName);
			}
			CodeSource codeSource = pluginInstance.getClass().getProtectionDomain().getCodeSource();
			URL loc;
			String path;
			if(codeSource == null || (loc = codeSource.getLocation()) == null || (path = loc.getFile()).isEmpty()) {
				throw new DependencyException("Unable to obtain jar file from Velocity plugin: " + pluginName);
			}
			try {
				return Arrays.asList(new JarDependency(
						new File(URLDecoder.decode(path, "UTF-8")), DependencyScope.PROVIDED));
			} catch (UnsupportedEncodingException e) {
				throw new Error(e); // Should be impossible.
			}
		}
		
		// Handle dependency through the parent.
		return super.parseDependency(project, dependencyStr);
	}
}
