package io.github.pieter12345.javaloader.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import io.github.pieter12345.javaloader.core.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.core.ProjectManager.LoadAllResult;
import io.github.pieter12345.javaloader.core.ProjectManager.RecompileAllResult;
import io.github.pieter12345.javaloader.core.ProjectManager.RecompileFeedbackHandler;
import io.github.pieter12345.javaloader.core.exceptions.CompileException;
import io.github.pieter12345.javaloader.core.exceptions.DepOrderViolationException;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.utils.Utils;
import io.github.pieter12345.javaloader.core.utils.AnsiColor.FormatException;

/**
 * Handles JavaLoader command execution.
 * @author P.J.S. Kools
 */
public class CommandExecutor {
	
	private final ProjectManager projectManager;
	private final ProjectStateListener projectStateListener;
	private final ExitCommandHandler exitCommandHandler;
	
	private final String commandPrefix;
	private final List<String> pluginAuthors;
	private final String version;
	
	private final FeedbackColorizer colorizer;
	private final int compilerFeedbackLimit;
	
	/**
	 * Creates a new {@link CommandExecutor}.
	 * @param projectManager - The project manager.
	 * @param projectStateListener - The project state listener used for loading, unloading and compiling
	 * JavaLoader projects.
	 * @param exitCommandHandler - The exit commamd handler or null if the exit command does not exist.
	 * @param commandPrefix - The prefix used for commands. This will be used for giving command feedback.
	 * @param pluginAuthors - The JavaLoader author(s).
	 * @param version - The JavaLoader version.
	 * @param colorizer - The colorizer, used to colorize command feedback.
	 * @param compilerFeedbackLimit - The compile error feedback limit per compiled project.
	 */
	public CommandExecutor(ProjectManager projectManager, ProjectStateListener projectStateListener,
			ExitCommandHandler exitCommandHandler, String commandPrefix, List<String> pluginAuthors, String version,
			FeedbackColorizer colorizer, int compilerFeedbackLimit) {
		this.projectManager = projectManager;
		this.projectStateListener = projectStateListener;
		this.exitCommandHandler = exitCommandHandler;
		this.commandPrefix = (commandPrefix == null || commandPrefix.trim().isEmpty()
				? "" : commandPrefix.trim() + " ");
		this.pluginAuthors = pluginAuthors;
		this.version = version;
		this.colorizer = colorizer;
		this.compilerFeedbackLimit = compilerFeedbackLimit;
	}
	
	/**
	 * Executes the given command.
	 * @param sender - The command sender, used to give feedback to.
	 * @param cmdParts - The command parts excluding command prefix.
	 */
	public void executeCommand(final CommandSender sender, String[] cmdParts) {
		
		// Default to help when no arguments are given.
		if(cmdParts.length == 0) {
			cmdParts = new String[] {"help"};
		}
		
		switch(cmdParts[0].toLowerCase()) {
			case "help":
				
				// "<prefix> help [command]".
				if(cmdParts.length == 1) {
					List<String> authors = this.pluginAuthors;
					String authorsStr = "Author" + (authors.size() == 1 ? "" : "s") + ": &8"
							+ (authors.size() == 0 ? "Unknown"
							: Utils.glueIterable(authors, (String str) -> str, "&a, &8")) + "&a.";
					sender.sendMessage(MessageType.INFO, this.colorizer.colorize(
							"&aVersion: &8" + this.version + "&a. " + authorsStr
							+ "\n&6  - " + this.commandPrefix + "help ["
									+ (this.commandPrefix.isEmpty() ? "" : "sub") + "command]"
							+ "\n&3    Displays this page or information about the "
									+ (this.commandPrefix.isEmpty() ? "" : "sub") + "command."
							+ "\n&6  - " + this.commandPrefix + "list"
							+ "\n&3    Displays a list of all projects and their status."
							+ "\n&6  - " + this.commandPrefix + "recompile [project]"
							+ "\n&3    Recompiles, unloads and loads the given or all projects."
							+ "\n&6  - " + this.commandPrefix + "unload [project]"
							+ "\n&3    Unloads the given or all projects."
							+ "\n&6  - " + this.commandPrefix + "load [project]"
							+ "\n&3    Loads the given or all projects."
							+ (this.exitCommandHandler == null ? ""
									: "\n&6  - " + this.commandPrefix + "exit"
									+ "\n&3    Exits JavaLoader.")).split("\n"));
				} else if(cmdParts.length == 2) {
					switch(cmdParts[1].toLowerCase()) {
						case "help":
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize(
									"&6" + this.commandPrefix + "help &8-&3 Displays command help."));
							return;
						case "list":
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize("&6" + this.commandPrefix
									+ "list &8-&3 Displays a list of all projects and their status."));
							return;
						case "recompile":
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize("&6" + this.commandPrefix
									+ "recompile [project] &8-&3"
									+ " Recompiles, unloads and loads the given project or all projects when no project"
									+ " is given. Recompiling happens before projects are unloaded, so the old project"
									+ " will stay loaded when a recompile Exception occurs."));
							return;
						case "load":
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize("&6" + this.commandPrefix
									+ "load [project] &8-&3 Loads the"
									+ " given project or all projects when no project is given. To load a project, only"
									+ " the .class files in the project folder have to be valid."
									+ " This will also load newly added projects."));
							return;
						case "unload":
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize("&6" + this.commandPrefix
									+ "unload [project] &8-&3 Unloads the"
									+ " given project or all projects when no project is given."
									+ " Projects that no longer exist will be removed."));
							return;
						case "exit":
							if(this.exitCommandHandler != null) {
							sender.sendMessage(MessageType.INFO, this.colorizer.colorize(
									"&6" + this.commandPrefix + "exit &8-&3 Exits JavaLoader."));
								return;
							}
							// Intended fall through to default case.
						default:
							sender.sendMessage(MessageType.ERROR,
									"Unknown " + (this.commandPrefix.isEmpty() ? "" : "sub") + "command: "
									+ this.commandPrefix + cmdParts[1]);
							return;
					}
				} else {
					sender.sendMessage(MessageType.ERROR, "Too many arguments.");
				}
				return;
				
			case "list":
				
				// "<prefix> list".
				if(cmdParts.length == 1) {
					
					// Get all projects and sort them.
					JavaProject[] projects = this.projectManager.getProjects();
					List<JavaProject> sortedProjects = Arrays.<JavaProject>asList(projects);
					sortedProjects.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
					
					// Give feedback for having no projects available.
					if(projects.length == 0) {
						sender.sendMessage(MessageType.INFO, "There are no projects available.");
						return;
					}
					
					// Construct and send the feedback message for >=1 projects available.
					String projectsStr = Utils.glueIterable(sortedProjects, (JavaProject project) ->
							(project.isEnabled() ? "&2" : "&c") + project.getName(),
							"&a" + ", ");
					sender.sendMessage(MessageType.INFO,
							this.colorizer.colorize("Projects (&2loaded&a/&cunloaded&a): " + projectsStr + "."));
				} else {
					sender.sendMessage(MessageType.ERROR, "Too many arguments.");
				}
				return;
				
			case "recompile":
				
				// "<prefix> recompile".
				if(cmdParts.length == 1) {
					
					// Recompile all projects.
					final List<String> messages = new ArrayList<String>();
					RecompileAllResult result = this.projectManager.recompileAllProjects(
							new RecompileFeedbackHandler() {
						@Override
						public void handleUnloadException(UnloadException e) {
							sender.sendMessage(MessageType.ERROR, "An UnloadException occurred while unloading"
									+ " java project \"" + e.getProject().getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
						@Override
						public void handleLoadException(LoadException e) {
							sender.sendMessage(MessageType.ERROR, "A LoadException occurred while loading"
									+ " java project \"" + e.getProject().getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
						@Override
						public void handleCompileException(CompileException e) {
							sender.sendMessage(MessageType.ERROR, "A CompileException occurred while compiling"
									+ " java project \"" + e.getProject().getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
						@Override
						public void compilerFeedback(String feedback) {
							messages.add(feedback);
						}
					}, this.projectStateListener);
					
					// Give compiler feedback.
					if(!messages.isEmpty() && compilerFeedbackLimit > 0) {
						String feedback = "";
						
						// Add at max all but one feedback string.
						for(int i = 0; i < messages.size() - 1; i++) {
							if(i >= compilerFeedbackLimit) {
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
						feedback = feedback.replace("\t", "    ");
						sender.sendMessage(MessageType.ERROR, "Compiler feedback:\n"
								+ this.colorizer.colorize("&6") + feedback);
					}
					
					// Give feedback.
					sender.sendMessage(MessageType.INFO, new String[] {
						"Recompile complete.",
						"    Projects added: " + result.addedProjects.size(),
						"    Projects removed: " + result.removedProjects.size(),
						"    Projects compiled: " + result.compiledProjects.size(),
						"    Projects unloaded: " + result.unloadedProjects.size(),
						"    Projects loaded: " + result.loadedProjects.size(),
						"    Projects with errors: " + result.errorProjects.size()
					});
					return;
				}
				
				// "<prefix> recompile <projectName>".
				if(cmdParts.length == 2) {
					final String projectName = cmdParts[1];
					
					// Get the project. Attempt to add it from the file system if it does not yet exist in the
					// project manager.
					JavaProject project = this.projectManager.getProject(projectName);
					if(project == null) {
						project = this.projectManager
								.addProjectFromProjectDirectory(projectName, this.projectStateListener);
						if(project == null) {
							sender.sendMessage(MessageType.ERROR, "Project does not exist: \"" + projectName + "\".");
							return;
						}
					}
					
					// Unload and remove the project if it was deleted from the file system.
					List<JavaProject> removedProjects = this.projectManager.unloadAndRemoveProjectIfDeleted(
							projectName, (UnloadException e) -> {
						sender.sendMessage(MessageType.ERROR, "An UnloadException occurred in"
								+ " java project \"" + e.getProject().getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					});
					if(removedProjects != null) {
						if(removedProjects.isEmpty()) {
							sender.sendMessage(MessageType.INFO, "Removed project because it no longer exists in"
									+ " the file system: \"" + projectName + "\".");
						} else {
							sender.sendMessage(MessageType.INFO, "Removed and unloaded project because it no"
									+ " longer exists in the file system: \"" + projectName + "\".");
							if(removedProjects.size() > 1) {
								assert(removedProjects.get(0).getName().equals(projectName));
								removedProjects.remove(0);
								sender.sendMessage(MessageType.INFO, "The following " + (removedProjects.size() == 1
										? "dependent was" : "dependents were") + " unloaded: "
										+ Utils.glueIterable(removedProjects, (JavaProject p) -> p.getName(), ", ")
										+ ".");
							}
						}
						return;
					}
					
					// Recompile the project.
					boolean success = false;
					final List<String> messages = new ArrayList<String>();
					try {
						this.projectManager.recompile(project,
								(String compilerFeedback) -> messages.add(compilerFeedback),
								(UnloadException e) -> sender.sendMessage(MessageType.ERROR, (e.getCause() == null
										? "UnloadException: " + e.getMessage() : "An UnloadException occurred in java"
										+ " project \"" + e.getProject().getName() + "\":\n"
										+ Utils.getStacktrace(e))));
						success = true;
					} catch (CompileException e) {
						sender.sendMessage(MessageType.ERROR, (e.getCause() == null
								? "CompileException: " + e.getMessage() : "A CompileException occurred in java"
								+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
					} catch (LoadException e) {
						sender.sendMessage(MessageType.ERROR, (e.getCause() == null
								? "LoadException: " + e.getMessage() : "A LoadException occurred in java"
								+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
					} catch (DepOrderViolationException e) {
						sender.sendMessage(MessageType.ERROR, (e.getCause() == null
								? "DepOrderViolationException: " + e.getMessage() : "A DepOrderViolationException"
								+ " occurred in java project \"" + e.getProject().getName() + "\":\n"
								+ Utils.getStacktrace(e)));
					} catch (IllegalArgumentException e) {
						throw new Error("Project is obtained from this manager, so this should be impossible.", e);
					}
					
					// Give compiler feedback.
					if(!messages.isEmpty() && compilerFeedbackLimit > 0) {
						String feedback = "";
						
						// Add at max all but one feedback string.
						for(int i = 0; i < messages.size() - 1; i++) {
							if(i >= compilerFeedbackLimit) {
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
						sender.sendMessage(MessageType.ERROR, "Compiler feedback:\n"
								+ this.colorizer.colorize("&6") + feedback);
					}
					
					// Send feedback.
					sender.sendMessage(MessageType.INFO,
							"Recompile complete" + (success ? "" : " (with errors)") + ".");
					
				} else {
					sender.sendMessage(MessageType.ERROR, "Too many arguments.");
				}
				return;
				
			case "unload":
				
				// "<prefix> unload".
				if(cmdParts.length == 1) {
					
					// Unload all projects.
					Set<JavaProject> unloadedProjects = this.projectManager.unloadAllProjects((UnloadException ex) -> {
						sender.sendMessage(MessageType.ERROR, "An UnloadException occurred while"
								+ " unloading java project \"" + ex.getProject().getName() + "\":"
								+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
					});
					
					// Remove no longer existing projects from the project manager.
					Set<JavaProject> removedProjects = this.projectManager.removeUnloadedProjectsIfDeleted();
					
					// Send feedback.
					sender.sendMessage(MessageType.INFO, "Unloaded " + unloadedProjects.size()
							+ " project" + (unloadedProjects.size() == 1 ? "" : "s") + ".");
					if(removedProjects.size() != 0) {
						sender.sendMessage(MessageType.INFO, "Removed " + removedProjects.size()
								+ " project" + (removedProjects.size() == 1 ? "" : "s")
								+ " due to their project directory no longer existing.");
					}
					return;
				}
				
				// "<prefix> unload <projectName>".
				if(cmdParts.length == 2) {
					String projectName = cmdParts[1];
					JavaProject project = this.projectManager.getProject(projectName);
					
					// Check if the project exists.
					if(project == null) {
						sender.sendMessage(MessageType.ERROR, "Project does not exist: " + projectName);
						return;
					}
					
					// Unload the project if it was loaded.
					if(project.isEnabled()) {
						try {
							project.unload(UnloadMethod.EXCEPTION_ON_LOADED_DEPENDENTS, (UnloadException e) -> {
								sender.sendMessage(MessageType.ERROR, "An UnloadException occurred while unloading"
										+ " java project \"" + project.getName() + "\":"
										+ (e.getCause() == null
												? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
							});
							sender.sendMessage(MessageType.INFO, "Project unloaded: " + projectName);
						} catch (UnloadException e) {
							sender.sendMessage(MessageType.ERROR, "An UnloadException occurred while unloading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					} else {
						sender.sendMessage(MessageType.ERROR, "Project was not enabled: " + projectName);
					}
					
					// Remove the project if it was successfully disabled and does not exist anymore.
					JavaProject removedProject = this.projectManager.removeUnloadedProjectIfDeleted(projectName);
					if(removedProject != null) {
						sender.sendMessage(MessageType.INFO,
								"Removed project due to its project directory no longer existing: " + projectName);
					}
					
				} else {
					sender.sendMessage(MessageType.ERROR, "Too many arguments.");
				}
				return;
			case "load":
				
				// "<prefix> load".
				if(cmdParts.length == 1) {
					
					// Add new projects (happens when a new project directory is created).
					this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
					
					// Load all projects.
					LoadAllResult loadAllResult = this.projectManager.loadAllProjects((LoadException ex) -> {
						sender.sendMessage(MessageType.ERROR, "A LoadException occurred while loading"
								+ " java project \"" + ex.getProject().getName() + "\":"
								+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
					});
					
					// Send feedback.
					sender.sendMessage(MessageType.INFO, "Loaded " + loadAllResult.loadedProjects.size()
							+ " project" + (loadAllResult.loadedProjects.size() == 1 ? "" : "s") + ".");
					return;
				}
				
				// "<prefix> load <projectName>".
				if(cmdParts.length == 2) {
					String projectName = cmdParts[1];
					JavaProject project = this.projectManager.getProject(projectName);
					
					// Check if the project exists. Add the project from the filesystem if it was added.
					if(project == null) {
						
						// Attempt to load the project from file. This works if it has been added during runtime.
						project = this.projectManager.addProjectFromProjectDirectory(
								projectName, this.projectStateListener);
						
						// Print an error if the project does not exist.
						if(project == null) {
							sender.sendMessage(MessageType.ERROR, "Project does not exist: " + projectName);
							return;
						}
						
					}
					
					// Load the project if it wasn't loaded.
					if(!project.isEnabled()) {
						try {
							project.load();
							sender.sendMessage(MessageType.INFO, "Project loaded: " + projectName);
						} catch (LoadException e) {
							sender.sendMessage(MessageType.ERROR, "A LoadException occurred while loading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					} else {
						sender.sendMessage(MessageType.ERROR, "Project already loaded: " + projectName);
					}
					
				} else {
					sender.sendMessage(MessageType.ERROR, "Too many arguments.");
				}
				return;
			case "exit":
				if(this.exitCommandHandler != null) {
					this.exitCommandHandler.onExitCommand();
					return;
				}
				// Intended fall through to default case.
			default:
				sender.sendMessage(MessageType.ERROR,
						"Unknown " + (this.commandPrefix.isEmpty() ? "" : "sub") + "command: " + cmdParts[0]);
				return;
		}
	}
	
	/**
	 * Used for handling the exit command in the {@link CommandExecutor}.
	 * @author P.J.S. Kools
	 */
	public static interface ExitCommandHandler {
		void onExitCommand();
	}
	
	/**
	 * Used for handling command feedback.
	 * @author P.J.S. Kools
	 */
	public static interface CommandSender {
		void sendMessage(MessageType messageType, String message);
		void sendMessage(MessageType messageType, String... messages);
	}
	
	/**
	 * Used for colorizing feedback messages.
	 * @author P.J.S. Kools
	 */
	public static interface FeedbackColorizer {
		
		/**
		 * Colorizes the given string, using '&' as colorize character. Accepted input is &1, &2, ..., &9, &a, ..., &f
		 * and &r to reset all colors. To print color character '&', type '&&'.
		 * @param str - The string to colorize.
		 * @return The colorized string or null when 'str' is null.
		 * @throws FormatException When the colorize character is found in combination with an invalid color character.
		 */
		String colorize(String str);
	}
	
	/**
	 * Represents the type of a feedback message.
	 * @author P.J.S. Kools
	 */
	public static enum MessageType {
		INFO,
		ERROR
	}
}
