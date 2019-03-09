package io.github.pieter12345.javaloader;

import static io.github.pieter12345.javaloader.dependency.DependencyScope.INCLUDE;
import static io.github.pieter12345.javaloader.dependency.DependencyScope.PROVIDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.pieter12345.javaloader.dependency.Dependency;
import io.github.pieter12345.javaloader.dependency.JarDependency;
import io.github.pieter12345.javaloader.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.exceptions.DependencyException;

/**
 * Tests the {@link JavaProject} class.
 * @author P.J.S. Kools
 */
class JavaProjectTest {
	
	private String projectName;
	private File projectDir;
	private ProjectManager manager;
	private ProjectStateListener projectStateListener;
	private JavaProject project;
	
	private static final String BASE_DIR_PATH = new File("").getAbsolutePath();
	
	@BeforeEach
	void init() {
		
		// Create a new JavaProject.
		this.projectName = "projectName";
		this.projectDir = new File(new File("").getAbsoluteFile(),
				"tempJavaProjects" + File.separator + "unexistingProject");
		this.manager = mock(ProjectManager.class);
		this.projectStateListener = mock(ProjectStateListener.class);
		this.project = new JavaProject(this.projectName, this.projectDir, this.manager, this.projectStateListener);
		
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
	
	@Test
	void testParseDependenciesEmptyString() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("");
		assertThat(dependencies).isEmpty();
	}
	
	@Test
	void testParseDependenciesSingleJarDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("jar " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("jar ./../../path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep2() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("jar ./path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(this.projectDir.getAbsoluteFile(), "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep3() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("jar .\\path\\to\\myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(this.projectDir.getAbsoluteFile(), "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleIncludeJarDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies(
				"jar -include " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleIncludeRandomCasingJarDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies(
				"jar -IncLUdE " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleProvidedJarDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies(
				"jar -provided " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), PROVIDED));
	}
	
	@Test
	void testParseDependenciesSingleProjectDep() throws DependencyException {
		Dependency[] dependencies = this.project.parseDependencies("project projName");
		assertThat(dependencies).containsExactly(new ProjectDependency("projName", this.manager));
	}
	
	@ParameterizedTest
	@ValueSource(strings = {
			
			// Missing ".jar" extension.
			"jar ./path/to/myJar",
			
			// Missing arguments (should all fail due to missing the ".jar" extension).
			"jar ",
			"jar",
			"jar -include ",
			"jar -include",
			"jar -provided ",
			"jar -provided",
			
			// Single scope argument with ".jar" extension.
			"jar -include.jar",
			
			// Invalid dependency type.
			"someInvalidType",
			"someInvalidType projectName",
			"someInvalidType ./path/to.myJar.jar",
			
			// Invalid scope.
			"jar -invalidScope ./path/to.myJar.jar",
			"jar - ./path/to.myJar.jar"
			
		})
	void testParseDependenciesInvalidInput(String input) {
		assertThrows(DependencyException.class, () -> this.project.parseDependencies(input));
	}
	
	@Test
	void testParseMultipleDependencies() throws DependencyException {
		String input = "jar ./myJar.jar"
				+ "\njar -include ./myJarInc.jar"
				+ "\njar -provided ./myJarProv.jar"
				+ "\nproject project1"
				+ "\nproject project2";
		Dependency[] dependencies = this.project.parseDependencies(input);
		assertThat(dependencies).containsExactly(
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJar.jar"), INCLUDE),
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJarInc.jar"), INCLUDE),
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJarProv.jar"), PROVIDED),
				new ProjectDependency("project1", this.manager),
				new ProjectDependency("project2", this.manager));
	}
	
}
