package io.github.pieter12345.javaloader.core.dependency;

import java.io.File;
import java.net.URL;
import java.util.Objects;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * Represents a JavaLoader project dependency for another JavaLoader project.
 * @author P.J.S. Kools
 */
public class ProjectDependency implements FileDependency {
	
	private final ProjectManager manager;
	private final String projectName;
	
	public ProjectDependency(String projectName, ProjectManager manager) {
		Objects.requireNonNull(projectName, "Project name may not be null.");
		Objects.requireNonNull(manager, "Project manager may not be null.");
		this.manager = manager;
		this.projectName = projectName;
	}
	
	@Override
	public DependencyScope getScope() {
		// JavaLoader parent projects should always be loaded before their children, so they should always be provided.
		return DependencyScope.PROVIDED;
	}
	
	/**
	 * Gets the URL leading to the bin directory of this project.
	 * @return The URL leading to the bin directory of this project
	 * or null if the project does not exist in the project manager.
	 */
	@Override
	public URL getURL() {
		File file = this.getFile();
		return (file == null ? null : Utils.fileToURL(file));
	}
	
	/**
	 * Gets the bin directory of this project.
	 * @return The bin directory of this project or null if the project does not exist in the project manager.
	 */
	@Override
	public File getFile() {
		JavaProject project = this.manager.getProject(this.projectName);
		return (project == null ? null : project.getBinDir());
	}
	
	/**
	 * Gets the JavaProject which this dependency represents.
	 * @return The JavaProject which this dependency represents
	 * or null if the project does not exist in the project manager.
	 */
	public JavaProject getProject() {
		return this.manager.getProject(this.projectName);
	}
	
	/**
	 * Returns the project name of the JavaProject which this dependency represents
	 * (even when the actual project does not exist).
	 * @return The project name.
	 */
	public String getProjectName() {
		return this.projectName;
	}
	
	/**
	 * Returns the project manager, used to resolve this dependency from the project name.
	 * @return The project manager.
	 */
	public ProjectManager getProjectManager() {
		return this.manager;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ProjectDependency
				&& ((ProjectDependency) obj).projectName.equals(this.projectName)
				&& ((ProjectDependency) obj).getScope().equals(this.getScope())
				&& ((ProjectDependency) obj).manager.equals(this.manager);
	}
	
	@Override
	public String toString() {
		return this.getClass().getName()
				+ "{projectName=\"" + this.projectName + "\", projectManager=" + this.manager.toString() + "}";
	}
	
}
