package io.github.pieter12345.javaloader.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;

/**
 * Tests the {@link JavaProject} class.
 * @author P.J.S. Kools
 */
class JavaProjectTest {
	
	private String projectName;
	private File projectDir;
	private ProjectManager manager;
	private ProjectDependencyParser dependencyParser;
	private ProjectStateListener projectStateListener;
	private JavaProject project;
	
	@BeforeEach
	void init() {
		
		// Create a new JavaProject.
		this.projectName = "projectName";
		this.projectDir = new File(new File("").getAbsoluteFile(),
				"tempJavaProjects" + File.separator + "unexistingProject");
		this.manager = mock(ProjectManager.class);
		this.dependencyParser = mock(ProjectDependencyParser.class);
		this.projectStateListener = mock(ProjectStateListener.class);
		this.project = new JavaProject(
				this.projectName, this.projectDir, this.manager, this.dependencyParser, this.projectStateListener);
	}
	
	/**
	 * Tests that getName(), getProjectDir(), getBinDir(), getSourceDir() and getProjectManager() return the values
	 * set in the constructor.
	 */
	@Test
	void testConstructorAndGetters() {
		
		// Assert that the getters return the same objects/values as the ones that were set.
		assertThat(this.project.getName()).isEqualTo(this.projectName);
		assertThat(this.project.getProjectDir()).isEqualTo(this.projectDir);
		assertThat(this.project.getBinDir()).isEqualTo(new File(this.projectDir.getAbsoluteFile(), "bin"));
		assertThat(this.project.getSourceDir()).isEqualTo(new File(this.projectDir.getAbsoluteFile(), "src"));
		assertThat(this.project.getProjectManager()).isSameAs(this.manager);
	}
}
