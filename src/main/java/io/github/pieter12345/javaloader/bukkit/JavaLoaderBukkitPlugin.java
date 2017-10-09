package io.github.pieter12345.javaloader.bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.pieter12345.javaloader.JavaProject;
import io.github.pieter12345.javaloader.JavaProject.CompileException;
import io.github.pieter12345.javaloader.JavaProject.LoadException;
import io.github.pieter12345.javaloader.JavaProject.UnloadException;
import io.github.pieter12345.javaloader.ProjectStateListener;
import io.github.pieter12345.javaloader.utils.Utils;

/**
 * JavaLoaderBukkitPlugin class.
 * This is the main class that will be loaded by Bukkit.
 * @author P.J.S. Kools
 * @version 0.0.1-SNAPSHOT
 * @since 05-01-2017
 */
public class JavaLoaderBukkitPlugin extends JavaPlugin {
	
	// Variables & Constants.
	private HashMap<String, JavaProject> projects = null;
	private static final String VERSION;
	private static final String PREFIX_INFO =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.RED + " ";
	private final File projectsDir = new File(this.getDataFolder().getAbsolutePath() + "/JavaProjects");
	private ProjectStateListener projectStateListener;
	
	static {
		// Get the version from the manifest.
		Package pack = JavaLoaderBukkitPlugin.class.getPackage();
		versionScope: {
			if(pack != null) {
				String implementationVersion = pack.getImplementationVersion();
				if(implementationVersion != null) {
					VERSION = pack.getImplementationVersion(); // Expected format: "A.B.C-SNAPSHOT".
					break versionScope;
				}
			}
			VERSION = "Unknown";
		}
	}
	
	// onEnable will run when the plugin is enabled.
	@Override
	public void onEnable() {
		
		// Check if a JDK is available, otherwise disable the plugin.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR
					+ "No java compiler available. This plugin requires a JDK to run on. Disabling plugin.");
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
		
		// Initialize project state listener.
		this.projectStateListener = new ProjectStateListener() {
			
			@Override
			public void onLoad(JavaProject project) {
				
				// Initialize the project instance class with the JavaPlugin.
				// Add a Bukkit Plugin implementation for JavaLoaderBukkitProjects.
				if(project.getInstance() instanceof JavaLoaderBukkitProject) {
					JavaLoaderBukkitProject bukkitProjectInstance = (JavaLoaderBukkitProject) project.getInstance();
					JavaLoaderBukkitProjectPlugin bukkitProjectPlugin = new JavaLoaderBukkitProjectPlugin(project);
					bukkitProjectInstance.initialize(project, bukkitProjectPlugin);
					bukkitProjectPlugin.setEnabled(true); // Sync Bukkit Plugin state.
					
					// Register commands used by the project.
					this.injectCommands(bukkitProjectPlugin, bukkitProjectInstance.getCommands());
					
				} else {
					project.getInstance().initialize(project);
				}
			}
			
			@Override
			public void onUnload(JavaProject project) {
				if(project.getInstance() instanceof JavaLoaderBukkitProject) {
					JavaLoaderBukkitProject bukkitProjectInstance = (JavaLoaderBukkitProject) project.getInstance();
					bukkitProjectInstance.getPlugin().setEnabled(false); // Sync Bukkit Plugin state.
					this.uninjectCommands(bukkitProjectInstance.getPlugin());
				}
			}
			
			@SuppressWarnings("unchecked")
			private void injectCommands(JavaLoaderBukkitProjectPlugin bukkitProjectPlugin, BukkitCommand[] commands) {
				if(commands == null || commands.length == 0) {
					return;
				}
				
				// Get the CommandMap.
				final SimpleCommandMap cmdMap;
				final PluginManager pluginManager = Bukkit.getPluginManager();
				if(pluginManager instanceof SimplePluginManager) {
					try {
						Field cmdMapField = SimplePluginManager.class.getDeclaredField("commandMap");
						cmdMapField.setAccessible(true);
						cmdMap = (SimpleCommandMap) cmdMapField.get(pluginManager);
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e) {
						throw new RuntimeException("Could not inject Bukkit commands"
								+ " because an Exception occurred in reflection code.", e);
					}
				} else {
					throw new RuntimeException("Could not inject Bukkit commands because Bukkit.getPluginManager()"
							+ " was not an instance of " + SimplePluginManager.class.getName());
				}
				
				// Get the currently known commands.
				final Map<String, Command> knownCommands;
				try {
					Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
					knownCommandsField.setAccessible(true);
					knownCommands = (Map<String, Command>) knownCommandsField.get(cmdMap);
				} catch (NoSuchFieldException | SecurityException
						| IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Could not inject Bukkit commands because"
							+ " the already existing commands could not be obtained.", e);
				}
				
				for(BukkitCommand command : commands) {
					
					// Generate the new command.
					PluginCommand bukkitCmd;
					try {
						Constructor<PluginCommand> constructor =
								PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
						constructor.setAccessible(true);
						bukkitCmd = constructor.newInstance(command.getName(), bukkitProjectPlugin);
					} catch (NoSuchMethodException | SecurityException | InstantiationException
							| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException("Could not inject Bukkit commands because"
								+ " an Exception occurred in reflection code while creating a new PluginCommand.", e);
					}
					bukkitCmd.setDescription(command.getDescription());
					bukkitCmd.setUsage(command.getUsageMessage());
					bukkitCmd.setPermission(command.getPermission());
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
//						Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "Command \"/" + bukkitCmd.getName() + "\""
//								+ " could not be registered.");
						// Throw an Exception as this should never happen since the command is unregistered above.
						throw new RuntimeException("Could not inject command because the command was already"
								+ " registered and failed to unregister."
								+ " The command that failed to register was: \"/" + bukkitCmd.getName() + "\"");
					}
					
				}
			}
			
			private void uninjectCommands(JavaLoaderBukkitProjectPlugin plugin) {
				
				// Get the CommandMap.
				SimpleCommandMap cmdMap;
				PluginManager pluginManager = Bukkit.getPluginManager();
				if(pluginManager instanceof SimplePluginManager) {
					try {
						Field cmdMapField = SimplePluginManager.class.getDeclaredField("commandMap");
						cmdMapField.setAccessible(true);
						cmdMap = (SimpleCommandMap) cmdMapField.get(pluginManager);
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e) {
						throw new RuntimeException("Could not uninject Bukkit commands"
								+ " because an Exception occurred in reflection code.", e);
					}
				} else {
					throw new RuntimeException("Could not uninject Bukkit commands because Bukkit.getPluginManager()"
							+ " was not an instance of " + SimplePluginManager.class.getName());
				}
				
				// Unregister all commands owned by the given plugin.
				Command[] registeredCommands = cmdMap.getCommands().toArray(new Command[0]);
				for(Command command : registeredCommands) {
					if(command instanceof PluginCommand && ((PluginCommand) command).getPlugin() == plugin) {
						command.unregister(cmdMap);
					}
				}
				
			}
		};
		
		// Loop over all project directories and add them as a JavaProject.
		this.projects = new HashMap<String, JavaProject>();
		File[] projectDirs = this.projectsDir.listFiles();
		if(projectDirs != null) {
			for(File projectDir : projectDirs) {
				if(projectDir.isDirectory() && !projectDir.getName().endsWith(".disabled")) {
					this.projects.put(projectDir.getName(),
							new JavaProject(projectDir.getName(), projectDir, this.projectStateListener));
				}
			}
		}
		
		// Load all projects (do not compile them unless they don't have a "bin" directory).
		for(JavaProject project : this.projects.values()) {
			
			// Compile the project if it has no "bin" directory.
			if(!project.getBinDir().exists()) {
				try {
					compile(project, Bukkit.getConsoleSender(), 5);
				} catch (CompileException e) {
					
					// Remove the newly created bin directory.
					Utils.removeFile(project.getBinDir());
					
					// Send feedback.
					Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "A CompileException occurred while compiling"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					continue;
				}
			}
			
			// Load the project.
			try {
				project.load();
			} catch (LoadException e) {
				Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
						+ " java project \"" + project.getName() + "\":"
						+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
			}
		}
		
		// Print feedback.
		Bukkit.getConsoleSender().sendMessage("JavaLoader " + VERSION + " enabled.");
		
	}
	
	// onDisable will run when the plugin is disabled.
	@Override
	public void onDisable() {
		
		// Unload all loaded projects.
		if(this.projects != null) {
			for(JavaProject project : this.projects.values()) {
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "An UnloadException occurred while"
								+ " unloading java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
			}
		}
		
		// Print feedback.
		Bukkit.getConsoleSender().sendMessage("JavaLoader " + VERSION + " disabled.");
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
		
		// Check if the plugin is enabled and validate the command prefix.
		if(!this.isEnabled() || !cmd.getName().equalsIgnoreCase("javaloader")) {
			return false;
		}
		
		// "/javaloader".
		if(args.length == 0) {
			args = new String[] {"help"};
		}
		
		switch(args[0].toLowerCase()) {
		case "help":
			
			// "/javaloader help [command]".
			if(args.length == 1) {
				sender.sendMessage(PREFIX_INFO + colorize("&aJavaLoader - Version: &8" + VERSION + "&a."
						+ " Author:&8 Pieter12345/woesh0007&a."
						+ "\n&6  - /javaloader help [subcommand]"
						+ "\n&3	  Displays this page or information about the subcommand."
						+ "\n&6  - /javaloader recompile [project]"
						+ "\n&3	  Recompiles, unloads and loads the given or all projects."
						+ "\n&6  - /javaloader unload [project]"
						+ "\n&3	  Unloads the given or all projects."
						+ "\n&6  - /javaloader load [project]"
						+ "\n&3	  Loads the given or all projects."));
			} else if(args.length == 2) {
				switch(args[1].toLowerCase()) {
				case "help":
					sender.sendMessage(PREFIX_INFO + colorize("&6/javaloader help &8-&3 Displays command help."));
					return true;
				case "recompile":
					sender.sendMessage(PREFIX_INFO + colorize("&6/javaloader recompile [project] &8-&3 Recompiles,"
							+ " unloads and loads the given project or all projects when no project is given."
							+ " Recompiling happens before projects are unloaded, so the old project will stay loaded"
							+ " when a recompile Exception occurs."));
					return true;
				case "load":
					sender.sendMessage(PREFIX_INFO + colorize("&6/javaloader load [project] &8-&3 Loads the given"
							+ " project or all projects when no project is given. To load a project, only the .class"
							+ " files in the project folder have to be valid."
							+ " This will also load newly added projects."));
					return true;
				case "unload":
					sender.sendMessage(PREFIX_INFO + colorize("&6/javaloader unload [project] &8-&3 Unloads the given"
							+ " project or all projects when no project is given."
							+ " Projects that no longer exist will be removed."));
					return true;
				default:
					sender.sendMessage(PREFIX_ERROR + "Unknown subcommand: /javaloader " + args[1]);
					return true;
				}
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
			
		case "recompile":
			
			// "/javaloader recompile".
			if(args.length == 1) {
				
				// Remove unexisting projects (happens when a project directory is removed).
				removeRemovedProjects(sender);
				
				// Add new projects (happens when a new project directory is created).
				checkAndAddNewProjects();
				
				// Store all projects that gave an error so they can be excluded from next steps.
				ArrayList<JavaProject> errorProjects = new ArrayList<JavaProject>();
				
				// Recompile everything to a "bin_new" directory.
				for(JavaProject project : this.projects.values()) {
					project.setBinDirName("bin_new");
					try {
						compile(project, sender, 5);
					} catch (CompileException e) {
						
						// Remove the newly created bin directory and set the old one.
						Utils.removeFile(project.getBinDir());
						project.setBinDirName("bin");
						
						// Send feedback.
						sender.sendMessage(PREFIX_ERROR + "A CompileException occurred while compiling"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						errorProjects.add(project);
						continue;
					}
					project.setBinDirName("bin");
				}
				
				
				// Unload all loaded projects.
				for(JavaProject project : this.projects.values()) {
					if(project.isEnabled()) {
						
						// Exclude projects with an error.
						if(errorProjects.contains(project)) {
							continue;
						}
						
						try {
							project.unload();
						} catch (UnloadException e) {
							sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					}
				}
				
				// Replace all current "bin" directories with the "bin_new" directories.
				for(JavaProject project : this.projects.values()) {
					
					// Exclude projects with an error.
					if(errorProjects.contains(project)) {
						continue;
					}
					
					// Get the "bin_new" directory.
					project.setBinDirName("bin_new");
					File newBinDir = project.getBinDir();
					project.setBinDirName("bin");
					
					// Replace the current "bin" directory with "bin_new" and remove "bin_new".
					if(project.getBinDir().exists() && !Utils.removeFile(project.getBinDir())) {
						sender.sendMessage(PREFIX_ERROR + "Failed to rename \"bin_new\" to \"bin\" because the \"bin\""
								+ " directory could not be removed for project \"" + project.getName() + "\"."
								+ " This can be fixed manually or by attempting another recompile. The project has"
								+ " already been disabled and some files of the \"bin\" directory might be removed.");
						errorProjects.add(project);
						continue;
					}
					if(!newBinDir.renameTo(project.getBinDir())) {
						sender.sendMessage(PREFIX_ERROR
								+ "Failed to rename \"bin_new\" to \"bin\" for project \"" + project.getName() + "\"."
								+ " This can be fixed manually or by attempting another recompile."
								+ " The project has already been disabled and the \"bin\" directory has been removed.");
						errorProjects.add(project);
						continue;
					}
				}
				
				// Load all projects.
				for(JavaProject project : this.projects.values()) {
					
					// Exclude projects with an error.
					if(errorProjects.contains(project)) {
						continue;
					}
					
					try {
						project.load();
					} catch (LoadException e) {
						sender.sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						errorProjects.add(project);
					}
				}
				
				// Send feedback.
				int count = this.projects.size();
				int errorCount = errorProjects.size();
				if(errorCount == 0) {
					sender.sendMessage(PREFIX_INFO + "Successfully compiled and loaded "
							+ count + (count == 1 ? " project" : " projects") + ".");
				} else {
					sender.sendMessage(PREFIX_INFO + "Compiled and loaded " + count
							+ (count == 1 ? " project" : " projects") + " of which " + errorCount + " failed.");
				}
				
			}
			// "/javaloader recompile <projectName>".
			else if(args.length == 2) {
				String projectName = args[1];
				JavaProject project = this.projects.get(projectName);
				
				// Check if the given project exists.
				// Add it from the filesystem if it was added and remove it if it's not on the filesystem anymore.
				if(project == null) {
					
					// Check if the project directory was added.
					File[] projectDirs = this.projectsDir.listFiles();
					if(projectDirs != null) {
						for(File projectDir : projectDirs) {
							if(projectDir.getName().equals(projectName)
									&& projectDir.isDirectory() && !projectDir.getName().endsWith(".disabled")) {
								project = new JavaProject(projectDir.getName(), projectDir, this.projectStateListener);
								this.projects.put(project.getName(), project);
							}
						}
					}
					if(project == null) {
						sender.sendMessage(PREFIX_ERROR + "Project does not exist: " + args[1]);
						return true;
					}
				} else {
					
					// Check if the project directory was removed.
					if(!project.getProjectDir().isDirectory()) {
						if(project.isEnabled()) {
							try {
								project.unload();
							} catch (UnloadException e) {
								sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading REMOVED"
										+ " java project \"" + project.getName() + "\":"
										+ (e.getCause() == null ?
												" " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
							}
						}
						this.projects.remove(projectName);
						sender.sendMessage(PREFIX_INFO + "Project unloaded and removed because the project directory"
								+ " no longer exists: " + projectName);
						return true;
					}
				}
				
				// Recompile the project to a "bin_new" directory.
				project.setBinDirName("bin_new");
				try {
					compile(project, sender, 5);
				} catch (CompileException e) {
					
					// Remove the newly created bin directory and set the old one.
					Utils.removeFile(project.getBinDir());
					project.setBinDirName("bin");
					
					// Send feedback and return.
					sender.sendMessage(PREFIX_ERROR + "A CompileException occurred while compiling"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					return true;
				}
				File newBinDir = project.getBinDir();
				project.setBinDirName("bin");
				
				
				// Unload the project if it was loaded.
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
				
				// Replace the current "bin" directory with "bin_new" and remove "bin_new".
				if(project.getBinDir().exists() && !Utils.removeFile(project.getBinDir())) {
					sender.sendMessage(PREFIX_ERROR + "Failed to rename \"bin_new\" to \"bin\" because the \"bin\""
							+ " directory could not be removed for project \"" + project.getName() + "\"."
							+ " This can be fixed manually or by attempting another recompile. The project has"
							+ " already been disabled and some files of the \"bin\" directory might be removed.");
					return true;
				}
				if(!newBinDir.renameTo(project.getBinDir())) {
					sender.sendMessage(PREFIX_ERROR
							+ "Failed to rename \"bin_new\" to \"bin\" for project \"" + project.getName() + "\"."
							+ " This can be fixed manually or by attempting another recompile."
							+ " The project has already been disabled and the \"bin\" directory has been removed.");
					return true;
				}
				
				// Load the project.
				try {
					project.load();
				} catch (LoadException e) {
					sender.sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					return true;
				}
				
				// Send feedback.
				sender.sendMessage(PREFIX_INFO
						+ "Successfully compiled and loaded project: " + project.getName() + ".");
				
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
			
		case "unload":
			// "/javaloader unload".
			if(args.length == 1) {
				
				// Unload all loaded projects and remove unexisting unloaded projects.
				int unloadCount = 0;
				for(Iterator<JavaProject> iterator = this.projects.values().iterator(); iterator.hasNext();) {
					JavaProject project = iterator.next();
					
					// Unload the project.
					if(project.isEnabled()) {
						try {
							project.unload();
							unloadCount++;
						} catch (UnloadException e) {
							sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
							continue;
						}
					}
					
					// Remove the project if it no longer exists.
					if(!project.getProjectDir().exists()) {
						iterator.remove();
					}
					
				}
				sender.sendMessage(PREFIX_INFO
						+ "Unloaded " + unloadCount + " project" + (unloadCount == 1 ? "" : "s") + ".");
			}
			// "/javaloader unload <projectName>".
			else if(args.length == 2) {
				String projectName = args[1];
				JavaProject project = this.projects.get(projectName);
				
				// Check if the project exists.
				if(project == null) {
					sender.sendMessage(PREFIX_ERROR + "Project does not exist: " + projectName);
					return true;
				}
				
				// Unload the project if it was loaded.
				if(project.isEnabled()) {
					try {
						project.unload();
						sender.sendMessage(PREFIX_INFO + "Project unloaded: " + projectName);
					} catch (UnloadException e) {
						sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				} else {
					sender.sendMessage(PREFIX_ERROR + "Project was not enabled: " + projectName);
				}
				
				// Remove the project if it was successfully disabled and does not exist anymore.
				if(!project.isEnabled() && !project.getProjectDir().exists()) {
					this.projects.remove(project);
				}
				
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
		case "load":
			// "/javaloader load".
			if(args.length == 1) {
				
				// Add new projects (happens when a new project directory is created).
				checkAndAddNewProjects();
				
				// Load all unloaded projects.
				int loadCount = 0;
				for(JavaProject project : this.projects.values()) {
					if(!project.isEnabled()) {
						try {
							project.load();
							loadCount++;
						} catch (LoadException e) {
							sender.sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					}
				}
				sender.sendMessage(PREFIX_INFO
						+ "Loaded " + loadCount + " project" + (loadCount == 1 ? "" : "s") + ".");
			}
			// "/javaloader load <projectName>".
			else if(args.length == 2) {
				String projectName = args[1];
				JavaProject project = this.projects.get(projectName);
				
				// Check if the project exists. Add the project from the filesystem if it was added.
				if(project == null) {
					
					// Check if the project directory was added.
					File[] projectDirs = this.projectsDir.listFiles();
					if(projectDirs != null) {
						for(File projectDir : projectDirs) {
							if(projectDir.getName().equals(projectName)
									&& projectDir.isDirectory() && !projectDir.getName().endsWith(".disabled")) {
								project = new JavaProject(projectDir.getName(), projectDir, this.projectStateListener);
								this.projects.put(project.getName(), project);
							}
						}
					}
					
					if(project == null) {
						sender.sendMessage(PREFIX_ERROR + "Project does not exist: " + projectName);
						return true;
					}
				}
				
				// Load the project if it wasn't loaded.
				if(!project.isEnabled()) {
					try {
						project.load();
						sender.sendMessage(PREFIX_INFO + "Project loaded: " + projectName);
					} catch (LoadException e) {
						sender.sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				} else {
					sender.sendMessage(PREFIX_ERROR + "Project already loaded: " + projectName);
				}
				
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
		default:
			sender.sendMessage(PREFIX_ERROR + "Unknown argument: " + args[0]);
			return true;
		}
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(command.getName().equals("javaloader")) {
			String search = args[args.length - 1].toLowerCase();

			// TAB-complete "/javaloader <arg>".
			if(args.length == 1) {
				List<String> ret = new ArrayList<String>();
				for(String comp : new String[] {"help", "load", "unload", "recompile"}) {
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
				for(String comp : this.getProjectNames()) {
					if(comp.toLowerCase().startsWith(search)) {
						ret.add(comp);
					}
				}
				return ret;
			}
			
		}
		return null;
	}
	
	private void checkAndAddNewProjects() {
		// Add new projects (happens when a new project directory is created).
		File[] projectDirs = this.projectsDir.listFiles();
		if(projectDirs != null) {
			for(File projectDir : projectDirs) {
				if(projectDir.isDirectory() && !projectDir.getName().endsWith(".disabled")
						&& !this.projects.containsKey(projectDir.getName())) {
					this.projects.put(projectDir.getName(),
							new JavaProject(projectDir.getName(), projectDir, this.projectStateListener));
				}
			}
		}
	}
	
	private void removeRemovedProjects(CommandSender feedbackSender) {
		// Remove unexisting projects (happens when a project directory is removed).
		for(Iterator<JavaProject> iterator = this.projects.values().iterator(); iterator.hasNext();) {
			JavaProject project = iterator.next();
			if(!project.getProjectDir().exists()) {
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						feedbackSender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading REMOVED"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
				iterator.remove();
			}
		}
	}
	
	private static String colorize(String str) {
		return str.replaceAll("(?<!\\&)\\&(?=[0-9a-fA-F])", ChatColor.COLOR_CHAR + "");
	}
	
	private void createExampleProject() {
		try {
			CodeSource codeSource = JavaLoaderBukkitPlugin.class.getProtectionDomain().getCodeSource();
			if(codeSource != null) {
				ZipInputStream inStream = new ZipInputStream(codeSource.getLocation().openStream());
				
				ZipEntry zipEntry;
				while((zipEntry = inStream.getNextEntry()) != null) {
					String name = zipEntry.getName();
					if(name.startsWith("exampleprojects/bukkit/")) {
						File targetFile = new File(this.projectsDir
								+ "/" + name.substring("exampleprojects/bukkit/".length()));
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
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "Failed to create example projects."
					+ " Here's the stacktrace:\n" + Utils.getStacktrace(e));
			return;
		}
	}
	
	private static void compile(JavaProject project, CommandSender sender, int feedbackLimit) throws CompileException {
		final ArrayList<String> messages = new ArrayList<String>();
		Writer writer = new Writer() {
			private String buff = "";
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				
				// Get the message.
				char[] toWrite = new char[len];
				System.arraycopy(cbuf, off, toWrite, 0, len);
				String message = new String(toWrite);
				message = message.replace("\r", "");
				
				// Divide the message in parts and add them as seperate warnings/errors.
				// The compiler sends entire lines and seperate newlines.
				if(message.equals("\n") || message.startsWith("\t") || message.startsWith(" ")) {
					this.buff += message;
				} else {
					if(!this.buff.isEmpty()) {
						messages.add(this.buff);
					}
					this.buff = message;
				}
			}
			@Override
			public void flush() throws IOException { }
			@Override
			public void close() throws IOException {
				if(!this.buff.isEmpty()) {
					messages.add(this.buff);
				}
			}
		};
		CompileException ex = null;
		try {
			project.compile(writer);
		} catch (CompileException e) {
			ex = e;
		}
		try {
			writer.close();
		} catch (IOException e) {
			// Never happens.
		}
		
		// Print the feedback.
		if(!messages.isEmpty() && feedbackLimit > 0) {
			String feedback = "";
			for(int i = 0; i < messages.size() - 1; i++) {
				if(i >= feedbackLimit && i != messages.size() - 1) {
					feedback += (feedback.endsWith("\n") ? "" : "\n") + "... " + (messages.size() - i - 1) + " more\n"
							+ messages.get(messages.size() - 1);
					break;
				}
				feedback += messages.get(i);
			}
			if(feedback.endsWith("\n")) {
				feedback = feedback.substring(0, feedback.length() - 1);
			}
			feedback = feedback.replace("\t", "    "); // Minecraft cannot display tab characters.
			sender.sendMessage(PREFIX_ERROR + "Compiler feedback:\n" + ChatColor.GOLD + feedback + ChatColor.RESET);
		}
		
		// Rethrow if compilation caused an Exception.
		if(ex != null) {
			throw ex;
		}
	}
	
	/**
	 * getProject method.
	 * @param name - The name of the JavaLoader project.
	 * @return The JavaProject or null if no project with the given name exists.
	 */
	public JavaProject getProject(String name) {
		return (this.projects == null ? null : this.projects.get(name));
	}
	
	/**
	 * getProjects method.
	 * @return An array containing all loaded JavaLoader projects.
	 */
	public JavaProject[] getProjects() {
		return (this.projects == null ? new JavaProject[0] : this.projects.values().toArray(new JavaProject[0]));
	}
	
	/**
	 * getProjectNames method.
	 * @return An array containing all loaded JavaLoader project names.
	 */
	public String[] getProjectNames() {
		return (this.projects == null ? new String[0] : this.projects.keySet().toArray(new String[0]));
	}
	
	/**
	 * getPlugin method.
	 * WARNING: Calls to this method from within a static code block or constructor in a JavaLoaderBukkitProject child
	 *  class might return null. It is advised to only call this method after the onLoad() method had been called.
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
	 *  class might return null. It is advised to only call this method after the onLoad() method had been called.
	 * @param projectName - The name of the JavaLoader project.
	 * @return The JavaLoaderBukkitProjectPlugin instance for this JavaLoader project.
	 */
	public JavaLoaderBukkitProjectPlugin getPlugin(String projectName) {
		JavaProject project = this.getProject(projectName);
		return (project == null ? null : this.getPlugin(project));
	}
}
