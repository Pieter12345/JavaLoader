package io.github.pieter12345.javaloader.bukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.pieter12345.javaloader.bukkit.dependency.BukkitProjectDependencyParser;
import io.github.pieter12345.javaloader.core.CommandExecutor;
import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.ProjectStateListener;
import io.github.pieter12345.javaloader.core.CommandExecutor.CommandSender;
import io.github.pieter12345.javaloader.core.CommandExecutor.MessageType;
import io.github.pieter12345.javaloader.core.ProjectManager.LoadAllResult;
import io.github.pieter12345.javaloader.core.exceptions.DuplicateProjectIdentifierException;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.utils.AnsiColor;
import io.github.pieter12345.javaloader.core.utils.ReflectionUtils;
import io.github.pieter12345.javaloader.core.utils.Utils;
import io.github.pieter12345.javaloader.core.utils.ReflectionUtils.Argument;

/**
 * JavaLoaderBukkitPlugin class.
 * This is the main class that will be loaded by Bukkit.
 * @author P.J.S. Kools
 * @since 05-01-2017
 */
public class JavaLoaderBukkitPlugin extends JavaPlugin {
	
	// Variables & Constants.
	private static final String PREFIX_INFO =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.RED + " ";
	private final Logger logger;
	
	private static final int COMPILER_FEEDBACK_LIMIT = 5; // The max amount of warnings/errors to print per recompile.
	
	private ProjectManager projectManager;
	private final File projectsDir = new File(this.getDataFolder().getAbsoluteFile(), "JavaProjects");
	private ProjectStateListener projectStateListener;
	
	private CommandExecutor commandExecutor;
	
	private Set<String> commandSyncCheckRequired; // Projects where injected commands might need sync with clients.
	private Map<String, Set<Command>> injectedCommandsMap;
	private Map<String, Set<Command>> syncedCommandsMap;
	private boolean commandSyncErrored = false;
	
	public JavaLoaderBukkitPlugin() {
		// This runs when Bukkit creates JavaLoader. Use onEnable() for initialization on enable instead.
		
		// Create a logger that adds the plugin name as a colorized name tag and converts Minecraft colorcodes to ANSI.
		this.logger = new Logger(JavaLoaderBukkitPlugin.class.getCanonicalName(), null) {
			private final String prefix = JavaLoaderBukkitPlugin.this.getDescription().getPrefix();
			private final String pluginName = ChatColor.GOLD + "[" + ChatColor.DARK_AQUA
					+ (this.prefix != null ? this.prefix : JavaLoaderBukkitPlugin.this.getDescription().getName())
					+ ChatColor.GOLD + "] ";
			
			@Override
			public void log(LogRecord logRecord) {
				Level logLevel = logRecord.getLevel();
				ChatColor textColor = (logLevel.equals(Level.SEVERE) ? ChatColor.RED
						: (logLevel.equals(Level.WARNING) ? ChatColor.GOLD : ChatColor.GREEN));
				logRecord.setMessage(AnsiColor.colorize(this.pluginName
						+ textColor + logRecord.getMessage() + ChatColor.RESET, ChatColor.COLOR_CHAR));
				super.log(logRecord);
			}
		};
		this.logger.setParent(this.getServer().getLogger());
		this.logger.setLevel(Level.ALL);
	}
	
	@Override
	public void onEnable() {
		
		// Check if a JDK is available, otherwise disable the plugin.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			this.logger.severe("No java compiler available."
					+ " This plugin requires the server to be started from a JDK version (Java Development Kit),"
					+ " rather than a JRE version (Java Runtime Environment). Disabling plugin.");
			this.setEnabled(false);
			return;
		}
		
		// Ensure that the "/plugins/JavaLoader" directory exists.
		this.getDataFolder().mkdirs();
		
		// Create the "/plugins/JavaLoader/JavaProjects" directory if it doesn't exist
		// and initialize it with an example.
		if(!this.projectsDir.exists()) {
			this.projectsDir.mkdir();
			this.createExampleProject();
		}
		
		// Create the project manager.
		this.projectManager = new ProjectManager(this.projectsDir, new BukkitProjectDependencyParser());
		
		// Initialize injected and synced commands set.
		this.injectedCommandsMap = new HashMap<String, Set<Command>>();
		this.syncedCommandsMap = new HashMap<String, Set<Command>>();
		this.commandSyncCheckRequired = new HashSet<String>();
		
		// Initialize project state listener.
		this.projectStateListener = new ProjectStateListener() {
			
			@Override
			public void onLoad(JavaProject project) throws LoadException {
				
				// Initialize the project instance class with the JavaPlugin.
				// Add a Bukkit Plugin implementation for JavaLoaderBukkitProjects.
				if(project.getInstance() instanceof JavaLoaderBukkitProject) {
					JavaLoaderBukkitProject bukkitProjectInstance = (JavaLoaderBukkitProject) project.getInstance();
					JavaLoaderBukkitProjectPlugin bukkitProjectPlugin = new JavaLoaderBukkitProjectPlugin(project);
					bukkitProjectInstance.initialize(project, bukkitProjectPlugin);
					bukkitProjectPlugin.setEnabled(true); // Sync Bukkit Plugin state.
					
					// Register commands used by the project.
					BukkitCommand[] commands;
					try {
						commands = bukkitProjectInstance.getCommands();
					} catch (Throwable e) {
						throw new LoadException(project, "A problem occurred in " + project.getName() + "'s "
								+ bukkitProjectInstance.getClass().getName() + ".getCommands()."
								+ " Is the project up to date?  Stacktrace:\n" + Utils.getStacktrace(e));
					}
					if(commands != null && commands.length != 0) {
						for(BukkitCommand command : commands) {
							if(command == null) {
								throw new LoadException(project, "The BukkitCommand array returned by the "
										+ bukkitProjectInstance.getClass().getName() + ".getCommands() method"
										+ " must not contain null values.");
							}
						}
						try {
							this.injectCommands(bukkitProjectPlugin, commands);
						} catch (Exception e) {
							throw new LoadException(project, "An Exception occurred while injecting "
									+ project.getName() + "'s commands into Bukkit. This means that JavaLoader is not"
									+ " fully compatible with the current version of Bukkit. This is a bug in"
									+ " JavaLoader, but you might be able to prevent this from triggering by altering"
									+ " what your " + bukkitProjectInstance.getClass().getName() + ".getCommands()"
									+ " method returns. Stacktrace:\n" + Utils.getStacktrace(e));
						}
					}
					
				} else {
					project.getInstance().initialize(project);
				}
			}
			
			@Override
			public void onUnload(JavaProject project) throws UnloadException {
				if(project.getInstance() instanceof JavaLoaderBukkitProject) {
					JavaLoaderBukkitProject bukkitProjectInstance = (JavaLoaderBukkitProject) project.getInstance();
					bukkitProjectInstance.getPlugin().setEnabled(false); // Sync Bukkit Plugin state.
					try {
						this.uninjectCommands(bukkitProjectInstance.getPlugin());
					} catch (Exception e) {
						throw new UnloadException(project, "An Exception occurred while uninjecting "
								+ project.getName() + "'s commands from Bukkit. This means that JavaLoader is not"
								+ " fully compatible with the current version of Bukkit. This is a bug in JavaLoader,"
								+ " but you might be able to prevent this from triggering by altering what your "
								+ bukkitProjectInstance.getClass().getName() + ".getCommands() method returns."
								+ " Stacktrace:\n" + Utils.getStacktrace(e));
					}
				}
			}
			
			private void injectCommands(JavaLoaderBukkitProjectPlugin bukkitProjectPlugin, BukkitCommand[] commands)
					throws SecurityException, IllegalAccessException, NoSuchFieldException,
					NoSuchMethodException, InstantiationException, InvocationTargetException {
				
				// Return if no commands have to be registered.
				if(commands == null || commands.length == 0) {
					return;
				}
				
				// Get the CommandMap from the SimplePluginManager.
				final PluginManager pluginManager = Bukkit.getPluginManager();
				if(!(pluginManager instanceof SimplePluginManager)) {
					throw new RuntimeException("Could not inject Bukkit commands because Bukkit.getPluginManager()"
							+ " was not an instance of " + SimplePluginManager.class.getName());
				}
				final SimpleCommandMap cmdMap = (SimpleCommandMap) ReflectionUtils.getField(
						SimplePluginManager.class, "commandMap", (SimplePluginManager) pluginManager);
				
				// Get the currently known commands.
				@SuppressWarnings("unchecked")
				Map<String, Command> knownCommands = (Map<String, Command>) ReflectionUtils.getField(
							SimpleCommandMap.class, "knownCommands", cmdMap);
				
				// Inject the commands, unregistering commands that will be overwritten.
				for(BukkitCommand command : commands) {
					
					// Generate the new command.
					final PluginCommand bukkitCmd = ReflectionUtils.newInstance(PluginCommand.class,
								new Argument<String>(String.class, command.getName()),
								new Argument<Plugin>(Plugin.class, bukkitProjectPlugin));
					bukkitCmd.setDescription(command.getDescription());
					String usageMessage = command.getUsageMessage();
					bukkitCmd.setUsage(usageMessage == null ? "" : usageMessage); // Bukkit's default is empty string.
					bukkitCmd.setPermission(command.getPermission());
					bukkitCmd.setPermissionMessage(command.getPermissionMessage());
					bukkitCmd.setAliases(command.getAliases());
					bukkitCmd.setExecutor(command.getExecutor());
					bukkitCmd.setTabCompleter(command.getTabCompleter());
					
					// Unregister the command if it already exists.
					Command removedCmd = knownCommands.remove(command.getName());
					if(removedCmd != null) {
						removedCmd.unregister(cmdMap);
					}
					
					// Register the new command.
					if(!cmdMap.register(JavaLoaderBukkitPlugin.this.getDescription().getName(), bukkitCmd)) {
						// Throw an Exception as this should never happen since the command is unregistered above.
						throw new RuntimeException("Could not inject command because the command was already"
								+ " registered and failed to unregister."
								+ " The command that failed to register was: \"/" + bukkitCmd.getName() + "\"");
					}
					
					// Update injected commands set and set command sync check flag.
					String pluginName = bukkitProjectPlugin.getName();
					Set<Command> injectedCommands = JavaLoaderBukkitPlugin.this.injectedCommandsMap.get(pluginName);
					if(injectedCommands == null) {
						injectedCommands = new HashSet<Command>();
						JavaLoaderBukkitPlugin.this.injectedCommandsMap.put(pluginName, injectedCommands);
					}
					injectedCommands.add(bukkitCmd);
					JavaLoaderBukkitPlugin.this.commandSyncCheckRequired.add(bukkitProjectPlugin.getName());
				}
			}
			
			private void uninjectCommands(JavaLoaderBukkitProjectPlugin plugin)
					throws SecurityException, IllegalAccessException, NoSuchFieldException {
				
				// Get the CommandMap.
				final PluginManager pluginManager = Bukkit.getPluginManager();
				if(!(pluginManager instanceof SimplePluginManager)) {
					throw new RuntimeException("Could not uninject Bukkit commands because Bukkit.getPluginManager()"
							+ " was not an instance of " + SimplePluginManager.class.getName());
				}
				final SimpleCommandMap cmdMap = (SimpleCommandMap) ReflectionUtils.getField(
						SimplePluginManager.class, "commandMap", (SimplePluginManager) pluginManager);
				
				// Get the currently known commands.
				@SuppressWarnings("unchecked")
				Map<String, Command> knownCommands = (Map<String, Command>) ReflectionUtils.getField(
							SimpleCommandMap.class, "knownCommands", cmdMap);
				
				// Unregister all commands owned by the given plugin.
				Iterator<Command> it = knownCommands.values().iterator();
				while(it.hasNext()) {
					Command command = it.next();
					if(command instanceof PluginCommand && ((PluginCommand) command).getPlugin() == plugin) {
						command.unregister(cmdMap);
						it.remove();
						
						// Update injected commands set and set command sync check flag.
						// Commands that were injected for this plugin through some custom method are simply ignored.
						String pluginName = plugin.getName();
						Set<Command> injectedCommands = JavaLoaderBukkitPlugin.this.injectedCommandsMap.get(pluginName);
						if(injectedCommands != null) {
							if(injectedCommands.remove(command)) {
								JavaLoaderBukkitPlugin.this.commandSyncCheckRequired.add(plugin.getName());
							}
						}
					}
				}
				
			}
		};
		
		// Initialize the command executor.
		this.commandExecutor = new CommandExecutor(this.projectManager, this.projectStateListener, null, "/javaloader",
				this.getDescription().getAuthors(), this.getDescription().getVersion(),
				(String str) -> colorize(str), COMPILER_FEEDBACK_LIMIT);
		
		// Loop over all project directories and add them as a JavaProject.
		this.projectManager.addProjectsFromProjectDirectory(
				this.projectStateListener, (DuplicateProjectIdentifierException ex) -> {
			this.logger.severe("Multiple projects found with name \"" + ex.getProjectName() + "\" at: "
					+ Utils.glueIterable(ex.getProjectDirs(), (File f) -> f.getAbsolutePath(), ", "));
		});
		
		// Load all projects.
		LoadAllResult loadAllResult = this.projectManager.loadAllProjects((LoadException ex) -> {
			this.logger.severe("A LoadException occurred while loading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		
		// Print feedback.
		JavaProject[] projects = this.projectManager.getProjects();
		this.logger.info("JavaLoader " + this.getDescription().getVersion() + " enabled. "
				+ loadAllResult.loadedProjects.size() + "/" + projects.length + " projects loaded.");
		
		// Command sync is not required here since Bukkit does this in a later startup stage.
		for(Entry<String, Set<Command>> entry : this.injectedCommandsMap.entrySet()) {
			this.syncedCommandsMap.put(entry.getKey(), new HashSet<Command>(entry.getValue()));
		}
		this.commandSyncCheckRequired.clear();
	}
	
	@Override
	public void onDisable() {
		
		// Unload all loaded projects and remove them from the project manager.
		if(this.projectManager != null) {
			this.projectManager.clear((UnloadException ex) -> {
				this.logger.severe("An UnloadException occurred while unloading"
						+ " java project \"" + ex.getProject().getName() + "\":"
						+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
			});
		}
		this.projectManager = null;
		this.injectedCommandsMap = null;
		this.syncedCommandsMap = null;
		this.commandSyncCheckRequired = null;
		this.projectStateListener = null;
		this.commandExecutor = null;
		
		// Print feedback.
		this.logger.info("JavaLoader " + this.getDescription().getVersion() + " disabled.");
	}
	
	@Override
	public boolean onCommand(final org.bukkit.command.CommandSender sender, Command cmd, String label, String[] args) {
		
		// Check if the plugin is enabled and validate the command prefix.
		if(!this.isEnabled() || !cmd.getName().equalsIgnoreCase("javaloader")) {
			return false;
		}
		
		// Execute the command.
		this.commandExecutor.executeCommand(new CommandSender() {
			@Override
			public void sendMessage(MessageType messageType, String message) {
				sender.sendMessage(this.getPrefix(messageType) + message);
			}
			@Override
			public void sendMessage(MessageType messageType, String... messages) {
				if(messages.length > 0) {
					messages[0] = this.getPrefix(messageType) + messages[0];
					sender.sendMessage(messages);
				}
			}
			public String getPrefix(MessageType messageType) {
				switch(messageType) {
					case ERROR:
						return PREFIX_ERROR;
					case INFO:
						return PREFIX_INFO;
					default:
						throw new Error(
								"Unimplemented " + MessageType.class.getSimpleName() + ": " + messageType.name());
				}
			}
		}, args);
		
		// Sync injected commands with clients if necessary.
		for(String project : this.commandSyncCheckRequired) {
			Set<Command> injectedCommands = this.injectedCommandsMap.get(project);
			Set<Command> syncedCommands = this.syncedCommandsMap.get(project);
			if(!commandSetEquals(injectedCommands, syncedCommands)) {
				this.syncCommands();
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether two {@link Command} sets are equal in the sense that their name, aliases and permission matches.
	 * @param set1
	 * @param set2
	 * @return {@code true} if the sets are considered equal, {@code} false otherwise.
	 */
	private static boolean commandSetEquals(Set<Command> set1, Set<Command> set2) {
		if(set1 == set2) {
			return true;
		}
		if(set1 == null || set2 == null || set1.size() != set2.size()) {
			return false;
		}
		for(Command cmd1 : set1) {
			for(Command cmd2 : set2) {
				if(cmd1.getName().equals(cmd2.getName())) {
					if(!cmd1.getAliases().equals(cmd2.getAliases())
							|| (cmd1.getPermission() != cmd2.getPermission() && cmd1.getPermission() != null
									&& !cmd1.getPermission().equals(cmd2.getPermission()))) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Synchronizes the commands known by Bukkit and the commands known by clients by calling the
	 * {@code CraftServer.syncCommands()} method through reflection.
	 */
	private void syncCommands() {
		Server server = Bukkit.getServer();
		try {
			Method syncCommandsMethod = server.getClass().getDeclaredMethod("syncCommands");
			syncCommandsMethod.setAccessible(true);
			syncCommandsMethod.invoke(server);
			this.commandSyncErrored = false;
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if(!this.commandSyncErrored) {
				this.logger.warning("Command synchronization failed."
						+ " This means that command and tab-completion might not work or update correctly for clients."
						+ " This warning will be disabled until the server is restarted or a successful sync happens."
						+ " Here's the stacktrace:\n" + Utils.getStacktrace(e));
				this.commandSyncErrored = true;
			}
			return;
		}
		
		this.syncedCommandsMap.clear();
		for(Entry<String, Set<Command>> entry : this.injectedCommandsMap.entrySet()) {
			this.syncedCommandsMap.put(entry.getKey(), new HashSet<Command>(entry.getValue()));
		}
		this.commandSyncCheckRequired.clear();
	}
	
	@Override
	public List<String> onTabComplete(
			org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
		if(command.getName().equals("javaloader")) {
			String search = args[args.length - 1].toLowerCase();
			
			// TAB-complete "/javaloader <arg>".
			if(args.length == 1) {
				List<String> ret = new ArrayList<String>();
				for(String comp : new String[] {"help", "list", "load", "unload", "recompile"}) {
					if(comp.startsWith(search)) {
						ret.add(comp);
					}
				}
				return ret;
			}
			
			// TAB-complete "/javaloader <load, unload, recompile> <arg>".
			if(args.length == 2 && (args[0].equalsIgnoreCase("load")
					|| args[0].equalsIgnoreCase("unload") || args[0].equalsIgnoreCase("recompile"))) {
				List<String> ret = new ArrayList<String>();
				for(String comp : this.projectManager.getProjectNames()) {
					if(comp.toLowerCase().startsWith(search)) {
						ret.add(comp);
					}
				}
				return ret;
			}
			
			// TAB-complete "/javaloader help <arg>".
			if(args.length == 2 && args[0].equalsIgnoreCase("help")) {
				List<String> ret = new ArrayList<String>();
				for(String comp : new String[] {"help", "list", "recompile", "load", "unload"}) {
					if(comp.toLowerCase().startsWith(search)) {
						ret.add(comp);
					}
				}
				return ret;
			}
			
			// Subcommand without tabcompleter.
			return Collections.emptyList();
		}
		return null;
	}
	
	/**
	 * Colorizes the given string by replacing color char '&' by {@link ChatColor#COLOR_CHAR} for
	 * color idenfitiers 0-9a-fA-F.
	 * @param str - The string to colorize.
	 * @return The colorized string.
	 */
	private static String colorize(String str) {
		return str.replaceAll("(?<!\\&)\\&(?=[0-9a-fA-F])", ChatColor.COLOR_CHAR + "");
	}
	
	private void createExampleProject() {
		try {
			CodeSource codeSource = JavaLoaderBukkitPlugin.class.getProtectionDomain().getCodeSource();
			if(codeSource != null) {
				
				// Select a source to copy from (directory (IDE) or jar (production)).
				if(codeSource.getLocation().getPath().endsWith("/")) {
					
					// The code is being ran from a non-jar source. Get the projects base directory.
					File exampleProjectsBaseDir = new File(
							URLDecoder.decode(codeSource.getLocation().getPath(), "UTF-8")
							+ "exampleprojects/bukkit");
					
					// Validate that the projects base directory exists.
					if(!exampleProjectsBaseDir.isDirectory()) {
						throw new FileNotFoundException("Example projects base directory not found at: "
								+ exampleProjectsBaseDir.getAbsolutePath());
					}
					
					// Copy all example projects into the projects directory.
					for(File exampleProjectFile : exampleProjectsBaseDir.listFiles()) {
						Utils.copyFile(exampleProjectFile, this.projectsDir);
					}
					
				} else {
					
					// Copy the example projects from the jar.
					ZipInputStream inStream = new ZipInputStream(codeSource.getLocation().openStream());
					
					ZipEntry zipEntry;
					while((zipEntry = inStream.getNextEntry()) != null) {
						String name = zipEntry.getName();
						if(name.startsWith("exampleprojects/bukkit/")) {
							File targetFile = new File(this.projectsDir,
									name.substring("exampleprojects/bukkit/".length()));
							if(name.endsWith("/")) {
								targetFile.mkdir();
							} else {
								FileOutputStream outStream = new FileOutputStream(targetFile);
								
								int amount = 0;
								byte[] buffer = new byte[1024];
								while((amount = inStream.read(buffer)) != -1) {
									outStream.write(buffer, 0, amount);
								}
								outStream.close();
							}
						}
					}
					inStream.close();
				}
			}
		} catch (Exception e) {
			this.logger.severe("Failed to create example projects."
					+ " Here's the stacktrace:\n" + Utils.getStacktrace(e));
			return;
		}
	}
	
	/**
	 * getProject method.
	 * @param name - The name of the JavaLoader project.
	 * @return The JavaLoaderProject instance or null if no project with the given name exists.
	 */
	public JavaLoaderProject getProject(String name) {
		return this.projectManager.getProjectInstance(name);
	}
	
	/**
	 * getProjects method.
	 * @return An array containing all loaded JavaLoader project instances.
	 */
	public JavaLoaderProject[] getProjects() {
		return this.projectManager.getProjectInstances();
	}
	
	/**
	 * getProjectNames method.
	 * @return An array containing all loaded JavaLoader project names.
	 */
	public String[] getProjectNames() {
		return this.projectManager.getProjectNames();
	}
	
	/**
	 * getPlugin method.
	 * WARNING: Calls to this method from within a static code block or constructor in a JavaLoaderBukkitProject child
	 *  class might return null. It is advised to only call this method after the onLoad() method has been called.
	 * @param project - The JavaProject to get the Plugin for.
	 * @return The JavaLoaderBukkitProjectPlugin instance for this JavaLoader project.
	 */
	public JavaLoaderBukkitProjectPlugin getPlugin(JavaProject project) {
		if(project.getInstance() instanceof JavaLoaderBukkitProject) {
			return ((JavaLoaderBukkitProject) project.getInstance()).getPlugin();
		}
		return null;
	}
	
	/**
	 * getPlugin method.
	 * WARNING: Calls to this method from within a static code block or constructor in a JavaLoaderBukkitProject child
	 *  class might return null. It is advised to only call this method after the onLoad() method has been called.
	 * @param projectName - The name of the JavaLoader project.
	 * @return The JavaLoaderBukkitProjectPlugin instance for this JavaLoader project.
	 */
	public JavaLoaderBukkitProjectPlugin getPlugin(String projectName) {
		JavaProject project = this.projectManager.getProject(projectName);
		return (project == null ? null : this.getPlugin(project));
	}
}
