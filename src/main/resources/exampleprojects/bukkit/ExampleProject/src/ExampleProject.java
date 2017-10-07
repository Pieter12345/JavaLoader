import io.github.pieter12345.javaloader.bukkit.JavaLoaderBukkitProject;
import io.github.pieter12345.javaloader.bukkit.BukkitCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * ExampleProject class.
 * This is an example project for the JavaLoader plugin.
 * Its purpose is to show how to create a project for the JavaLoader plugin.
 * @author P.J.S. Kools
 */
public class ExampleProject extends JavaLoaderBukkitProject {
	
	@Override
	public void onLoad() {
		
		// Register an event listener.
		this.getPlugin().getServer().getPluginManager().registerEvents(new Listener() {
			
			// Send a welcome message to players on join.
			@EventHandler
			public void playerJoinEvent(PlayerJoinEvent event) {
				event.getPlayer().sendMessage("[JavaLoader ExampleProject] Welcome to the server. This message can be"
						+ " configured in JavaLoaders ExampleProject.");
			}
			
		}, this.getPlugin());
		
		// Print feedback.
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "[DEBUG] ExampleProject project loaded." + ChatColor.RESET);
	}
	
	@Override
	public void onUnload() {
		
		// Unregister all listeners from this project.
		HandlerList.unregisterAll(this.getPlugin());
		
		// Print feedback.
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[DEBUG] ExampleProject project unloaded." + ChatColor.RESET);
	}
	
	@Override
	public String getVersion() {
		return "0.0.1-SNAPSHOT";
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
		
		// Validate command prefix.
		if(!command.getName().equalsIgnoreCase("examplecommand")) {
			return false;
		}
		
		// Send some message.
		sender.sendMessage("[DEBUG] [ExampleProject] onCommand() called for command: /" + command.getName());
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return null;
	}
	
	@Override
	public BukkitCommand[] getCommands() {
		return new BukkitCommand[] {
				new BukkitCommand("examplecommand", "An example command, defined in JavaLoader's example project.", "Usage: /examplecommand.",
						"javaloader.exampleproject.examplecommand", new String[] {"examplecmd"}, this, this)
			};
	}
}