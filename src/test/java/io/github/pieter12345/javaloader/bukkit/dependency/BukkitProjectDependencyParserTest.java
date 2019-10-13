package io.github.pieter12345.javaloader.bukkit.dependency;

import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParserTest;

/**
 * Tests the {@link BukkitProjectDependencyParser} class.
 * @author P.J.S. Kools
 */
class BukkitProjectDependencyParserTest extends ProjectDependencyParserTest {
	
	@Override
	public ProjectDependencyParser createProjectDependencyParser() {
		return new BukkitProjectDependencyParser();
	}
}
