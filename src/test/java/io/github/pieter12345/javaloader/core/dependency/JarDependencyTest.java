package io.github.pieter12345.javaloader.core.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * Tests the {@link JarDependency} class.
 * @author P.J.S. Kools
 */
class JarDependencyTest {

	/**
	 * Tests that the constructor throws an exception for a null jar file argument.
	 */
	@Test
	void testConstructorJarFileNonNull() {
		assertThrows(NullPointerException.class, () -> new JarDependency(null, DependencyScope.INCLUDE));
	}
	
	/**
	 * Tests that the constructor throws an exception for a null dependency scope argument.
	 */
	@Test
	void testConstructorDependencyScopeNonNull() {
		assertThrows(NullPointerException.class, () -> new JarDependency(mock(File.class), null));
	}
	
	/**
	 * Tests that the getScope() method returns the scope passed to the constructor.
	 */
	@ParameterizedTest
	@EnumSource(DependencyScope.class)
	void testGetScope(DependencyScope scope) {
		JarDependency dep = new JarDependency(mock(File.class), scope);
		assertThat(dep.getScope()).isSameAs(scope);
	}
	
	/**
	 * Tests that the getFile() method returns the file passed to the constructor.
	 */
	@ParameterizedTest
	@EnumSource(DependencyScope.class)
	void testGetFile(DependencyScope scope) {
		File file = mock(File.class);
		JarDependency dep = new JarDependency(file, scope);
		assertThat(dep.getFile()).isSameAs(file);
	}
	
	/**
	 * Tests that the getURL() method returns the URL of the file passed to the constructor.
	 */
	@ParameterizedTest
	@EnumSource(DependencyScope.class)
	void testGetURL(DependencyScope scope) {
		File file = new File("some/path/to/some/file.jar");
		JarDependency dep = new JarDependency(file, scope);
		assertThat(dep.getURL()).isEqualTo(Utils.fileToURL(file));
	}
	
	/**
	 * Tests the equals(Object) method.
	 */
	@Test
	void testEquals() {
		JarDependency dep11 = new JarDependency(new File("some/path/to/some/file.jar"), DependencyScope.INCLUDE);
		JarDependency dep12 = new JarDependency(new File("some/path/to/some/file.jar"), DependencyScope.INCLUDE);
		JarDependency dep21 = new JarDependency(new File("some/path/to/some/file.jar"), DependencyScope.PROVIDED);
		JarDependency dep22 = new JarDependency(new File("some/path/to/some/file.jar"), DependencyScope.PROVIDED);
		JarDependency dep3 = new JarDependency(new File("some/path/to/some/file2.jar"), DependencyScope.INCLUDE);
		JarDependency dep4 = new JarDependency(new File("some/path/to/some/file2.jar"), DependencyScope.PROVIDED);
		JarDependency dep5 = null;
		
		// Assert that dep11 and dep12 are equal.
		assertThat(dep11).isEqualTo(dep12);
		assertThat(dep12).isEqualTo(dep11);

		// Assert that dep21 and dep22 are equal.
		assertThat(dep21).isEqualTo(dep22);
		assertThat(dep22).isEqualTo(dep21);
		
		// Assert that dep11, dep21, dep3, dep4 and dep5 are not equal.
		assertThat(dep11).isNotIn(dep21, dep3, dep4, dep5);
		assertThat(dep21).isNotIn(dep11, dep3, dep4, dep5);
		assertThat(dep3).isNotIn(dep11, dep21, dep4, dep5);
		assertThat(dep4).isNotIn(dep11, dep21, dep3, dep5);
		assertThat(dep5).isNotIn(dep11, dep21, dep3, dep4);
	}
	
	/**
	 * Tests the toString() method.
	 */
	@Test
	void testToString() {
		
		// Create the jar dependency.
		File file = new File("some/path/to/some/file.jar");
		JarDependency dep = new JarDependency(file, DependencyScope.INCLUDE);
		
		// Assert that the toString() method returns the expected string.
		assertThat(dep.toString()).isEqualTo(JarDependency.class.getName() + "{jarFile=\"" + file.getAbsolutePath()
				+ "\", scope=" + DependencyScope.INCLUDE.toString() + "}");
	}
	
}
