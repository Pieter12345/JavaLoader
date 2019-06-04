package io.github.pieter12345.javaloader.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.Writer;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.github.pieter12345.javaloader.core.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.core.exceptions.JavaProjectException;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.exceptions.handlers.UnloadExceptionHandler;

/**
 * Tests the {@link ProjectManager} class.
 * @author P.J.S. Kools
 */
class ProjectManagerTest {
	
	private ProjectManager manager;
	private File projectsDirMock;
	
	@BeforeEach
	void init() {
		this.projectsDirMock = mock(File.class);
		this.manager = new ProjectManager(this.projectsDirMock);
	}
	
	/**
	 * Tests the load order of projects that depend on eachother as (A -> B === A depends on B):
	 * C -> B -> A.
	 */
	@Test
	void testLoadAllOrder() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", false, this.manager, "projectA");
		JavaProject projectC = generateAndAddMockProject("projectC", false, this.manager, "projectB");
		
		// Invoke the loadAllProjects() method.
		this.manager.loadAllProjects((LoadException ex) -> {
			fail("Unexpected LoadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.load() call order.
		InOrder inOrder = inOrder(projectA, projectB, projectC);
		inOrder.verify(projectA).load();
		inOrder.verify(projectB).load();
		inOrder.verify(projectC).load();
		
		// Verify that every project was only loaded once.
		verify(projectA, times(1)).load();
		verify(projectB, times(1)).load();
		verify(projectC, times(1)).load();
	}
	
	/**
	 * Tests the load order of projects that depend on eachother as (A -> B === A depends on B):
	 * A -> B <- C.
	 */
	@Test
	void testLoadAllOrder2() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, this.manager, "projectB");
		JavaProject projectB = generateAndAddMockProject("projectB", false, this.manager);
		JavaProject projectC = generateAndAddMockProject("projectC", false, this.manager, "projectB");
		
		// Invoke the loadAllProjects() method.
		this.manager.loadAllProjects((LoadException ex) -> {
			fail("Unexpected LoadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.load() call order.
		InOrder inOrder = inOrder(projectA, projectB);
		inOrder.verify(projectB).load();
		inOrder.verify(projectA).load();
		
		inOrder = inOrder(projectB, projectC);
		inOrder.verify(projectB).load();
		inOrder.verify(projectC).load();
		
		// Verify that every project was only loaded once.
		verify(projectA, times(1)).load();
		verify(projectB, times(1)).load();
		verify(projectC, times(1)).load();
	}
	
	/**
	 * Tests the load order of projects that depend on eachother as (A -> B === A depends on B):
	 * A <- B -> C.
	 */
	@Test
	void testLoadAllOrder3() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", false, this.manager, "projectA", "projectC");
		JavaProject projectC = generateAndAddMockProject("projectC", false, this.manager);
		
		// Invoke the loadAllProjects() method.
		this.manager.loadAllProjects((LoadException ex) -> {
			fail("Unexpected LoadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.load() call order.
		InOrder inOrder = inOrder(projectA, projectB);
		inOrder.verify(projectA).load();
		inOrder.verify(projectB).load();
		
		inOrder = inOrder(projectB, projectC);
		inOrder.verify(projectC).load();
		inOrder.verify(projectB).load();
		
		// Verify that every project was only loaded once.
		verify(projectA, times(1)).load();
		verify(projectB, times(1)).load();
		verify(projectC, times(1)).load();
	}
	
	/**
	 * Tests the unload order of projects that depend on eachother as (A -> B === A depends on B):
	 * C -> B -> A.
	 */
	@Test
	void testUnloadAllOrder() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", true, this.manager, "projectA");
		JavaProject projectC = generateAndAddMockProject("projectC", true, this.manager, "projectB");
		
		// Invoke the unloadAllProjects() method.
		this.manager.unloadAllProjects((UnloadException ex) -> {
			fail("Unexpected UnloadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.unload() call order.
		InOrder inOrder = inOrder(projectA, projectB, projectC);
		inOrder.verify(projectC).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectB).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectA).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		
		// Verify that every project was only loaded once.
		verify(projectA, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectB, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectC, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
	}
	
	/**
	 * Tests the unload order of projects that depend on eachother as (A -> B === A depends on B):
	 * A -> B <- C.
	 */
	@Test
	void testUnloadAllOrder2() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, this.manager, "projectB");
		JavaProject projectB = generateAndAddMockProject("projectB", true, this.manager);
		JavaProject projectC = generateAndAddMockProject("projectC", true, this.manager, "projectB");
		
		// Invoke the unloadAllProjects() method.
		this.manager.unloadAllProjects((UnloadException ex) -> {
			fail("Unexpected UnloadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.unload() call order.
		InOrder inOrder = inOrder(projectA, projectB);
		inOrder.verify(projectA).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectB).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		
		inOrder = inOrder(projectB, projectC);
		inOrder.verify(projectC).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectB).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		
		// Verify that every project was only unloaded once.
		verify(projectA, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectB, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectC, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
	}
	
	/**
	 * Tests the unload order of projects that depend on eachother as (A -> B === A depends on B):
	 * A <- B -> C.
	 */
	@Test
	void testUnloadAllOrder3() throws JavaProjectException {
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", true, this.manager, "projectA", "projectC");
		JavaProject projectC = generateAndAddMockProject("projectC", true, this.manager);
		
		// Invoke the unloadAllProjects() method.
		this.manager.unloadAllProjects((UnloadException ex) -> {
			fail("Unexpected UnloadException in mock project (should never run).", ex);
		});
		
		// Verify the JavaProject.unload() call order.
		InOrder inOrder = inOrder(projectA, projectB);
		inOrder.verify(projectB).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectA).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		
		inOrder = inOrder(projectB, projectC);
		inOrder.verify(projectB).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		inOrder.verify(projectC).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		
		// Verify that every project was only unloaded once.
		verify(projectA, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectB, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
		verify(projectC, times(1)).unload(any(UnloadMethod.class), any(UnloadExceptionHandler.class));
	}
	
	/**
	 * Tests that addProject(JavaProject) adds the project with one and two projects, using the
	 * getProjects() and getProjectNames() methods to validate the add.
	 */
	@Test
	void testAddProject() {
		
		// Create the projects.
		JavaProject projectA = generateMockProject("projectA", false, this.manager);
		JavaProject projectB = generateMockProject("projectB", false, this.manager);
		
		// Add projectA.
		this.manager.addProject(projectA);
		
		// Assert that the project was added.
		assertThat(this.manager.getProjects()).containsExactly(projectA);
		assertThat(this.manager.getProjectNames()).containsExactly("projectA");
		
		// Add projectB.
		this.manager.addProject(projectB);
		
		// Assert that the project was added.
		assertThat(this.manager.getProjects()).containsExactlyInAnyOrder(projectA, projectB);
		assertThat(this.manager.getProjectNames()).containsExactlyInAnyOrder("projectA", "projectB");
		
	}
	
	/**
	 * Tests that hasProject(JavaProject) returns false before, and true after a call to addProject(JavaProject).
	 */
	@Test
	void testHasProject() {
		
		// Create the project.
		JavaProject projectA = generateMockProject("projectA", false, this.manager);
		
		// Assert that the project is not added added.
		assertThat(this.manager.hasProject("projectA")).isFalse();
		
		// Add the project.
		this.manager.addProject(projectA);
		
		// Assert that the project was added.
		assertThat(this.manager.hasProject("projectA")).isTrue();
	}
	
	/**
	 * Tests that a second call to addProject(JavaProject) with the same project does not add the project again.
	 */
	@Test
	void testAddSameProjectTwice() {
		
		// Create the project.
		JavaProject projectA = generateMockProject("projectA", false, this.manager);
		
		// Add the project.
		this.manager.addProject(projectA);
		
		// Assert that the project was added.
		assertThat(this.manager.getProjects()).containsExactly(projectA);
		
		// Add the project again.
		this.manager.addProject(projectA);
		
		// Assert that the project is still only once on the list.
		assertThat(this.manager.getProjects()).containsExactly(projectA);
		
	}
	
	/**
	 * Tests that a call to addProject(JavaProject) with a project that has the same name as an already-added project
	 * gets ignored.
	 */
	@Test
	void testAddDifferentProjectTwice() {
		
		// Create the projects.
		JavaProject projectA = generateMockProject("projectA", false, this.manager);
		JavaProject projectA2 = generateMockProject("projectA", false, this.manager);
		
		// Add the project.
		this.manager.addProject(projectA);
		
		// Assert that the project was added.
		assertThat(this.manager.getProjects()).containsExactly(projectA);
		
		// Add the other project with the same name.
		this.manager.addProject(projectA2);
		
		// Assert that adding the second project was ignored.
		assertThat(this.manager.getProjects()).containsExactly(projectA);
		
	}
	
	/**
	 * Tests that an exception is thrown when adding a project that has a different project manager assigned to it.
	 */
	@Test
	void testAddProjectDifferentManager() {
		
		// Create the project.
		JavaProject projectA = generateMockProject("projectA", false, mock(ProjectManager.class));
		
		// Assert that adding the project throws an exception.
		assertThrows(IllegalStateException.class, () -> this.manager.addProject(projectA));
		
	}
	
	/**
	 * Tests that removing an unloading project works.
	 */
	@Test
	void testRemoveUnloadedProject() {
		
		// Create and add the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", false, this.manager);
		JavaProject projectC = generateAndAddMockProject("projectC", true, this.manager);
		
		// Assert that the projects were added.
		assertThat(this.manager.getProjects()).containsExactlyInAnyOrder(projectA, projectB, projectC);
		
		// Remove a project.
		this.manager.removeProject(projectB);
		
		// Assert that the project was removed.
		assertThat(this.manager.getProjects()).containsExactlyInAnyOrder(projectA, projectC);
		
	}
	
	/**
	 * Tests that removing a loaded project throws an exception.
	 */
	@Test
	void testRemoveLoadedProject() {
		
		// Create and add the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, this.manager);
		JavaProject projectB = generateAndAddMockProject("projectB", true, this.manager);
		JavaProject projectC = generateAndAddMockProject("projectC", true, this.manager);
		
		// Assert that the projects were added.
		assertThat(this.manager.getProjects()).containsExactlyInAnyOrder(projectA, projectB, projectC);
		
		// Assert that removing the loaded project throws an exception.
		assertThrows(IllegalStateException.class, () -> this.manager.removeProject(projectB));
		
	}
	
	/**
	 * Tests that getProjectsDir() returns the projects dir which was passed to the constructor.
	 */
	@Test
	void testGetProjectsDir() {
		assertThat(this.manager.getProjectsDir()).isEqualTo(this.projectsDirMock);
	}
	
	
	
	/**
	 * Generates a mock JavaProject object and adds it to the given project manager.
	 * @param projectName - The name of the project.
	 * @param isEnabled - The value which will be returned by {@link JavaProject#isEnabled()}.
	 * @param manager - The project manager.
	 * @param projectDependencies - The dependencies of the project.
	 * @return The generated mock project.
	 */
	private static JavaProject generateAndAddMockProject(String projectName, boolean isEnabled,
			ProjectManager manager, String... projectDependencies) {
		
		// Create the project mock.
		JavaProject project = generateMockProject(projectName, isEnabled, manager, projectDependencies);
		
		// Add the project to the project manager.
		manager.addProject(project);
		
		// Return the project mock.
		return project;
	}
	
	/**
	 * Generates a mock JavaProject object.
	 * @param projectName - The name of the project.
	 * @param isEnabled - The value which will be returned by {@link JavaProject#isEnabled()}.
	 * @param manager - The project manager.
	 * @param projectDependencies - The dependencies of the project.
	 * @return The generated mock project.
	 */
	private static JavaProject generateMockProject(String projectName, boolean isEnabled,
			ProjectManager manager, String... projectDependencies) {
		
		// Create the project mock.
		JavaProject project = mock(JavaProject.class);
		
		// Generate project dependencies array.
		ProjectDependency[] dependencies = new ProjectDependency[projectDependencies.length];
		for(int i = 0; i < projectDependencies.length; i++) {
			dependencies[i] = new ProjectDependency(projectDependencies[i], manager);
		}
		
		// Set return values for methods that will be called.
		when(project.getName()).thenReturn(projectName);
		when(project.getProjectManager()).thenReturn(manager);
		when(project.isEnabled()).thenReturn(isEnabled);
		when(project.getDependencies()).thenReturn(dependencies);
		
		// Prevent load(), unload() and compile() from executing.
		try {
			doNothing().when(project).load();
			doReturn(Arrays.asList(project)).when(project).unload(
					any(UnloadMethod.class), any(UnloadExceptionHandler.class));
			doNothing().when(project).compile(any(Writer.class));
		} catch (JavaProjectException e) {
			// Never happens.
			throw new RuntimeException(e);
		}
		
		// Return the project mock.
		return project;
		
	}
	
}
