package io.github.pieter12345.javaloader.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import io.github.pieter12345.javaloader.core.CommandExecutor;
import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.ProjectStateListener;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.CommandExecutor.CommandSender;
import io.github.pieter12345.javaloader.core.CommandExecutor.MessageType;
import io.github.pieter12345.javaloader.core.ProjectManager.LoadAllResult;
import io.github.pieter12345.javaloader.core.exceptions.DuplicateProjectIdentifierException;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.utils.AnsiColor;
import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * JavaLoaderStandalone class.
 * This is the main class for the JavaLoader standalone version.
 * @author P.J.S. Kools
 */
public class JavaLoaderStandalone {
	
	// Variables & Constants.
	private static final String VERSION;
	private static final List<String> AUTHORS = Arrays.asList("Pieter12345/Woesh0007");
	private static final String PREFIX_INFO =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.RED + " ";
	
	private static final int COMPILER_FEEDBACK_LIMIT = 5; // The max amount of warnings/errors to print per recompile.
	
	private ProjectManager projectManager;
	private final File projectsDir;
	private ProjectStateListener projectStateListener;
	
	private CommandExecutor commandExecutor;
	
	private boolean enabled = false;
	
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
				if(javaLoaderStandalone.enabled) {
					printFeedback("The JVM is being terminated. Unloading all projects.");
					javaLoaderStandalone.stop();
					printFeedback("Projects unloaded.");
				}
			}
		});
		
		// Start JavaLoader.
		javaLoaderStandalone.start();
	}
	
	/**
	 * Creates a JavaLoader standalone instance. The projects directory will be set to 'JavaProjects' in the same
	 * directory as the JavaLoader jar file.
	 */
	public JavaLoaderStandalone() {
		this(new File(new File("").getAbsoluteFile(), "JavaProjects"));
	}
	
	/**
	 * Creates a JavaLoader standalone instance.
	 * @param projectsDir - The directory which JavaLoader will use to search for project directories in.
	 */
	public JavaLoaderStandalone(File projectsDir) {
		this.projectsDir = projectsDir;
	}
	
	/**
	 * Starts the JavaLoader standalone version.
	 */
	public void start() {
		
		// Set the projects directory.
		if(this.projectsDir.isFile()) {
			throw new IllegalStateException("Projects directory path leads to an existing file (and not a directory): "
					+ this.projectsDir.getAbsolutePath());
		}
		
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
		this.projectManager = new ProjectManager(this.projectsDir, new ProjectDependencyParser());
		
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
		
		// Initialize the command executor.
		this.commandExecutor = new CommandExecutor(this.projectManager, this.projectStateListener, () -> {
			printFeedback("Unloading all projects.");
			this.stop();
			printFeedback("Stopping JavaLoader.");
		}, null, AUTHORS, VERSION, (String str) -> AnsiColor.colorize(str), COMPILER_FEEDBACK_LIMIT);
		
		// Loop over all project directories and add them as a JavaProject.
		this.projectManager.addProjectsFromProjectDirectory(
				this.projectStateListener, (DuplicateProjectIdentifierException ex) -> {
			printFeedback(PREFIX_ERROR + "Multiple projects found with name \"" + ex.getProjectName() + "\" at: "
					+ Utils.glueIterable(ex.getProjectDirs(), (File f) -> f.getAbsolutePath(), ", "));
		});
		
		// Load all projects.
		LoadAllResult loadAllResult = this.projectManager.loadAllProjects((LoadException ex) -> {
			printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		
		// Print feedback.
		JavaProject[] projects = this.projectManager.getProjects();
		printFeedback("JavaLoader " + VERSION + " started. " + loadAllResult.loadedProjects.size() + "/"
				+ projects.length + " projects loaded.");
		
		// Set the enabled flag.
		this.enabled = true;
		
		// Start the input reader on a new thread.
		new Thread(() -> {
			Scanner scanner = new Scanner(System.in);
			while(this.enabled) {
				if(scanner.hasNextLine()) {
					String command = scanner.nextLine().replaceAll("[\r\n\t]", " ").trim();
					if(command.isEmpty()) {
						continue;
					}
					this.executeCommand(command);
				}
				
				// Sleep for a short while to ease the CPU when reading System.in does not block.
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
			scanner.close();
		}).start();
		
	}
	
	/**
	 * Unloads all projects and stops the System.in listener thread. If exceptions occur during project unloading,
	 * they will be printed to the console. Does nothing if {@link JavaLoaderStandalone} is not running.
	 */
	public void stop() {
		if(!this.enabled) {
			return;
		}
		this.projectManager.unloadAllProjects((UnloadException ex) -> {
			printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		this.projectManager = null;
		this.projectStateListener = null;
		this.commandExecutor = null;
		this.enabled = false;
	}
	
	/**
	 * Executes the given command.
	 * @param command - The command to execute.
	 */
	public void executeCommand(String command) {
		String[] cmdParts = command.split(" ");
		this.commandExecutor.executeCommand(new CommandSender() {
			@Override
			public void sendMessage(MessageType messageType, String message) {
				printFeedback(this.getPrefix(messageType) + message);
			}
			@Override
			public void sendMessage(MessageType messageType, String... messages) {
				this.sendMessage(messageType, Utils.glueIterable(Arrays.asList(messages), (str) -> str, "\n"));
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
		}, cmdParts);
	}
	
	private void createExampleProject() {
		try {
			CodeSource codeSource = JavaLoaderStandalone.class.getProtectionDomain().getCodeSource();
			if(codeSource != null) {
				
				// Select a source to copy from (directory (IDE) or jar (production)).
				if(codeSource.getLocation().getPath().endsWith("/")) { // Has a '/' separator regardless of the OS.
					
					// The code is being ran from a non-jar source. Get the projects base directory.
					File exampleProjectsBaseDir = new File(
							URLDecoder.decode(codeSource.getLocation().getPath(), "UTF-8")
							+ "exampleprojects" + File.separator + "standalone");
					
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
						if(name.startsWith("exampleprojects/standalone/")) {
							File targetFile = new File(this.projectsDir,
									name.substring("exampleprojects/standalone/".length()));
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
	 * @return The JavaLoaderProject instance or null if no project with the given name is loaded.
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
