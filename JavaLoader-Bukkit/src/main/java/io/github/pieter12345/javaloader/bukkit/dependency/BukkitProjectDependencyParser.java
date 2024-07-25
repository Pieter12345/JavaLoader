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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
			String libNameDesc = dependencyStr.substring("mclib ".length()).trim();
			
			// Get library names.
			List<String> libNameList;
			try {
				libNameList = this.getBundledMCLibNameList();
			} catch (FileNotFoundException e1) {
				throw new DependencyException("Unable to obtain bundled library list from server jar"
						+ " (could not find resource META-INF/libraries.list): " + libNameDesc);
			} catch (IOException e1) {
				throw new DependencyException("Unable to obtain bundled library list from server jar"
						+ " (could not read resource META-INF/libraries.list): " + libNameDesc);
			}
			
			// Match given library name.
			List<String> libNames = new ArrayList<>();
			boolean isRegexMatch = libNameDesc.startsWith("<") && libNameDesc.endsWith(">");
			if(isRegexMatch) {
				
				// Handle "<...>" entries as regex.
				String libNameRegex = libNameDesc.substring(1, libNameDesc.length() - 1) + "\\.jar";
				Pattern matchPattern;
				try {
					matchPattern = Pattern.compile(libNameRegex);
				} catch (PatternSyntaxException e) {
					throw new DependencyException("Invalid mclib name regex: "
							+ libNameRegex.substring(0, libNameRegex.length() - "\\.jar".length()));
				}
				for(String libName : libNameList) {
					if(matchPattern.matcher(libName).matches()) {
						libNames.add(libName);
					}
				}
			} else if(!libNameList.contains(libNameDesc + ".jar")) {
				throw new DependencyException("Library not bundled in server jar: " + libNameDesc);
			} else {
				libNames.add(libNameDesc + ".jar");
			}
			
			// Get library jar paths and convert them to library jar dependencies.
			List<Dependency> dependencies = new ArrayList<>();
			String bundlerRepoDir = System.getProperty("bundlerRepoDir", "bundler");
			for(String libName : libNames) {
				File libFile = new File(bundlerRepoDir, "libraries" + File.separator + libName);
				if(!libFile.exists()) {
					if(isRegexMatch) {
						continue; // Ignore non-existent (accidentally) matched entry. Not all entries are unpacked.
					}
					throw new DependencyException(
							"Library not found in bundler libraries directory at: " + libFile.getAbsolutePath());
				}
				dependencies.add(new JarDependency(libFile, DependencyScope.PROVIDED));
			}
			
			// Return library jar dependencies.
			return dependencies;
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
