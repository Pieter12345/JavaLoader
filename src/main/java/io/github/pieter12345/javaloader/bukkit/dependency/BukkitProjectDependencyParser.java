package io.github.pieter12345.javaloader.bukkit.dependency;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.dependency.Dependency;
import io.github.pieter12345.javaloader.core.dependency.DependencyScope;
import io.github.pieter12345.javaloader.core.dependency.JarDependency;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.exceptions.DependencyException;

/**
 * Represents a dependency parser for JavaLoader Bukkit project dependencies.
 * @author P.J.S. Kools
 */
public class BukkitProjectDependencyParser extends ProjectDependencyParser {
	
	/**
	 * Creates a new {@link BukkitProjectDependencyParser}.
	 */
	public BukkitProjectDependencyParser() {
	}
	
	@Override
	public Dependency parseDependency(JavaProject project, String dependencyStr) throws DependencyException {
		
		// Handle Bukkit plugin dependencies ("plugin pluginName").
		if(dependencyStr.toLowerCase().startsWith("plugin ")) {
			String pluginName = dependencyStr.substring("plugin ".length()).trim();
			Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
			if(plugin == null) {
				throw new DependencyException("Plugin not loaded in Bukkit: " + pluginName);
			}
			CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
			URL loc;
			String path;
			if(codeSource == null || (loc = codeSource.getLocation()) == null || (path = loc.getFile()).isEmpty()) {
				throw new DependencyException("Unable to obtain jar file from Bukkit plugin: " + pluginName);
			}
			try {
				return new JarDependency(new File(URLDecoder.decode(path, "UTF-8")), DependencyScope.PROVIDED);
			} catch (UnsupportedEncodingException e) {
				throw new Error(e); // Should be impossible.
			}
		}
		
		// Handle dependency through the parent.
		return super.parseDependency(project, dependencyStr);
	}
}
