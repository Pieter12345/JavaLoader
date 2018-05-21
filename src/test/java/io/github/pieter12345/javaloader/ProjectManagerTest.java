package io.github.pieter12345.javaloader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Writer;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.github.pieter12345.javaloader.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.exceptions.JavaProjectException;
import io.github.pieter12345.javaloader.exceptions.LoadException;
import io.github.pieter12345.javaloader.exceptions.UnloadException;
import io.github.pieter12345.javaloader.exceptions.handlers.UnloadExceptionHandler;

/**
 * Tests the {@link ProjectManager} class.
 * @author P.J.S. Kools
 */
class ProjectManagerTest {
	
	/**
	 * Tests the load order of projects that depend on eachother as (A -> B === A depends on B):
	 * C -> B -> A.
	 */
	@Test
	void testLoadAllOrder() throws JavaProjectException {
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, manager);
		JavaProject projectB = generateAndAddMockProject("projectB", false, manager, "projectA");
		JavaProject projectC = generateAndAddMockProject("projectC", false, manager, "projectB");
		
		// Invoke the loadAllProjects() method.
		manager.loadAllProjects((LoadException ex) -> {
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
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, manager, "projectB");
		JavaProject projectB = generateAndAddMockProject("projectB", false, manager);
		JavaProject projectC = generateAndAddMockProject("projectC", false, manager, "projectB");
		
		// Invoke the loadAllProjects() method.
		manager.loadAllProjects((LoadException ex) -> {
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
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", false, manager);
		JavaProject projectB = generateAndAddMockProject("projectB", false, manager, "projectA", "projectC");
		JavaProject projectC = generateAndAddMockProject("projectC", false, manager);
		
		// Invoke the loadAllProjects() method.
		manager.loadAllProjects((LoadException ex) -> {
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
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, manager);
		JavaProject projectB = generateAndAddMockProject("projectB", true, manager, "projectA");
		JavaProject projectC = generateAndAddMockProject("projectC", true, manager, "projectB");
		
		// Invoke the unloadAllProjects() method.
		manager.unloadAllProjects((UnloadException ex) -> {
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
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, manager, "projectB");
		JavaProject projectB = generateAndAddMockProject("projectB", true, manager);
		JavaProject projectC = generateAndAddMockProject("projectC", true, manager, "projectB");
		
		// Invoke the unloadAllProjects() method.
		manager.unloadAllProjects((UnloadException ex) -> {
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
		
		// Create the project manager.
		final ProjectManager manager = new ProjectManager(null);
		
		// Create the projects.
		JavaProject projectA = generateAndAddMockProject("projectA", true, manager);
		JavaProject projectB = generateAndAddMockProject("projectB", true, manager, "projectA", "projectC");
		JavaProject projectC = generateAndAddMockProject("projectC", true, manager);
		
		// Invoke the unloadAllProjects() method.
		manager.unloadAllProjects((UnloadException ex) -> {
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
	 * Generates a mock JavaProject object and adds it to the given project manager.
	 * Some methods have been given an override return value and/or are supressed from ever running.
	 * @param projectName - The name of the project.
	 * @param isEnabled - The value which will be returned by {@link JavaProject#isEnabled()}.
	 * @param manager - The project manager.
	 * @param projectDependencies - The dependencies of the project.
	 * @return The generated mock project.
	 */
	private static JavaProject generateAndAddMockProject(String projectName, boolean isEnabled,
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
			doNothing().when(project).compile(mock(Writer.class));
		} catch (JavaProjectException e) {
			// Never happens.
			throw new RuntimeException(e);
		}
		
		// Add the project to the project manager.
		manager.addProject(project);
		
		// Return the project mock.
		return project;
	}
	
}
