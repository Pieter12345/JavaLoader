package io.github.pieter12345.javaloader.core.dependency;

import static io.github.pieter12345.javaloader.core.dependency.DependencyScope.INCLUDE;
import static io.github.pieter12345.javaloader.core.dependency.DependencyScope.PROVIDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.exceptions.DependencyException;

/**
 * Tests the {@link ProjectDependencyParser} class.
 * @author P.J.S. Kools
 */
public class ProjectDependencyParserTest {
	
	private ProjectDependencyParser dependencyParser;
	private File projectDir;
	private ProjectManager manager;
	private JavaProject projectMock;
	
	private static final String BASE_DIR_PATH = new File("").getAbsolutePath();
	
	public ProjectDependencyParser createProjectDependencyParser() {
		return new ProjectDependencyParser();
	}
	
	@BeforeEach
	void init() {
		this.dependencyParser = this.createProjectDependencyParser();
		this.projectMock = mock(JavaProject.class);
		this.projectDir = new File(new File("").getAbsoluteFile(),
				"tempJavaProjects" + File.separator + "unexistingProject");
		this.manager = mock(ProjectManager.class);
		doReturn(this.projectDir).when(this.projectMock).getProjectDir();
		doReturn(this.manager).when(this.projectMock).getProjectManager();
	}
	
	@Test
	void testParseDependenciesEmptyString() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(this.projectMock, "");
		assertThat(dependencies).isEmpty();
	}
	
	@Test
	void testParseDependenciesSingleJarDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar ./../../path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep2() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar ./path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(this.projectDir.getAbsoluteFile(), "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleRelativeJarDep3() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar .\\path\\to\\myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(this.projectDir.getAbsoluteFile(), "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleIncludeJarDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar -include " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleIncludeRandomCasingJarDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar -IncLUdE " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), INCLUDE));
	}
	
	@Test
	void testParseDependenciesSingleProvidedJarDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(
				this.projectMock, "jar -provided " + BASE_DIR_PATH + "/path/to/myJar.jar");
		assertThat(dependencies).containsExactly(new JarDependency(
				new File(BASE_DIR_PATH, "path/to/myJar.jar"), PROVIDED));
	}
	
	@Test
	void testParseDependenciesSingleProjectDep() throws DependencyException {
		Dependency[] dependencies = this.dependencyParser.parseDependencies(this.projectMock, "project projName");
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
		assertThrows(DependencyException.class, () -> this.dependencyParser.parseDependencies(this.projectMock, input));
	}
	
	@Test
	void testParseMultipleDependencies() throws DependencyException {
		String input = "jar ./myJar.jar"
				+ "\njar -include ./myJarInc.jar"
				+ "\njar -provided ./myJarProv.jar"
				+ "\nproject project1"
				+ "\nproject project2";
		Dependency[] dependencies = this.dependencyParser.parseDependencies(this.projectMock, input);
		assertThat(dependencies).containsExactly(
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJar.jar"), INCLUDE),
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJarInc.jar"), INCLUDE),
				new JarDependency(new File(this.projectDir.getAbsoluteFile(), "myJarProv.jar"), PROVIDED),
				new ProjectDependency("project1", this.manager),
				new ProjectDependency("project2", this.manager));
	}
}
