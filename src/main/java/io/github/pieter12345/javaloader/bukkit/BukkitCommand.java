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
	private String description;
	private String usageMessage;
	private String permission;
	private String permissionMessage;
	private List<String> aliases;
	
	private CommandExecutor executor;
	private TabCompleter completer;
	
	/**
	 * Creates a new BukkitCommand with the given name.
	 * @param name - The name of the command ("mycommand" would be triggered by "/mycommand" in-game).
	 * @throws NullPointerException When name is null.
	 */
	public BukkitCommand(String name) throws NullPointerException {
		Objects.requireNonNull(name, "Name must not be null.");
		this.name = name.toLowerCase(Locale.ENGLISH);
		this.description = "";
		this.usageMessage = null;
		this.permission = null;
		this.permissionMessage = null;
		this.aliases = new ArrayList<String>();
		
		this.executor = null;
		this.completer = null;
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
	 * @param aliases - A list of aliases for the command.
	 * Supplying an empty list or null will cause no aliases to be set.
	 * @param executor - The CommandExecutor which will be used to handle the command.
	 * Supplying null will cause no command executor to be set.
	 * @param completer - The TabCompleter which will be used to handle TAB-completions for the command.
	 * Supplying null will cause no tabcompleter to be set.
	 * @throws NullPointerException When name is null.
	 * @deprecated Use {@link BukkitCommand#BukkitCommand(String)} with setters for the parameters.
	 */
	@Deprecated
	public BukkitCommand(String name, String description, String usageMessage, String permission,
			List<String> aliases, CommandExecutor executor, TabCompleter completer) throws NullPointerException {
		Objects.requireNonNull(name, "Name must not be null.");
		this.name = name.toLowerCase(Locale.ENGLISH);
		this.description = (description == null ? "" : description);
		this.usageMessage = usageMessage;
		this.permission = permission;
		this.permissionMessage = null;
		this.aliases = (aliases == null ? new ArrayList<String>() : new ArrayList<String>(aliases));
		
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
	 * @deprecated Use {@link BukkitCommand#BukkitCommand(String)} with setters for the parameters.
	 */
	@Deprecated
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
	 * setDescription method.
	 * @param description - The command description or null to supply no description.
	 * @return This instance.
	 */
	public BukkitCommand setDescription(String description) {
		Objects.requireNonNull(description, "Description must not be null.");
		this.description = description;
		return this;
	}
	
	/**
	 * getUsageMessage method.
	 * @return The command usage message.
	 */
	public String getUsageMessage() {
		return this.usageMessage;
	}
	
	/**
	 * setUsageMessage method.
	 * @param message - The command usage message.
	 * Supplying an empty string or null will cause no usage message to be set.
	 * @return This instance.
	 */
	public BukkitCommand setUsageMessage(String message) {
		this.usageMessage = message;
		return this;
	}
	
	/**
	 * getPermission method.
	 * @return The permission required to use the command.
	 */
	public String getPermission() {
		return this.permission;
	}
	
	/**
	 * setPermission method.
	 * @param message - The required permission to use this command.
	 * Supplying an empty string or null will cause everyone to have permission to execute this command.
	 * @return This instance.
	 */
	public BukkitCommand setPermission(String permission) {
		this.permission = permission;
		return this;
	}
	
	/**
	 * getPermissionMessage method.
	 * @return The no-permission message of this command.
	 */
	public String getPermissionMessage() {
		return this.permissionMessage;
	}
	
	/**
	 * setPermission method.
	 * @param message - The no-permission message of this command.
	 * Supplying null will cause the default no-permission message to be used. Supplying an empty string will suppress
	 * the no-permission message.
	 * @return This instance.
	 */
	public BukkitCommand setPermissionMessage(String message) {
		this.permissionMessage = message;
		return this;
	}
	
	/**
	 * getAliases method.
	 * @return A list (clone) containing all command aliases.
	 */
	public List<String> getAliases() {
		return new ArrayList<String>(this.aliases);
	}
	
	/**
	 * setAliases method.
	 * @param aliases - An array of aliases for the command.
	 * Supplying an empty array will cause no aliases to be set.
	 * @return This instance.
	 * @throws NullPointerException If aliases or one of its elements is null.
	 */
	public BukkitCommand setAliases(List<String> aliases) throws NullPointerException {
		Objects.requireNonNull(aliases, "Aliases must not be null.");
		for(String alias : aliases) {
			Objects.requireNonNull(alias, "All elements in aliases must not be null.");
		}
		this.aliases = new ArrayList<String>(aliases);
		return this;
	}
	
	/**
	 * setAliases method.
	 * @param aliases - An array of aliases for the command.
	 * Supplying an empty array will cause no aliases to be set.
	 * @return This instance.
	 * @throws NullPointerException If aliases or one of its elements is null.
	 */
	public BukkitCommand setAliases(String... aliases) throws NullPointerException {
		Objects.requireNonNull(aliases, "Aliases must not be null.");
		this.setAliases(Arrays.asList(aliases));
		return this;
	}
	
	/**
	 * addAlias method.
	 * @param alias - The command alias to add.
	 * @return This instance.
	 * @throws NullPointerException If alias is null.
	 */
	public BukkitCommand addAlias(String alias) throws NullPointerException {
		Objects.requireNonNull(alias, "Alias must not be null.");
		this.aliases.add(alias);
		return this;
	}
	
	/**
	 * getExecutor method.
	 * @return The command executor.
	 */
	public CommandExecutor getExecutor() {
		return this.executor;
	}
	
	/**
	 * setExecutor method.
	 * @param executor - The CommandExecutor which will be used to handle the command.
	 * Supplying null will cause no command executor to be set.
	 * @return This instance.
	 */
	public BukkitCommand setExecutor(CommandExecutor executor) {
		this.executor = executor;
		return this;
	}
	
	/**
	 * getTabCompleter method.
	 * @return The command tabcompleter.
	 */
	public TabCompleter getTabCompleter() {
		return this.completer;
	}
	
	/**
	 * setTabCompleter method.
	 * @param completer - The TabCompleter which will be used to handle TAB-completions for the command.
	 * Supplying null will cause no tabcompleter to be set.
	 * @return This instance.
	 */
	public BukkitCommand setTabCompleter(TabCompleter completer) {
		this.completer = completer;
		return this;
	}
}
