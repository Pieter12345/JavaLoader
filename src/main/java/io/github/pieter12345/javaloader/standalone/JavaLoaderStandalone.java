package io.github.pieter12345.javaloader.standalone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import io.github.pieter12345.javaloader.JavaProject;
import io.github.pieter12345.javaloader.ProjectStateListener;
import io.github.pieter12345.javaloader.JavaProject.CompileException;
import io.github.pieter12345.javaloader.JavaProject.LoadException;
import io.github.pieter12345.javaloader.JavaProject.UnloadException;
import io.github.pieter12345.javaloader.utils.AnsiColor;
import io.github.pieter12345.javaloader.utils.Utils;

public class JavaLoaderStandalone {
	
	// Variables & Constants.
	private HashMap<String, JavaProject> projects = null;
	private static final String VERSION;
	private static final String PREFIX_INFO =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.RED + " ";
	private final File projectsDir = new File(new File("").getAbsolutePath() + "/JavaProjects");
	private ProjectStateListener projectStateListener;
	
	private boolean enabled = true;
	
	public static void main(String[] args) {
		final JavaLoaderStandalone javaLoaderStandalone = new JavaLoaderStandalone();
		
		// Add a shutdown hook to unload projects before the JVM is being terminated.
		// This does not always run on shutdown.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				printFeedback("The JVM is being terminated. Unloading all projects.");
				javaLoaderStandalone.unloadAllProjects();
				printFeedback("Projects unloaded.");
			}
		});
	}
	
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
					compile(project, 5);
				} catch (CompileException e) {
					
					// Remove the newly created bin directory.
					Utils.removeFile(project.getBinDir());
					
					// Send feedback.
					printFeedback(PREFIX_ERROR + "A CompileException occurred while compiling"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					continue;
				}
			}
			
			// Load the project.
			try {
				project.load();
			} catch (LoadException e) {
				printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
						+ " java project \"" + project.getName() + "\":"
						+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
			}
		}
		
		// Print feedback.
		printFeedback("JavaLoader " + VERSION + " started.");
		
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
	
	public void unloadAllProjects() {
		
		// Unload all loaded projects.
		if(this.projects != null) {
			for(JavaProject project : this.projects.values()) {
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
			}
		}
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
				case "recompile":
					printFeedback(PREFIX_INFO + colorize("&6recompile [project] &8-&3 Recompiles,"
							+ " unloads and loads the given project or all projects when no project is given."
							+ " Recompiling happens before projects are unloaded, so the old project will stay loaded"
							+ " when a recompile Exception occurs."));
					return;
				case "load":
					printFeedback(PREFIX_INFO + colorize("&6load [project] &8-&3 Loads the given"
							+ " project or all projects when no project is given. To load a project, only the .class"
							+ " files in the project folder have to be valid."
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
			
		case "recompile":
			
			// "/recompile".
			if(args.length == 0) {
				
				// Remove unexisting projects (happens when a project directory is removed).
				removeRemovedProjects();
				
				// Add new projects (happens when a new project directory is created).
				checkAndAddNewProjects();
				
				// Store all projects that gave an error so they can be excluded from next steps.
				ArrayList<JavaProject> errorProjects = new ArrayList<JavaProject>();
				
				// Recompile everything to a "bin_new" directory.
				for(JavaProject project : this.projects.values()) {
					project.setBinDirName("bin_new");
					try {
						compile(project, 5);
					} catch (CompileException e) {
						
						// Remove the newly created bin directory and set the old one.
						Utils.removeFile(project.getBinDir());
						project.setBinDirName("bin");
						
						// Send feedback.
						printFeedback(PREFIX_ERROR + "A CompileException occurred while compiling"
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
							printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
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
						printFeedback(PREFIX_ERROR + "Failed to rename \"bin_new\" to \"bin\" because the \"bin\""
								+ " directory could not be removed for project \"" + project.getName() + "\"."
								+ " This can be fixed manually or by attempting another recompile. The project has"
								+ " already been disabled and some files of the \"bin\" directory might be removed.");
						errorProjects.add(project);
						continue;
					}
					if(!newBinDir.renameTo(project.getBinDir())) {
						printFeedback(PREFIX_ERROR
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
						printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						errorProjects.add(project);
					}
				}
				
				// Send feedback.
				int count = this.projects.size();
				int errorCount = errorProjects.size();
				if(errorCount == 0) {
					printFeedback(PREFIX_INFO + "Successfully compiled and loaded "
				+ count + (count == 1 ? " project" : " projects") + ".");
				} else {
					printFeedback(PREFIX_INFO + "Compiled and loaded " + count
							+ (count == 1 ? " project" : " projects") + " of which " + errorCount + " failed.");
				}
				
			}
			// "recompile <projectName>".
			else if(args.length == 1) {
				String projectName = args[0];
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
						printFeedback(PREFIX_ERROR + "Project does not exist: " + args[0]);
						return;
					}
				} else {
					
					// Check if the project directory was removed.
					if(!project.getProjectDir().isDirectory()) {
						if(project.isEnabled()) {
							try {
								project.unload();
							} catch (UnloadException e) {
								printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading REMOVED"
										+ " java project \"" + project.getName() + "\":"
										+ (e.getCause() == null ?
												" " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
							}
						}
						this.projects.remove(projectName);
						printFeedback(PREFIX_INFO + "Project unloaded and removed because the project directory"
								+ " no longer exists: " + projectName);
						return;
					}
				}
				
				// Recompile the project to a "bin_new" directory.
				project.setBinDirName("bin_new");
				try {
					compile(project, 5);
				} catch (CompileException e) {
					
					// Remove the newly created bin directory and set the old one.
					Utils.removeFile(project.getBinDir());
					project.setBinDirName("bin");
					
					// Send feedback and return.
					printFeedback(PREFIX_ERROR + "A CompileException occurred while compiling"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					return;
				}
				File newBinDir = project.getBinDir();
				project.setBinDirName("bin");
				
				
				// Unload the project if it was loaded.
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
				
				// Replace the current "bin" directory with "bin_new" and remove "bin_new".
				if(project.getBinDir().exists() && !Utils.removeFile(project.getBinDir())) {
					printFeedback(PREFIX_ERROR + "Failed to rename \"bin_new\" to \"bin\" because the \"bin\""
							+ " directory could not be removed for project \"" + project.getName() + "\"."
							+ " This can be fixed manually or by attempting another recompile. The project has"
							+ " already been disabled and some files of the \"bin\" directory might be removed.");
					return;
				}
				if(!newBinDir.renameTo(project.getBinDir())) {
					printFeedback(PREFIX_ERROR
							+ "Failed to rename \"bin_new\" to \"bin\" for project \"" + project.getName() + "\"."
							+ " This can be fixed manually or by attempting another recompile."
							+ " The project has already been disabled and the \"bin\" directory has been removed.");
					return;
				}
				
				// Load the project.
				try {
					project.load();
				} catch (LoadException e) {
					printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
							+ " java project \"" + project.getName() + "\":"
							+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					return;
				}
				
				// Send feedback.
				printFeedback(PREFIX_INFO + "Successfully compiled and loaded project: " + project.getName() + ".");
				
			} else {
				printFeedback(PREFIX_ERROR + "Too many arguments.");
			}
			return;
			
		case "unload":
			// "unload".
			if(args.length == 0) {
				
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
							printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading"
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
				printFeedback(PREFIX_INFO
						+ "Unloaded " + unloadCount + " project" + (unloadCount == 1 ? "" : "s") + ".");
			}
			// "unload <projectName>".
			else if(args.length == 1) {
				String projectName = args[0];
				JavaProject project = this.projects.get(projectName);
				
				// Check if the project exists.
				if(project == null) {
					printFeedback(PREFIX_ERROR + "Project does not exist: " + projectName);
					return;
				}
				
				// Unload the project if it was loaded.
				if(project.isEnabled()) {
					try {
						project.unload();
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
				if(!project.isEnabled() && !project.getProjectDir().exists()) {
					this.projects.remove(project);
				}
				
			} else {
				printFeedback(PREFIX_ERROR + "Too many arguments.");
			}
			return;
		case "load":
			// "load".
			if(args.length == 0) {
				
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
							printFeedback(PREFIX_ERROR + "A LoadException occurred while loading"
									+ " java project \"" + project.getName() + "\":"
									+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
						}
					}
				}
				printFeedback(PREFIX_INFO + "Loaded " + loadCount + " project" + (loadCount == 1 ? "" : "s") + ".");
			}
			// "load <projectName>".
			else if(args.length == 1) {
				String projectName = args[0];
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
			this.unloadAllProjects();
			this.enabled = false;
			printFeedback("Stopping JavaLoader.");
			return;
		default:
			printFeedback(PREFIX_ERROR + "Unknown command: " + command);
			return;
		}
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
	
	private void removeRemovedProjects() {
		// Remove unexisting projects (happens when a project directory is removed).
		for(Iterator<JavaProject> iterator = this.projects.values().iterator(); iterator.hasNext();) {
			JavaProject project = iterator.next();
			if(!project.getProjectDir().exists()) {
				if(project.isEnabled()) {
					try {
						project.unload();
					} catch (UnloadException e) {
						printFeedback(PREFIX_ERROR + "An UnloadException occurred while unloading REMOVED"
								+ " java project \"" + project.getName() + "\":"
								+ (e.getCause() == null ? " " + e.getMessage() : "\n" + Utils.getStacktrace(e)));
					}
				}
				iterator.remove();
			}
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
	
	private static void compile(JavaProject project, int feedbackLimit) throws CompileException {
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
			printFeedback(PREFIX_ERROR + "Compiler feedback:\n" + AnsiColor.YELLOW + feedback + AnsiColor.RESET);
		}
		
		// Rethrow if compilation caused an Exception.
		if(ex != null) {
			throw ex;
		}
	}
	
	private static void printFeedback(String str) {
		System.out.println(AnsiColor.stripColors(str)); // TODO - Add ANSI color code support (jansi?).
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
}
