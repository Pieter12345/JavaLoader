package io.github.pieter12345.javaloader.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

/**
 * BukkitCommand class.
 * Represents a command on a Bukkit server.
 * @author P.J.S. Kools
 */
public final class BukkitCommand {
	
	// Variables & Constants.
	private final String name;
	private final String description;
	private final String usageMessage;
	private final String permission;
	private final List<String> aliases;
	
	private final CommandExecutor executor;
	private final TabCompleter completer;
	
	/**
	 * Constructor.
	 * Creates a new BukkitCommand with the given parameters.
	 * @param name - The name of the command ("mycommand" would be triggered by "/mycommand" in-game).
	 * @param description - The command description or null to supply no description.
	 * @param usageMessage - The command usage message.
	 * Supplying an empty string or null will cause no usage message to be set.
	 * @param permission - The required permission to use this command.
	 * Supplying an empty string or null will cause everyone to have permission to execute this command.
	 * @param aliases - A list of aliases for the command.
	 * Supplying an empty list or null will cause no aliases to be set.
	 * @param executor - The CommandExecutor which will be used to handle the command.
	 * Supplying null will cause no command executor to be set.
	 * @param completer - The TabCompleter which will be used to handle TAB-completions for the command.
	 * Supplying null will cause no tabcompleter to be set.
	 * @throws NullPointerException When name is null.
	 */
	public BukkitCommand(String name, String description, String usageMessage, String permission,
			List<String> aliases, CommandExecutor executor, TabCompleter completer) throws NullPointerException {
		Objects.requireNonNull(name, "Name may not be null.");
		this.name = name.toLowerCase(Locale.ENGLISH);
		this.description = description;
		this.usageMessage = usageMessage;
		this.permission = permission;
		this.aliases = (aliases == null ? new ArrayList<String>() : aliases);
		
		this.executor = executor;
		this.completer = completer;
	}
	
	/**
	 * Constructor.
	 * Creates a new BukkitCommand with the given parameters.
	 * @param name - The name of the command ("mycommand" would be triggered by "/mycommand" in-game).
	 * @param description - The command description or null to supply no description.
	 * @param usageMessage - The command usage message.
	 * Supplying an empty string or null will cause no usage message to be set.
	 * @param permission - The required permission to use this command.
	 * Supplying an empty string or null will cause everyone to have permission to execute this command.
	 * @param aliases - An array of aliases for the command.
	 * Supplying an empty array or null will cause no aliases to be set.
	 * @param executor - The CommandExecutor which will be used to handle the command.
	 * Supplying null will cause no command executor to be set.
	 * @param completer - The TabCompleter which will be used to handle TAB-completions for the command.
	 * Supplying null will cause no tabcompleter to be set.
	 * @throws NullPointerException When name is null.
	 */
	public BukkitCommand(String name, String description, String usageMessage, String permission,
			String[] aliases, CommandExecutor executor, TabCompleter completer) throws NullPointerException {
		this(name, description, usageMessage, permission,
				(aliases == null ? null : Arrays.asList(aliases)), executor, completer);
	}
	
	/**
	 * getName method.
	 * @return The name of the command.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * getDescription method.
	 * @return The command description.
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * getUsageMessage method.
	 * @return The command usage message.
	 */
	public String getUsageMessage() {
		return this.usageMessage;
	}
	
	/**
	 * getPermission method.
	 * @return The permission required to use the command.
	 */
	public String getPermission() {
		return this.permission;
	}
	
	/**
	 * getAliases method.
	 * @return A list (clone) containing all command aliases.
	 */
	public List<String> getAliases() {
		return new ArrayList<String>(this.aliases);
	}
	
	/**
	 * getExecutor method.
	 * @return The command executor.
	 */
	public CommandExecutor getExecutor() {
		return this.executor;
	}
	
	/**
	 * getTabCompleter method.
	 * @return The command tabcompleter.
	 */
	public TabCompleter getTabCompleter() {
		return this.completer;
	}
}
