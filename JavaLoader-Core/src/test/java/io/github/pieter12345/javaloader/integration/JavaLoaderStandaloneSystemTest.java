package io.github.pieter12345.javaloader.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.utils.Utils;
import io.github.pieter12345.javaloader.standalone.JavaLoaderStandalone;

/**
 * Tests system behaviour using the {@link JavaLoaderStandalone} class.
 * @author P.J.S. Kools
 */
class JavaLoaderStandaloneSystemTest {

	private static final File PROJECTS_DIR = new File(new File("").getAbsoluteFile(), "tempJavaProjects");
	
	private JavaLoaderStandalone javaLoader;
	
	/**
	 * Ensure that the projects directory that will be used does not exist yet. Otherwise, fail to prevent tests from
	 * altering the existing directory.
	 */
	@BeforeAll
	static void initAll() {
		// If this fails, manually remove the PROJECTS_DIR directory from the file system.
		assertThat(PROJECTS_DIR.exists()).isFalse();
	}
	
	/**
	 * Creates a new JavaLoaderStandalone instance.
	 */
	@BeforeEach
	void init() {
		this.javaLoader = new JavaLoaderStandalone(PROJECTS_DIR);
	}
	
	/**
	 * Stops the JavaLoaderStandalone instance and removes the possibly generated projects directory.
	 */
	@AfterEach
	void tearDown() {
		this.javaLoader.stop();
		if(PROJECTS_DIR.exists()) {
			Utils.removeFile(PROJECTS_DIR);
			assertThat(PROJECTS_DIR.exists()).isFalse();
		}
	}
	
	/**
	 * Tests that starting JavaLoader generates an example project.
	 */
	@Test
	void testExampleProjectGeneration() {
		
		// Assert that the projects directory does not exist.
		assertThat(PROJECTS_DIR.exists()).isFalse();
		
		// Start JavaLoader standalone.
		this.javaLoader.start();
		
		// Assert that an example project was added.
		assertThat(this.javaLoader.getProjectNames()).isNotEmpty();
	}
	
	/**
	 * Tests that starting JavaLoader does not generate example projects when the projects directory exists.
	 */
	@Test
	void testExampleProjectGeneration2() {
		
		// Assert that the projects directory does not exist.
		assertThat(PROJECTS_DIR.exists()).isFalse();
		
		// Create the projects directory.
		PROJECTS_DIR.mkdir();
		
		// Assert that the projects directory exists.
		assertThat(PROJECTS_DIR.exists()).isTrue();
		
		// Start JavaLoader standalone.
		this.javaLoader.start();
		
		// Assert that no example project was added.
		assertThat(this.javaLoader.getProjectNames()).isEmpty();
	}
	
	/**
	 * Tests:
	 * <ul>
	 * <li>That starting JavaLoader generates an uncompiled example project.</li>
	 * <li>That this project is loaded and enabled after a recompile and load.</li>
	 * <li>That this project is unloaded after an unload.</li>
	 * <li>That this project is loaded and enabled after being loaded again.</li>
	 * </ul>
	 */
	@Test
	void testProjectCompileLoadUnloadLoad() {
		
		// Assert that the projects directory does not exist.
		assertThat(PROJECTS_DIR.exists()).isFalse();
		
		// Start JavaLoader standalone.
		this.javaLoader.start();
		
		// Assert that an example project was added.
		assertThat(this.javaLoader.getProjectNames()).isNotEmpty();
		
		// Get an example project.
		String projectName = this.javaLoader.getProjectNames()[0];
		
		// Assert that the project has not been compiled yet.
		assertThat(this.javaLoader.getProject(projectName)).isNull();
		
		// Compile and load the project.
		this.javaLoader.executeCommand("recompile " + projectName);
		this.javaLoader.executeCommand("load " + projectName);
		
		// Assert that the project has been loaded.
		JavaLoaderProject project = this.javaLoader.getProject(projectName);
		assertThat(project).isNotNull();
		assertThat(project.isLoaded()).isTrue();
		
		// Unload the project and assert that it was unloaded.
		this.javaLoader.executeCommand("unload " + projectName);
		assertThat(this.javaLoader.getProject(projectName)).isNull();
		assertThat(project.isLoaded()).isFalse();
		
		// Load the project again and assert that it has been loaded.
		this.javaLoader.executeCommand("load " + projectName);
		project = this.javaLoader.getProject(projectName);
		assertThat(project).isNotNull();
		assertThat(project.isLoaded()).isTrue();
	}
}
