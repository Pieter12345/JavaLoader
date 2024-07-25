package io.github.pieter12345.javaloader.core.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.exceptions.DependencyException;

/**
 * Represents a dependency parser for JavaLoader project dependencies.
 * @author P.J.S. Kools
 */
public class ProjectDependencyParser {
	
	/**
	 * Creates a new {@link ProjectDependencyParser}.
	 */
	public ProjectDependencyParser() {
	}
	
	/**
	 * Parses the given dependencies string and returns the {@link Dependency} objects that represent these
	 * dependencies.
	 * @param project - The {@link JavaProject} which's dependencies are parsed.
	 * @param dependenciesStr - The dependencies string representation to parse.
	 * @return A {@link List} containing the parsed dependencies.
	 * @throws DependencyException If a dependency description is in an invalid format.
	 */
	public List<Dependency> parseDependencies(JavaProject project, String dependencyStr) throws DependencyException {
		
		// Replace cariage returns and tabs with whitespaces.
		dependencyStr = dependencyStr.replaceAll("[\r\t]", " ");
		
		// Remove leading and trailing whitespaces.
		dependencyStr = dependencyStr.replaceAll("(?<=(^|\n))[ ]+(?! )", "");
		dependencyStr = dependencyStr.replaceAll("(?<! )[ ]+(?=(\n|$))", "");
		
		// Remove comments starting with "//" or "#".
		dependencyStr = dependencyStr.replaceAll("(\\/\\/|\\#)[^$\n]*(?=(\n|$))", "");
		
		// Remove empty lines.
		dependencyStr = dependencyStr.replaceAll("(?<=(^|\n))[\n]+(?!$)", "");
		
		// Replace backslash file seperators (since these are not guarenteed to work outside of Windows).
		dependencyStr = dependencyStr.replace('\\', '/');
		
		// Split and parse the dependencies.
		if(dependencyStr.isEmpty()) {
			return new ArrayList<>();
		}
		String[] dependencyStrs = dependencyStr.split("\n");
		List<Dependency> dependencies = new ArrayList<>();
		for(int i = 0; i < dependencyStrs.length; i++) {
			dependencies.addAll(this.parseDependency(project, dependencyStrs[i]));
		}
		
		// Return the dependencies.
		return dependencies;
	}
	
	/**
	 * Parses the given dependency entry and returns the {@link Dependency} objects that it represents.
	 * @param project - The {@link JavaProject} for which this dependency entry is being parsed.
	 * @param dependencyStr - The dependency entry string to parse.
	 * @return The {@link List} containing the resulting dependencies.
	 * @throws DependencyException If the dependency entry is in an invalid format.
	 */
	public List<Dependency> parseDependency(JavaProject project, String dependencyStr) throws DependencyException {
		
		// Handle JavaProject dependencies ("project projectName").
		if(dependencyStr.toLowerCase().startsWith("project ")) {
			String projectName = dependencyStr.substring("project ".length()).trim();
			return Arrays.asList(new ProjectDependency(projectName, project.getProjectManager()));
		}
		
		// Handle .jar file dependencies ("jar -provided ./rel/path/to/dep.jar" or "jar C:/path/to/dep.jar").
		if(dependencyStr.toLowerCase().startsWith("jar ")) {
			String dependency = dependencyStr.substring("jar ".length()).trim();
			
			// Validate the .jar suffix.
			if(!dependency.toLowerCase().endsWith(".jar")) {
				throw new DependencyException("Dependency format invalid."
						+ " Jar dependency does not have a .jar extension: " + dependencyStr);
			}
			
			// Get the dependency scope (include or provided).
			DependencyScope scope;
			if(dependency.startsWith("-")) {
				int ind = dependency.indexOf(' ');
				String arg = (ind == -1 ? dependency.substring(1) : dependency.substring(1, ind));
				dependency = dependency.substring(ind + 1); // 'ind + 1' always exists due to the ".jar" suffix.
				switch(arg.toLowerCase()) {
					case "include": {
						scope = DependencyScope.INCLUDE;
						break;
					}
					case "provided": {
						scope = DependencyScope.PROVIDED;
						break;
					}
					default: {
						throw new DependencyException("Dependency format invalid."
								+ " Expected option \"INCLUDE\" or \"PROVIDED\" in syntax:"
								+ " \"jar -option pathToJar\",  but received option: \"" + arg + "\".");
					}
				}
			} else {
				scope = DependencyScope.INCLUDE;
			}
			
			// Get the dependency's file object, supporting relative paths.
			// Relative paths start with a '.' and are relative to the project's directory.
			// "dependency" has a ".jar" suffix, so some length checks here are redundant.
			if(dependency.startsWith(".") && (dependency.length() == 1 || dependency.charAt(1) == '/')) {
				dependency = project.getProjectDir().getAbsolutePath() + dependency.substring(1);
			}
			File file = new File(dependency);
			
			// Add the dependency to the array.
			return Arrays.asList(new JarDependency(file, scope));
		}
		
		// Dependency format not recognised.
		throw new DependencyException("Dependency format invalid: " + dependencyStr);
	}
}
