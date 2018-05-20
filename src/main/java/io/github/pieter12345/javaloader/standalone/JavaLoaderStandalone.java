package io.github.pieter12345.javaloader.standalone;

import java.io.File;
import java.io.FileOutputStream;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import io.github.pieter12345.javaloader.JavaLoaderProject;
import io.github.pieter12345.javaloader.JavaProject;
import io.github.pieter12345.javaloader.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.ProjectManager;
import io.github.pieter12345.javaloader.ProjectManager.RecompileAllResult;
import io.github.pieter12345.javaloader.ProjectManager.RecompileFeedbackHandler;
import io.github.pieter12345.javaloader.ProjectStateListener;
import io.github.pieter12345.javaloader.exceptions.CompileException;
import io.github.pieter12345.javaloader.exceptions.DepOrderViolationException;
import io.github.pieter12345.javaloader.exceptions.LoadException;
import io.github.pieter12345.javaloader.exceptions.UnloadException;
import io.github.pieter12345.javaloader.utils.AnsiColor;
import io.github.pieter12345.javaloader.utils.Utils;

/**
 * JavaLoaderStandalone class.
 * This is the main class for the JavaLoader standalone version.
 * @author P.J.S. Kools
 */
public class JavaLoaderStandalone {
	
	// Variables & Constants.
	private static final String VERSION;
	private static final String PREFIX_INFO =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.RED + " ";
	
	private static final int COMPILER_FEEDBACK_LIMIT = 5; // The max amount of warnings/errors to print per recompile.
	
	private ProjectManager projectManager;
	private final File projectsDir = new File(new File("").getAbsolutePath() + "/JavaProjects");
	private ProjectStateListener projectStateListener;
	
	private boolean enabled = true;
	
	static {
		// Get the version from the manifest.
		Package pack = JavaLoaderStandalone.class.getPackage();
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
	
	public static void main(String[] args) {
		final JavaLoaderStandalone javaLoaderStandalone = new JavaLoaderStandalone();
		
		// Add a shutdown hook to unload projects before the JVM is being terminated.
		// This does not always run on shutdown.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				printFeedback("The JVM is being terminated. Unloading all projects.");
				if(javaLoaderStandalone.projectManager != null) {
					javaLoaderStandalone.projectManager.unloadAllProjects((UnloadException ex) -> {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + ex.getProject().getName() + "\":"
								+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
					});
				}
				printFeedback("Projects unloaded.");
			}
		});
	}
	
	public JavaLoaderStandalone() {
		
		// Check if a JDK is available, otherwise disable the plugin.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			throw new RuntimeException("No java compiler available. This software requires a JDK to run on.");
		}
		
		// Create the "/JavaLoader/JavaProjects" directory if it doesn't exist and initialize it with an example.
		if(!this.projectsDir.exists()) {
			this.projectsDir.mkdir();
			this.createExampleProject();
		}
		
		// Create the project manager.
		this.projectManager = new ProjectManager(this.projectsDir);
		
		// Initialize project state listener.
		this.projectStateListener = new ProjectStateListener() {
			
			@Override
			public void onLoad(JavaProject project) {
				
				// Initialize the project instance class with the JavaProject.
				project.getInstance().initialize(project);
			}
			
			@Override
			public void onUnload(JavaProject project) {
			}
		};
		
		// Loop over all project directories and add them as a JavaProject.
		this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
		
		// Load all projects.
		Set<JavaProject> loadedProjects = this.projectManager.loadAllProjects((LoadException ex) -> {
			printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		
		// Print feedback.
		JavaProject[] projects = this.projectManager.getProjects();
		printFeedback("JavaLoader " + VERSION + " started. " + loadedProjects.size() + "/"
				+ projects.length + " projects loaded.");
		
		// Start the input reader.
		Scanner scanner = new Scanner(System.in);
		while(this.enabled) {
			if(scanner.hasNextLine()) {
				String line = scanner.nextLine().replaceAll("[\r\n\t]", " ").trim();
				if(line.isEmpty()) {
					continue;
				}
				String[] lineParts = line.split(" ");
				String command = lineParts[0];
				String[] args = new String[lineParts.length - 1];
				System.arraycopy(lineParts, 1, args, 0, args.length);
				this.processCommand(command, args);
			}
		}
		scanner.close();
		
	}
	
	public void processCommand(String command, String[] args) {
		
		switch(command.toLowerCase()) {
			case "help":
				
				// "help [command]".
				if(args.length == 0) {
					printFeedback(PREFIX_INFO + colorize("&aJavaLoader - Version: &8" + VERSION + "&a."
							+ " Author:&8 Pieter12345/woesh0007&a."
							+ "\n&6  - help [subcommand]"
							+ "\n&3	  Displays this page or information about the subcommand."
							+ "\n&6  - list"
							+ "\n&3   Displays a list of all projects and their status."
							+ "\n&6  - recompile [project]"
							+ "\n&3	  Recompiles, unloads and loads the given or all projects."
							+ "\n&6  - unload [project]"
							+ "\n&3	  Unloads the given or all projects."
							+ "\n&6  - load [project]"
							+ "\n&3	  Loads the given or all projects."));
				} else if(args.length == 1) {
					switch(args[0].toLowerCase()) {
						case "help":
							printFeedback(PREFIX_INFO + colorize("&6help &8-&3 Displays command help."));
							return;
						case "list":
							printFeedback(PREFIX_INFO + colorize(
									"&6list &8-&3 Displays a list of all projects and their status."));
							return;
						case "recompile":
							printFeedback(PREFIX_INFO + colorize("&6recompile [project] &8-&3 Recompiles,"
									+ " unloads and loads the given project or all projects when no project is given."
									+ " Recompiling happens before projects are unloaded, so the old project will stay"
									+ " loaded when a recompile Exception occurs."));
							return;
						case "load":
							printFeedback(PREFIX_INFO + colorize("&6load [project] &8-&3 Loads the given"
									+ " project or all projects when no project is given. To load a project, only the"
									+ " .class files in the project folder have to be valid."
									+ " This will also load newly added projects."));
							return;
						case "unload":
							printFeedback(PREFIX_INFO + colorize("&6unload [project] &8-&3 Unloads the given"
									+ " project or all projects when no project is given."
									+ " Projects that no longer exist will be removed."));
							return;
						default:
							printFeedback(PREFIX_ERROR + "Unknown command: " + args[0]);
							return;
					}
				} else {
					printFeedback(PREFIX_ERROR + "Too many arguments.");
				}
				return;
				
			case "list":
				
				// "list".
				if(args.length == 0) {
					
					// TODO - Consider adding new projects from the file system here.
					
					// Get all projects and sort them.
					JavaProject[] projects = this.projectManager.getProjects();
					List<JavaProject> sortedProjects = Arrays.<JavaProject>asList(projects);
					sortedProjects.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
					
					// Give feedback for having no projects available.
					if(projects.length == 0) {
						printFeedback(PREFIX_INFO + "There are no projects available.");
						return;
					}
					
					// Construct the feedback message for >=1 projects available.
					String projectsStr = Utils.glueIterable(sortedProjects, (JavaProject project) ->
							(project.isEnabled() ? AnsiColor.GREEN : AnsiColor.RED_BRIGHT) + project.getName(),
							AnsiColor.GREEN_BRIGHT + ", ");
					String message = colorize("Projects (&2loaded&a/&cunloaded&a): " + projectsStr + ".");
					
					// Send the feedback.
					printFeedback(PREFIX_INFO + message);
				} else {
					printFeedback(PREFIX_ERROR + "Too many arguments.");
				}
				return;
				
			case "recompile":
				
				// "recompile".
				if(args.length == 0) {
					
					// Recompile all projects.
					final List<String> messages = new ArrayList<String>();
					RecompileAllResult result = this.projectManager.recompileAllProjects(
							new RecompileFeedbackHandler() {
						@Override
						public void handleUnloadException(UnloadException e) {
							printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
									+ " java project \"" + e.getProject().getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
						@Override
						public void handleLoadException(LoadException e) {
							printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
									+ " java project \"" + e.getProject().getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
						@Override
						public void handleCompileException(CompileException e) {
							printFeedback(PREFIX_ERROR + "A CompileException occurred while compiling"
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
						printFeedback(PREFIX_ERROR + "Compiler feedback:\n"
								+ AnsiColor.YELLOW + feedback + AnsiColor.RESET);
					}
					
					// Give feedback.
					printFeedback(PREFIX_INFO + "Recompile complete."
							+ "\n    Projects added: " + result.addedProjects.size()
							+ "\n    Projects removed: " + result.removedProjects.size()
							+ "\n    Projects compiled: " + result.compiledProjects.size()
							+ "\n    Projects unloaded: " + result.unloadedProjects.size()
							+ "\n    Projects loaded: " + result.loadedProjects.size()
							+ "\n    Projects with errors: " + result.errorProjects.size());
					return;
				}
				
				// "recompile <projectName>".
				if(args.length == 1) {
					String projectName = args[0];
					
					// Get the project. Attempt to add it from the file system if it does not yet exist in the
					// project manager.
					JavaProject project = this.projectManager.getProject(projectName);
					if(project == null) {
						project = this.projectManager
								.addProjectFromProjectDirectory(projectName, this.projectStateListener);
						if(project == null) {
							printFeedback(PREFIX_ERROR + "Project does not exist: \"" + projectName + "\".");
							return;
						}
					}
					
					// Unload and remove the project if it was deleted from the file system.
					List<JavaProject> removedProjects = this.projectManager.unloadAndRemoveProjectIfDeleted(
							projectName, (UnloadException e) -> {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred in"
								+ " java project \"" + e.getProject().getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					});
					if(removedProjects != null) {
						if(removedProjects.isEmpty()) {
							printFeedback(PREFIX_INFO + "Removed project because it no longer exists in the file"
									+ " system: \"" + projectName + "\".");
						} else {
							printFeedback(PREFIX_INFO + "Removed and unloaded project because it no longer exists in"
									+ " the file system: \"" + projectName + "\".");
							if(removedProjects.size() > 1) {
								assert(removedProjects.get(0).getName().equals(projectName));
								removedProjects.remove(0);
								printFeedback(PREFIX_INFO + "The following " + (removedProjects.size() == 1
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
								(UnloadException e) -> printFeedback(PREFIX_ERROR + (e.getCause() == null
										? "UnloadException: " + e.getMessage() : "An UnloadException occurred in java"
										+ " project \"" + e.getProject().getName() + "\":\n"
										+ Utils.getStacktrace(e))));
						success = true;
					} catch (CompileException e) {
						printFeedback(PREFIX_ERROR + (e.getCause() == null
								? "CompileException: " + e.getMessage() : "A CompileException occurred in java"
								+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
					} catch (LoadException e) {
						printFeedback(PREFIX_ERROR + (e.getCause() == null
								? "LoadException: " + e.getMessage() : "A LoadException occurred in java"
								+ " project \"" + e.getProject().getName() + "\":\n" + Utils.getStacktrace(e)));
					} catch (DepOrderViolationException e) {
						printFeedback(PREFIX_ERROR + (e.getCause() == null
								? "DepOrderViolationException: " + e.getMessage() : "A DepOrderViolationException"
								+ " occurred in java project \"" + e.getProject().getName() + "\":\n"
								+ Utils.getStacktrace(e)));
					} catch (IllegalArgumentException e) {
						throw new InternalError(
								"Project is obtained from this manager, so this should be impossible.", e);
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
						printFeedback(PREFIX_ERROR + "Compiler feedback:\n"
								+ AnsiColor.YELLOW + feedback + AnsiColor.RESET);
					}
					
					// Send feedback.
					printFeedback(PREFIX_INFO + "Recompile complete" + (success ? "" : " (with errors)") + ".");
					
				} else {
					printFeedback(PREFIX_ERROR + "Too many arguments.");
				}
				return;
				
			case "unload":
				
				// "unload".
				if(args.length == 0) {
					
					// Unload all projects.
					Set<JavaProject> unloadedProjects = this.projectManager.unloadAllProjects((UnloadException ex) -> {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + ex.getProject().getName() + "\":"
								+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
					});
					
					// Remove no longer existing projects from the project manager.
					Set<JavaProject> removedProjects = this.projectManager.removeUnloadedProjectsIfDeleted();
					
					// Send feedback.
					printFeedback(PREFIX_INFO + "Unloaded " + unloadedProjects.size()
							+ " project" + (unloadedProjects.size() == 1 ? "" : "s") + ".");
					if(removedProjects.size() != 0) {
						printFeedback(PREFIX_INFO + "Removed " + removedProjects.size()
								+ " project" + (removedProjects.size() == 1 ? "" : "s")
								+ " due to their project directory no longer existing.");
					}
					return;
				}
				
				// "unload <projectName>".
				if(args.length == 1) {
					String projectName = args[0];
					JavaProject project = this.projectManager.getProject(projectName);
					
					// Check if the project exists.
					if(project == null) {
						printFeedback(PREFIX_ERROR + "Project does not exist: " + projectName);
						return;
					}
					
					// Unload the project if it was loaded.
					if(project.isEnabled()) {
						try {
							project.unload(UnloadMethod.EXCEPTION_ON_LOADED_DEPENDENTS, (UnloadException e) -> {
								printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
										+ " java project \"" + project.getName() + "\":"
										+ (e.getCause() == null ? " " + e.getMessage() : "\n"
										+ Utils.getStacktrace(e)));
							});
							printFeedback(PREFIX_INFO + "Project unloaded: " + projectName);
						} catch (UnloadException e) {
							printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					} else {
						printFeedback(PREFIX_ERROR + "Project was not enabled: " + projectName);
					}
					
					// Remove the project if it was successfully disabled and does not exist anymore.
					JavaProject removedProject = this.projectManager.removeUnloadedProjectIfDeleted(projectName);
					if(removedProject != null) {
						printFeedback(PREFIX_INFO
								+ "Removed project due to its project directory no longer existing: " + projectName);
					}
					
				} else {
					printFeedback(PREFIX_ERROR + "Too many arguments.");
				}
				return;
				
			case "load":
				
				// "load".
				if(args.length == 0) {
					
					// Add new projects (happens when a new project directory is created).
					this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
					
					// Load all projects.
					Set<JavaProject> loadedProjects = this.projectManager.loadAllProjects((LoadException ex) -> {
						printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
								+ " java project \"" + ex.getProject().getName() + "\":"
								+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
					});
					
					// Send feedback.
					printFeedback(PREFIX_INFO + "Loaded " + loadedProjects.size()
							+ " project" + (loadedProjects.size() == 1 ? "" : "s") + ".");
					return;
				}
				
				// "load <projectName>".
				if(args.length == 1) {
					String projectName = args[0];
					JavaProject project = this.projectManager.getProject(projectName);
					
					// Check if the project exists. Add the project from the filesystem if it was added.
					if(project == null) {
						
						// Attempt to load the project from file. This works if it has been added during runtime.
						project = this.projectManager.addProjectFromProjectDirectory(
								projectName, this.projectStateListener);
						
						// Print an error if the project does not exist.
						if(project == null) {
							printFeedback(PREFIX_ERROR + "Project does not exist: " + projectName);
							return;
						}
						
					}
					
					// Load the project if it wasn't loaded.
					if(!project.isEnabled()) {
						try {
							project.load();
							printFeedback(PREFIX_INFO + "Project loaded: " + projectName);
						} catch (LoadException e) {
							printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					} else {
						printFeedback(PREFIX_ERROR + "Project already loaded: " + projectName);
					}
					
				} else {
					printFeedback(PREFIX_ERROR + "Too many arguments.");
				}
				return;
				
			case "exit":
				this.projectManager.unloadAllProjects((UnloadException ex) -> {
					printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
							+ " java project \"" + ex.getProject().getName() + "\":"
							+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
				});
				this.enabled = false;
				printFeedback("Stopping JavaLoader.");
				return;
				
			default:
				printFeedback(PREFIX_ERROR + "Unknown command: " + command);
				return;
		}
	}
	
	private static String colorize(String str) {
		return AnsiColor.colorize(str);
	}
	
	private void createExampleProject() {
		try {
			CodeSource codeSource = JavaLoaderStandalone.class.getProtectionDomain().getCodeSource();
			if(codeSource != null) {
				if(codeSource.getLocation().getPath().endsWith("/")) {
					throw new Exception("Creation of example projects is not supported when running from an IDE.");
				}
				ZipInputStream inStream = new ZipInputStream(codeSource.getLocation().openStream());
				
				ZipEntry zipEntry;
				while((zipEntry = inStream.getNextEntry()) != null) {
					String name = zipEntry.getName();
					if(name.startsWith("exampleprojects/standalone/")) {
						File targetFile = new File(this.projectsDir
								+ "/" + name.substring("exampleprojects/standalone/".length()));
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
			} else {
				throw new NullPointerException("CodeSource is null.");
			}
		} catch (Exception e) {
			printFeedback(PREFIX_ERROR + "Failed to create example projects."
					+ " Here's the stacktrace:\n" + Utils.getStacktrace(e));
			return;
		}
	}
	
	private static void printFeedback(String str) {
		System.out.println(AnsiColor.stripColors(str)); // TODO - Add ANSI color code support (jansi?).
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
}
