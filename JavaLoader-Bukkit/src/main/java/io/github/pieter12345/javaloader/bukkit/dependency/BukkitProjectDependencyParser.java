package io.github.pieter12345.javaloader.bukkit.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
	
	private static List<String> bundledLibNameList = null;
	
	/**
	 * Creates a new {@link BukkitProjectDependencyParser}.
	 */
	public BukkitProjectDependencyParser() {
	}
	
	@Override
	public List<Dependency> parseDependency(JavaProject project, String dependencyStr) throws DependencyException {
		
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
				return Arrays.asList(new JarDependency(
						new File(URLDecoder.decode(path, "UTF-8")), DependencyScope.PROVIDED));
			} catch (UnsupportedEncodingException e) {
				throw new Error(e); // Should be impossible.
			}
		}
		
		// Handle bundled Minecraft libraries.
		if(dependencyStr.toLowerCase().startsWith("mclib ")) {
			String libName = dependencyStr.substring("mclib ".length()).trim();
			
			// Get library names.
			List<String> libNameList;
			try {
				libNameList = this.getBundledMCLibNameList();
			} catch (FileNotFoundException e1) {
				throw new DependencyException("Unable to obtain bundled library list from server jar"
						+ " (could not find resource META-INF/libraries.list): " + libName);
			} catch (IOException e1) {
				throw new DependencyException("Unable to obtain bundled library list from server jar"
						+ " (could not read resource META-INF/libraries.list): " + libName);
			}
			
			// Match given library name.
			if(!libNameList.contains(libName + ".jar")) {
				throw new DependencyException("Library not bundled in server jar: " + libName);
			}
			
			// Get library jar path.
			String bundlerRepoDir = System.getProperty("bundlerRepoDir", "bundler");
			File libFile = new File(bundlerRepoDir + File.separator + "libraries" + File.separator + libName + ".jar");
			if(!libFile.exists()) {
				throw new DependencyException(
						"Library not found in bundler libraries directory at: " + libFile.getAbsolutePath());
			}
			
			// Return library jar dependency.
			return Arrays.asList(new JarDependency(libFile, DependencyScope.PROVIDED));
		}
		
		// Handle dependency through the parent.
		return super.parseDependency(project, dependencyStr);
	}
	
	/**
	 * Gets the list of bundled Minecraft library names from the server bootstrap jar (MC 1.18+).
	 * The result from this call will be cached upon success.
	 * @return The list of library names.
	 * @throws FileNotFoundException If the "META-INF/libraries.list" resource could not be found.
	 * @throws IOException If an I/O error occurs while reading the "META-INF/libraries.list" resource.
	 */
	private List<String> getBundledMCLibNameList() throws FileNotFoundException, IOException {
		
		// Return cached library list if available.
		if(bundledLibNameList != null) {
			return bundledLibNameList;
		}
		
		// Read libraries list.
		InputStream inStream = Bukkit.class.getClassLoader().getResourceAsStream("META-INF/libraries.list");
		if(inStream == null) {
			throw new FileNotFoundException();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
		List<String> libEntries = reader.lines().collect(Collectors.toList());
		
		// Parse library list.
		List<String> libList = new ArrayList<>();
		for(String libEntry : libEntries) {
			String[] split = libEntry.split(" ");
			if(split.length == 2 && split[1].length() > 1) {
				libList.add(split[1].substring(1));
			}
		}
		
		// Cache and return library list.
		bundledLibNameList = libList;
		return libList;
	}
}
