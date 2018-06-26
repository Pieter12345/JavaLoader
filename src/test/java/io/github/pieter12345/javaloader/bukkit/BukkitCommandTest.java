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
	 * Tests that the constructor (with aliases list) properly sets all passed values according to getters.
	 */
	@Test
	void testConstructorAndGetters() {
		
		// Create the BukkitCommand.
		String name = "someCommand";
		String desc = "This is a command for Bukkit.";
		String usageMessage = "Usage: /someCommand";
		String permission = "some.perm.node";
		List<String> aliases = Arrays.asList("someAlias", "someOtherAlias");
		CommandExecutor executor = mock(CommandExecutor.class);
		TabCompleter completer = mock(TabCompleter.class);
		BukkitCommand command = new BukkitCommand(name, desc, usageMessage, permission, aliases, executor, completer);
		
		// Assert that the getters return the set values.
		assertThat(command.getName()).isEqualTo(name.toLowerCase(Locale.ENGLISH)); // Name gets lowercased.
		assertThat(command.getDescription()).isEqualTo(desc);
		assertThat(command.getUsageMessage()).isEqualTo(usageMessage);
		assertThat(command.getPermission()).isEqualTo(permission);
		assertThat(command.getAliases()).containsExactlyInAnyOrder(aliases.toArray(new String[0]));
		assertThat(command.getExecutor()).isSameAs(executor);
		assertThat(command.getTabCompleter()).isSameAs(completer);
		
	}
	
	/**
	 * Tests that the constructor (with aliases array) properly sets all passed values according to getters.
	 */
	@Test
	void testConstructorAndGetters2() {
		
		// Create the BukkitCommand.
		String name = "someCommand";
		String desc = "This is a command for Bukkit.";
		String usageMessage = "Usage: /someCommand";
		String permission = "some.perm.node";
		String[] aliases = new String[] {"someAlias", "someOtherAlias"};
		CommandExecutor executor = mock(CommandExecutor.class);
		TabCompleter completer = mock(TabCompleter.class);
		BukkitCommand command = new BukkitCommand(name, desc, usageMessage, permission, aliases, executor, completer);
		
		// Assert that the getters return the set values.
		assertThat(command.getName()).isEqualTo(name.toLowerCase(Locale.ENGLISH)); // Name gets lowercased.
		assertThat(command.getDescription()).isEqualTo(desc);
		assertThat(command.getUsageMessage()).isEqualTo(usageMessage);
		assertThat(command.getPermission()).isEqualTo(permission);
		assertThat(command.getAliases()).containsExactlyInAnyOrder(aliases);
		assertThat(command.getExecutor()).isSameAs(executor);
		assertThat(command.getTabCompleter()).isSameAs(completer);
		
	}
	
	/**
	 * Tests that the constructor sets all values as expected when allowed null arguments are passed.
	 */
	@Test
	void testConstructorAllAllowedNull() {
		
		// Create the BukkitCommand.
		String name = "someCommand";
		BukkitCommand command = new BukkitCommand(name, null, null, null, (List<String>) null, null, null);
		
		// Assert that the getters return the set values.
		assertThat(command.getName()).isEqualTo(name.toLowerCase(Locale.ENGLISH)); // Name gets lowercased.
		assertThat(command.getDescription()).isNull();
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
		assertThrows(NullPointerException.class,
				() -> new BukkitCommand(null, "", "", "", (List<String>) null, null, null));
		
	}
	
	
	
}
