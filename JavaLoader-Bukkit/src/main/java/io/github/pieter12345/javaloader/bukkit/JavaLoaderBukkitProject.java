package io.github.pieter12345.javaloader.bukkit;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.JavaProject;

/**
 * JavaLoaderBukkitProject class.
 * This class represents a JavaLoader Bukkit project.
 * A JavaLoader project on a Bukkit server should have a main class which extends from this class.
 * @author P.J.S. Kools
 */
public abstract class JavaLoaderBukkitProject extends JavaLoaderProject implements TabCompleter, CommandExecutor {
	
	// Variables & Constants.
	private JavaLoaderBukkitProjectPlugin plugin = null;
	
	/**
	 * initialize method.
	 * Initializes the JavaLoaderBukkitProject with the given JavaProject and JavaLoaderBukkitProjectPlugin.
	 * @param project - The JavaProject.
	 * @param plugin - The JavaLoaderBukkitProjectPlugin Plugin implementation for the project.
	 */
	protected final void initialize(JavaProject project, JavaLoaderBukkitProjectPlugin plugin) {
		super.initialize(project);
		this.plugin = plugin;
	}
	
	/**
	 * getPlugin method.
	 * @return A Bukkit Plugin implementation for this project.
	 */
	public final JavaLoaderBukkitProjectPlugin getPlugin() {
		return this.plugin;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
		return false;
	}
	
	/**
	 * getCommands method.
	 * @return A list of commands that should be registered on load.
	 */
	public BukkitCommand[] getCommands() {
		return null;
	}
}
