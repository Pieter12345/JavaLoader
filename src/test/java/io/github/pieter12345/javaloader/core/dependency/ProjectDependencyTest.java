package io.github.pieter12345.javaloader.core.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * Tests the {@link JarDependency} class.
 * @author P.J.S. Kools
 */
class ProjectDependencyTest {

	/**
	 * Tests that the constructor throws an exception for a null project name argument.
	 */
	@Test
	void testConstructorProjectNameNonNull() {
		assertThrows(NullPointerException.class, () -> new ProjectDependency(null, mock(ProjectManager.class)));
	}
	
	/**
	 * Tests that the constructor throws an exception for a null project manager argument.
	 */
	@Test
	void testConstructorProjectManagerNonNull() {
		assertThrows(NullPointerException.class, () -> new ProjectDependency("Project1", null));
	}
	
	/**
	 * Tests that the getScope() method returns DependencyScope.PROVIDED.
	 */
	@Test
	void testGetScope() {
		ProjectDependency dep = new ProjectDependency("Project1", mock(ProjectManager.class));
		assertThat(dep.getScope()).isSameAs(DependencyScope.PROVIDED);
	}
	
	/**
	 * Tests that the getFile() method returns the project binary directory file.
	 */
	@Test
	void testGetFile() {
		
		// Create the project manager and set the required methods up to be able to return the projects bin directory.
		String projectName = "Project1";
		File projectBinDir = mock(File.class);
		ProjectManager manager = mock(ProjectManager.class);
		JavaProject project = mock(JavaProject.class);
		doReturn(projectBinDir).when(project).getBinDir();
		doReturn(project).when(manager).getProject(projectName);
		
		// Create the project dependency.
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getFile() methods returns the projects binary directory.
		assertThat(dep.getFile()).isSameAs(projectBinDir);
		
		// Assert that only the necessary calls were made to the project and the project manager.
		verify(manager, times(1)).getProject(projectName);
		verify(project, times(1)).getBinDir();
		verifyNoMoreInteractions(manager, project);
	}
	
	/**
	 * Tests that the getFile() method returns null if the project does not exist in the project manager.
	 */
	@Test
	void testGetFileUnexistingProject() {
		
		// Create the project manager and set the required methods up to be able to return the projects bin directory.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		doReturn(null).when(manager).getProject(projectName);
		
		// Create the project dependency.
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getFile() methods returns null.
		assertThat(dep.getFile()).isNull();
		
		// Assert that only the necessary calls were made to the project manager.
		verify(manager, times(1)).getProject(projectName);
		verifyNoMoreInteractions(manager);
	}
	
	/**
	 * Tests that the getURL() method returns the URL of the project binary directory file.
	 */
	@Test
	void testGetURL() {
		
		// Create the project manager and set the required methods up to be able to return the projects bin directory.
		String projectName = "Project1";
		File projectBinDir = new File("path/to/some/project/bin");
		ProjectManager manager = mock(ProjectManager.class);
		JavaProject project = mock(JavaProject.class);
		doReturn(projectBinDir).when(project).getBinDir();
		doReturn(project).when(manager).getProject(projectName);
		
		// Create the project dependency.
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getURL() methods returns the URL to the projects binary directory.
		assertThat(dep.getURL()).isEqualTo(Utils.fileToURL(projectBinDir));
		
		// Assert that only the necessary calls were made to the project and the project manager.
		verify(manager, times(1)).getProject(projectName);
		verify(project, times(1)).getBinDir();
		verifyNoMoreInteractions(manager, project);
	}
	
	/**
	 * Tests that the getURL() method returns null if the project does not exist in the project manager.
	 */
	@Test
	void testGetURLUnexistingProject() {
		
		// Create the project manager and set the required methods up to be able to return the projects bin directory.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		doReturn(null).when(manager).getProject(projectName);
		
		// Create the project dependency.
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getURL() methods returns null
		assertThat(dep.getURL()).isNull();
		
		// Assert that only the necessary calls were made to the project manager.
		verify(manager, times(1)).getProject(projectName);
		verifyNoMoreInteractions(manager);
	}
	
	/**
	 * Tests that the getProject() method returns project returned by the project manager's getProject(String) method.
	 */
	@Test
	void testGetProject() {
		
		// Create the project manager and set the required methods up to be able to return the projects bin directory.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		JavaProject project = mock(JavaProject.class);
		doReturn(project).when(manager).getProject(projectName);
		
		// Create the project dependency.
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getProject() methods returns the project.
		assertThat(dep.getProject()).isSameAs(project);
		
		// Assert that only the necessary calls were made to the project and the project manager.
		verify(manager, times(1)).getProject(projectName);
		verifyNoMoreInteractions(manager, project);
	}
	
	/**
	 * Tests that the getProjectName() method returns project name passed to the constructor.
	 */
	@Test
	void testGetProjectName() {
		
		// Create the project dependency.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getProjectName() methods returns the project name.
		assertThat(dep.getProjectName()).isEqualTo(projectName);
		
		// Assert that no interactions were made with the project manager.
		verifyNoMoreInteractions(manager);
	}
	
	/**
	 * Tests that the getProjectManager() method returns project manager passed to the constructor.
	 */
	@Test
	void testGetProjectManager() {
		
		// Create the project dependency.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the getProjectManager() methods returns the project manager.
		assertThat(dep.getProjectManager()).isSameAs(manager);
		
		// Assert that no interactions were made with the project manager.
		verifyNoMoreInteractions(manager);
	}
	
	/**
	 * Tests the equals(Object) method.
	 */
	@Test
	void testEquals() {
		ProjectManager manager1 = mock(ProjectManager.class);
		ProjectManager manager2 = mock(ProjectManager.class);
		ProjectDependency dep11 = new ProjectDependency("Project1", manager1);
		ProjectDependency dep12 = new ProjectDependency("Project1", manager1);
		ProjectDependency dep2 = new ProjectDependency("Project1", manager2);
		ProjectDependency dep3 = new ProjectDependency("Project2", manager1);
		ProjectDependency dep4 = null;
		
		// Assert that dep11 and dep12 are equal.
		assertThat(dep11).isEqualTo(dep12);
		assertThat(dep12).isEqualTo(dep11);
		
		// Assert that dep11, dep2, dep3 and dep4 are not equal.
		assertThat(dep11).isNotIn(dep2, dep3, dep4);
		assertThat(dep2).isNotIn(dep11, dep3, dep4);
		assertThat(dep3).isNotIn(dep11, dep2, dep4);
		assertThat(dep4).isNotIn(dep11, dep2, dep3);
	}
	
	/**
	 * Tests the toString() method.
	 */
	@Test
	void testToString() {
		
		// Create the project dependency.
		String projectName = "Project1";
		ProjectManager manager = mock(ProjectManager.class);
		doReturn("ProjectManager@123hash456").when(manager).toString();
		ProjectDependency dep = new ProjectDependency(projectName, manager);
		
		// Assert that the toString() method returns the expected string.
		assertThat(dep.toString()).isEqualTo(ProjectDependency.class.getName()
				+ "{projectName=\"" + projectName + "\", projectManager=" + manager.toString() + "}");
	}
	
}
