package io.github.pieter12345.javaloader.bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import io.github.pieter12345.javaloader.exceptions.CompileException;
import io.github.pieter12345.javaloader.exceptions.DepOrderViolationException;
import io.github.pieter12345.javaloader.exceptions.LoadException;
import io.github.pieter12345.javaloader.exceptions.UnloadException;
import io.github.pieter12345.javaloader.JavaLoaderProject;
import io.github.pieter12345.javaloader.JavaProject;
import io.github.pieter12345.javaloader.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.ProjectManager;
import io.github.pieter12345.javaloader.ProjectManager.RecompileAllResult;
import io.github.pieter12345.javaloader.ProjectManager.RecompileFeedbackHandler;
import io.github.pieter12345.javaloader.ProjectStateListener;
import io.github.pieter12345.javaloader.utils.Utils;

/**
 * JavaLoaderBukkitPlugin class.
 * This is the main class that will be loaded by Bukkit.
 * @author P.J.S. Kools
 * @since 05-01-2017
 */
public class JavaLoaderBukkitPlugin extends JavaPlugin {
	
	// Variables & Constants.
	private static final String VERSION;
	private static final String PREFIX_INFO =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			ChatColor.GOLD + "[" + ChatColor.DARK_AQUA + "JavaLoader" + ChatColor.GOLD + "]" + ChatColor.RED + " ";
	
	private static final int COMPILER_FEEDBACK_LIMIT = 5; // The max amount of warnings/errors to print per recompile.
	
	private ProjectManager projectManager;
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
		
		// Create the project manager.
		this.projectManager = new ProjectManager(this.projectsDir);
		
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
					} catch (Exception e) {
						throw new LoadException(project, "An Exception occurred in " + project.getName() + "'s "
								+ bukkitProjectInstance.getClass().getName() + ".getCommands()."
								+ " Is the project up to date?  Stacktrace:\n" + Utils.getStacktrace(e));
					}
					this.injectCommands(bukkitProjectPlugin, commands);
					
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
		this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
		
		// Load all projects.
		Set<JavaProject> loadedProjects = this.projectManager.loadAllProjects((LoadException ex) -> {
			Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		
		// Print feedback.
		JavaProject[] projects = this.projectManager.getProjects();
		Bukkit.getConsoleSender().sendMessage("JavaLoader " + VERSION + " enabled. " + loadedProjects.size() + "/"
				+ projects.length + " projects loaded.");
		
	}
	
	@Override
	public void onDisable() {
		
		// Unload all loaded projects and remove them from the project manager.
		this.projectManager.clear((UnloadException ex) -> {
			Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		this.projectManager = null;
		
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
						+ "\n&6  - /javaloader list"
						+ "\n&3   Displays a list of all projects and their status."
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
				case "list":
					sender.sendMessage(PREFIX_INFO + colorize(
							"&6/javaloader list &8-&3 Displays a list of all projects and their status."));
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
			
		case "list":
			
			// "/javaloader list".
			if(args.length == 1) {
				
				// TODO - Consider adding new projects from the file system here.
				
				// Get all projects and sort them.
				JavaProject[] projects = this.projectManager.getProjects();
				List<JavaProject> sortedProjects = Arrays.<JavaProject>asList(projects);
				sortedProjects.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
				
				// Give feedback for having no projects available.
				if(projects.length == 0) {
					sender.sendMessage(PREFIX_INFO + "There are no projects available.");
					return true;
				}
				
				// Construct the feedback message for >=1 projects available.
				String projectsStr = Utils.glueIterable(sortedProjects, (JavaProject project) ->
						(project.isEnabled() ? ChatColor.DARK_GREEN : ChatColor.RED) + project.getName()
						, ChatColor.GREEN + ", ");
				String message = colorize("Projects (&2loaded&a/&cunloaded&a): " + projectsStr + ".");
				
				// Send the feedback.
				sender.sendMessage(PREFIX_INFO + message);
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
			
		case "recompile":
			
			// "/javaloader recompile".
			if(args.length == 1) {
				
				// Recompile all projects.
				final List<String> messages = new ArrayList<String>();
				RecompileAllResult result = this.projectManager.recompileAllProjects(new RecompileFeedbackHandler() {
					@Override
					public void handleUnloadException(UnloadException e) {
						sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + e.getProject().getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
					@Override
					public void handleLoadException(LoadException e) {
						sender.sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
								+ " java project \"" + e.getProject().getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
					@Override
					public void handleCompileException(CompileException e) {
						sender.sendMessage(PREFIX_ERROR + "A CompileException occurred while compiling"
								+ " java project \"" + e.getProject().getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
					@Override
					public void compilerFeedback(String feedback) {
						messages.add(feedback);
					}
				}, this.projectStateListener);
				
				// Give compiler feedback.
				if(!messages.isEmpty() && COMPILER_FEEDBACK_LIMIT > 0) {
					String feedback = "";
					
					// Add at max all but one feedback string.
					for(int i = 0; i < messages.size() - 1; i++) {
						if(i >= COMPILER_FEEDBACK_LIMIT) {
							feedback += (feedback.endsWith("\n") ? "" : "\n") + "... "
									+ (messages.size() - i - 1) + " more";
							break;
						}
						feedback += messages.get(i);
					}
					
					// Add the last feedback string. This is always "x errors".
					feedback += (feedback.endsWith("\n") ? "" : "\n") + messages.get(messages.size() - 1);
					
					if(feedback.endsWith("\n")) {
						feedback = feedback.substring(0, feedback.length() - 1);
					}
					feedback = feedback.replace("\t", "    "); // Minecraft cannot display tab characters.
					sender.sendMessage(PREFIX_ERROR + "Compiler feedback:\n"
							+ ChatColor.GOLD + feedback + ChatColor.RESET);
				}
				
				// Give feedback.
				sender.sendMessage(new String[] {
						PREFIX_INFO + "Recompile complete.",
						"    Projects added: " + result.addedProjects.size(),
						"    Projects removed: " + result.removedProjects.size(),
						"    Projects compiled: " + result.compiledProjects.size(),
						"    Projects unloaded: " + result.unloadedProjects.size(),
						"    Projects loaded: " + result.loadedProjects.size(),
						"    Projects with errors: " + result.errorProjects.size()
						});
				
			}
			// "/javaloader recompile <projectName>".
			else if(args.length == 2) {
				final String projectName = args[1];
				
				// Get the project. Attempt to add it from the file system if it does not yet exist in the
				// project manager.
				JavaProject project = this.projectManager.getProject(projectName);
				if(project == null) {
					project = this.projectManager
							.addProjectFromProjectDirectory(projectName, this.projectStateListener);
					if(project == null) {
						sender.sendMessage(PREFIX_ERROR + "Project does not exist: \"" + projectName + "\".");
						return true;
					}
				}
				
				// Unload and remove the project if it was deleted from the file system.
				List<JavaProject> removedProjects = this.projectManager.unloadAndRemoveProjectIfDeleted(
						projectName, (UnloadException e) -> {
					sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred in"
							+ " java project \"" + e.getProject().getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
				});
				if(removedProjects != null) {
					if(removedProjects.isEmpty()) {
						sender.sendMessage(PREFIX_INFO + "Removed project because it no longer exists in the file"
								+ " system: \"" + projectName + "\".");
					} else {
						sender.sendMessage(PREFIX_INFO + "Removed and unloaded project because it no longer exists in"
								+ " the file system: \"" + projectName + "\".");
						if(removedProjects.size() > 1) {
							assert(removedProjects.get(0).getName().equals(projectName));
							removedProjects.remove(0);
							sender.sendMessage(PREFIX_INFO + "The following " + (removedProjects.size() == 1
									? "dependent was" : "dependents were") + " unloaded: "
									+ Utils.glueIterable(removedProjects, (JavaProject p) -> p.getName(), ", ") + ".");
						}
					}
					return true;
				}
				
				// Recompile the project.
				boolean success = false;
				final List<String> messages = new ArrayList<String>();
				try {
					this.projectManager.recompile(project,
							(String compilerFeedback) -> messages.add(compilerFeedback),
							(UnloadException e) -> sender.sendMessage(PREFIX_ERROR + (e.getCause() == null
									? "UnloadException: " + e.getMessage() : "An UnloadException occurred in java"
									+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e))));
					success = true;
				} catch (CompileException e) {
					sender.sendMessage(PREFIX_ERROR + (e.getCause() == null
							? "CompileException: " + e.getMessage() : "A CompileException occurred in java"
							+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
				} catch (LoadException e) {
					sender.sendMessage(PREFIX_ERROR + (e.getCause() == null
							? "LoadException: " + e.getMessage() : "A LoadException occurred in java"
							+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
				} catch (DepOrderViolationException e) {
					sender.sendMessage(PREFIX_ERROR + (e.getCause() == null
							? "DepOrderViolationException: " + e.getMessage() : "A DepOrderViolationException occurred"
							+ " in java project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
				} catch (IllegalArgumentException e) {
					throw new InternalError("Project is obtained from this manager, so this should be impossible.", e);
				}
				
				// Give compiler feedback.
				if(!messages.isEmpty() && COMPILER_FEEDBACK_LIMIT > 0) {
					String feedback = "";
					
					// Add at max all but one feedback string.
					for(int i = 0; i < messages.size() - 1; i++) {
						if(i >= COMPILER_FEEDBACK_LIMIT) {
							feedback += (feedback.endsWith("\n") ? "" : "\n") + "... "
									+ (messages.size() - i - 1) + " more";
							break;
						}
						feedback += messages.get(i);
					}
					
					// Add the last feedback string. This is always "x errors".
					feedback += (feedback.endsWith("\n") ? "" : "\n") + messages.get(messages.size() - 1);
					
					if(feedback.endsWith("\n")) {
						feedback = feedback.substring(0, feedback.length() - 1);
					}
					feedback = feedback.replace("\t", "    "); // Minecraft cannot display tab characters.
					sender.sendMessage(PREFIX_ERROR + "Compiler feedback:\n"
							+ ChatColor.GOLD + feedback + ChatColor.RESET);
				}
				
				// Send feedback.
				sender.sendMessage(PREFIX_INFO + "Recompile complete" + (success ? "" : " (with errors)") + ".");
				
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
			
		case "unload":
			// "/javaloader unload".
			if(args.length == 1) {
				
				// Unload all projects.
				Set<JavaProject> unloadedProjects = this.projectManager.unloadAllProjects((UnloadException ex) -> {
					Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
							+ " java project \"" + ex.getProject().getName() + "\":"
							+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
				});
				
				// Remove no longer existing projects from the project manager.
				Set<JavaProject> removedProjects = this.projectManager.removeUnloadedProjectsIfDeleted();
				
				// Send feedback.
				sender.sendMessage(PREFIX_INFO + "Unloaded " + unloadedProjects.size()
						+ " project" + (unloadedProjects.size() == 1 ? "" : "s") + ".");
				if(removedProjects.size() != 0) {
					sender.sendMessage(PREFIX_INFO + "Removed " + removedProjects.size()
							+ " project" + (removedProjects.size() == 1 ? "" : "s")
							+ " due to their project directory no longer existing.");
				}
			}
			// "/javaloader unload <projectName>".
			else if(args.length == 2) {
				String projectName = args[1];
				JavaProject project = this.projectManager.getProject(projectName);
				
				// Check if the project exists.
				if(project == null) {
					sender.sendMessage(PREFIX_ERROR + "Project does not exist: " + projectName);
					return true;
				}
				
				// Unload the project if it was loaded.
				if(project.isEnabled()) {
					try {
						project.unload(UnloadMethod.EXCEPTION_ON_LOADED_DEPENDENTS, (UnloadException e) -> {
							sender.sendMessage(PREFIX_ERROR + "An UnloadException occurred while unloading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						});
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
				JavaProject removedProject = this.projectManager.removeUnloadedProjectIfDeleted(projectName);
				if(removedProject != null) {
					sender.sendMessage(PREFIX_INFO
							+ "Removed project due to its project directory no longer existing: " + projectName);
				}
				
			} else {
				sender.sendMessage(PREFIX_ERROR + "Too many arguments.");
			}
			return true;
		case "load":
			// "/javaloader load".
			if(args.length == 1) {
				
				// Add new projects (happens when a new project directory is created).
				this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
				
				// Load all projects.
				Set<JavaProject> loadedProjects = this.projectManager.loadAllProjects((LoadException ex) -> {
					Bukkit.getConsoleSender().sendMessage(PREFIX_ERROR + "A LoadException occurred while loading"
							+ " java project \"" + ex.getProject().getName() + "\":"
							+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
				});
				
				// Send feedback.
				sender.sendMessage(PREFIX_INFO + "Loaded " + loadedProjects.size()
						+ " project" + (loadedProjects.size() == 1 ? "" : "s") + ".");
				
			}
			// "/javaloader load <projectName>".
			else if(args.length == 2) {
				String projectName = args[1];
				JavaProject project = this.projectManager.getProject(projectName);
				
				// Check if the project exists. Add the project from the filesystem if it was added.
				if(project == null) {
					
					// Attempt to load the project from file. This works if it has been added during runtime.
					project = this.projectManager.addProjectFromProjectDirectory(
							projectName, this.projectStateListener);
					
					// Print an error if the project does not exist.
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
			
			// Subcommand without tabcompleter.
			return Collections.emptyList();
		}
		return null;
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
