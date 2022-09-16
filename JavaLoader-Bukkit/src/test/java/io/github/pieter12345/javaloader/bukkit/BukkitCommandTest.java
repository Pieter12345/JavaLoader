package io.github.pieter12345.javaloader.bukkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link BukkitCommand} class.
 * @author P.J.S. Kools
 */
class BukkitCommandTest {
	
	/**
	 * Tests BukkitCommand(String) constructor, setters and getters.
	 */
	@Test
	void testSettersAndGetters() {
		
		// Create the BukkitCommand.
		String name = "someCommand";
		String desc = "This is a command for Bukkit.";
		String usageMessage = "Usage: /someCommand";
		String permission = "some.perm.node";
		String permissionMessage = "No permission!";
		List<String> aliases = Arrays.asList("someAlias", "someOtherAlias");
		CommandExecutor executor = mock(CommandExecutor.class);
		TabCompleter completer = mock(TabCompleter.class);
		BukkitCommand command = new BukkitCommand(name).setDescription(desc).setUsageMessage(usageMessage)
				.setPermission(permission).setPermissionMessage(permissionMessage).setAliases(aliases)
				.setExecutor(executor).setTabCompleter(completer);
		
		// Assert that the getters return the set values.
		assertThat(command.getName()).isEqualTo(name.toLowerCase(Locale.ENGLISH)); // Name gets lowercased.
		assertThat(command.getDescription()).isEqualTo(desc);
		assertThat(command.getUsageMessage()).isEqualTo(usageMessage);
		assertThat(command.getPermission()).isEqualTo(permission);
		assertThat(command.getPermissionMessage()).isEqualTo(permissionMessage);
		assertThat(command.getAliases()).containsExactlyInAnyOrder(aliases.toArray(new String[0]));
		assertThat(command.getExecutor()).isSameAs(executor);
		assertThat(command.getTabCompleter()).isSameAs(completer);
	}
	
	/**
	 * Tests setAliases(String...).
	 */
	@Test
	void testAliasArraySetter() {
		
		// Create the BukkitCommand.
		BukkitCommand command = new BukkitCommand("someCommand").setAliases("alias1", "alias2");
		
		// Assert that the aliases return the set values.
		assertThat(command.getAliases()).containsExactlyInAnyOrder("alias1", "alias2");
	}
	
	/**
	 * Tests addAlias(String).
	 */
	@Test
	void testAddAlias() {
		
		// Create the BukkitCommand.
		BukkitCommand command = new BukkitCommand("someCommand").setAliases("alias1", "alias2").addAlias("alias3");
		
		// Assert that the aliases return the set values.
		assertThat(command.getAliases()).containsExactlyInAnyOrder("alias1", "alias2", "alias3");
	}
	
	/**
	 * Tests setAliases(String...) and addAlias(String) combined.
	 */
	@Test
	void testSetAndAddAlias() {
		
		// Create the BukkitCommand.
		String alias = "alias";
		BukkitCommand command = new BukkitCommand("someCommand").addAlias(alias);
		
		// Assert that the aliases return the set values.
		assertThat(command.getAliases()).containsExactly(alias);
	}
	
	/**
	 * Tests that the constructor sets all values as expected when allowed null arguments are passed.
	 */
	@Test
	void testConstructorDefaultValues() {
		
		// Create the BukkitCommand.
		String name = "someCommand";
		BukkitCommand command = new BukkitCommand(name);
		
		// Assert that the getters return the default values.
		assertThat(command.getName()).isEqualTo(name.toLowerCase(Locale.ENGLISH)); // Name gets lowercased.
		assertThat(command.getDescription()).isEmpty();
		assertThat(command.getUsageMessage()).isNull();
		assertThat(command.getPermission()).isNull();
		assertThat(command.getAliases()).isEmpty();
		assertThat(command.getExecutor()).isNull();
		assertThat(command.getTabCompleter()).isNull();
	}
	
	/**
	 * Tests that the constructor throws an exception when a null name is passed.
	 */
	@Test
	void testConstructorNameNonNull() {
		assertThrows(NullPointerException.class, () -> new BukkitCommand(null));
	}
}
