package io.github.pieter12345.javaloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import io.github.pieter12345.javaloader.dependency.Dependency;
import io.github.pieter12345.javaloader.dependency.DependencyScope;
import io.github.pieter12345.javaloader.dependency.FileDependency;
import io.github.pieter12345.javaloader.dependency.JarDependency;
import io.github.pieter12345.javaloader.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.utils.Utils;

import javax.tools.JavaCompiler.CompilationTask;

import com.google.common.io.Files;

/**
 * JavaProject class.
 * Represents a Java project.
 * @author P.J.S. Kools
 */
public class JavaProject {
	
	// Variables & Constants.
	private final File projectDir;
	private final String projectName;
	private File binDir;
	private File srcDir;
	private JavaProjectClassLoader classLoader = null;
	private JavaLoaderProject projectInstance = null;
	private Dependency[] dependencies = null;
	private boolean enabled = false;
	private String version = null;
	private final ProjectStateListener stateListener;
	private final ProjectManager manager;
	
	private static final char CLASSPATH_SEPERATOR;
	
	static {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.startsWith("win")) {
			CLASSPATH_SEPERATOR = ';';
		} else if(os.equals("linux") || os.equals("mac os x")) {
			CLASSPATH_SEPERATOR = ':';
		} else {
			// Unknown OS. Default to ':' and hope it'll work.
			CLASSPATH_SEPERATOR = ':';
		}
		
	}
	
	/**
	 * Creates a new JavaProject with the given parameters and loads its compiled dependencies if available.
	 * @param projectName - The name of the project.
	 * @param projectDir - The project directory (containing the src directory).
	 * @param manager - The project manager used to resolve project dependencies.
	 * @param stateListener - The ProjectStateListener which will be called on load/unload. This can be used to
	 * execute Minecraft server implementation dependent code such as injecting and removing commands and listeners.
	 */
	public JavaProject(String projectName, File projectDir,
			ProjectManager manager, ProjectStateListener stateListener) {
		this.projectName = projectName;
		this.projectDir = projectDir;
		this.binDir = new File(this.projectDir.getAbsolutePath() + "/bin");
		this.srcDir = new File(this.projectDir.getAbsolutePath() + "/src");
		this.stateListener = stateListener;
		this.manager = manager;
	}
	
	/**
	 * Creates a new JavaProject with the given parameters and loads its compiled dependencies if available.
	 * @param projectName - The name of the project.
	 * @param projectDir - The project directory (containing the src directory).
	 * @param manager - The project manager used to resolve project dependencies.
	 */
	public JavaProject(String projectName, File projectDir, ProjectManager manager) {
		this(projectName, projectDir, manager, null);
	}
	
	/**
	 * compile method.
	 * Compiles the JavaProject.
	 * @param feedbackWriter - A Writer to write all compile errors/warnings from the java compiler to.
	 *  If this is null, System.err will be used.
	 * @throws CompileException If an Exception occurs while compiling the project.
	 */
	public void compile(Writer feedbackWriter) throws CompileException {
		try {
			
			// Get the dependencies and validate their existence.
			// For JavaLoader projects, also validate that the project exists in the project manger.
			final File dependenciesFile = new File(this.projectDir.getAbsolutePath() + "/dependencies.txt");
			Dependency[] dependencies = this.readDependencies(dependenciesFile);
			List<File> dependencyFiles = new ArrayList<File>();
			for(Dependency dependency : dependencies) {
				
				// Handle project dependencies.
				if(dependency instanceof ProjectDependency) {
					ProjectDependency projectDependency = (ProjectDependency) dependency;
					File file = projectDependency.getFile();
					
					// Validate that the project exists in the project manager.
					if(file == null) {
						throw new CompileException(this,
								"Dependency project not found: " + projectDependency.getProjectName());
					}
					
					// Validate that the projects binary directory (containing the actual dependency files) exists.
					if(!file.exists()) {
						throw new CompileException(this, "Dependency project exists, but has not been compiled: "
								+ projectDependency.getProjectName());
					}
					
					// Add the projects binary directory to the list.
					dependencyFiles.add(file);
					
					continue;
				}
				
				// Handle file dependencies.
				if(dependency instanceof FileDependency) {
					FileDependency fileDependency = (FileDependency) dependency;
					File file = fileDependency.getFile();
					if(!file.exists()) {
						throw new CompileException(this, "Dependency file does not exist: " + file.getAbsolutePath());
					}
					dependencyFiles.add(file);
					continue;
				}
				
				// Handle unsupported dependencies.
				throw new CompileException(this,
						"Unsupported dependency implementation: " + dependency.getClass().getName());
			}
			
			// List all .java files in the source directory.
			ArrayList<File> files = new ArrayList<File>();
			Stack<File> dirStack = new Stack<File>();
			dirStack.push(this.srcDir);
			while(!dirStack.isEmpty()) {
				File[] localFiles = dirStack.pop().listFiles();
				if(localFiles != null) {
					for(File localFile : localFiles) {
						if(localFile.isDirectory()) {
							dirStack.push(localFile);
						} else if(localFile.getName().endsWith(".java")) {
							files.add(localFile);
						}
					}
				}
			}
			if(files.size() == 0) {
				throw new CompileException(this, "No sourcefiles found.");
			}
			
			// Remove the bin directory.
			if(this.binDir.exists() && !Utils.removeFile(this.binDir)) {
				throw new CompileException(this, "Unable to remove bin directory at: " + this.binDir.getAbsolutePath());
			}
			
			// Create the new bin directory.
			if(!this.binDir.mkdir()) {
				throw new CompileException(this, "Unable to create bin directory at: " + this.binDir.getAbsolutePath());
			}
			
			// Get the name of the .jar file of this plugin
			// (Using the JavaLoaderProject class because that one is required for all projects).
			java.security.CodeSource codeSource = JavaLoaderProject.class.getProtectionDomain().getCodeSource();
			if(codeSource == null || codeSource.getLocation().getFile().isEmpty()) {
				throw new CompileException(this, "Unable to include this"
						+ " plugins .jar file to the classpath because the CodeSource returned null.");
			}
			String pluginJarFilePath = codeSource.getLocation().toURI().getPath();
			
			// Get the complete classpath (including the binDir and passed classpath entries such as jar file paths).
			String classpath = System.getProperty("java.class.path") + CLASSPATH_SEPERATOR + pluginJarFilePath;
			for(File dependencyFile : dependencyFiles) {
				classpath = classpath + ";" + dependencyFile.getAbsolutePath();
			}
			
			// Create the compiler options array.
			ArrayList<String> options = new ArrayList<String>();
			options.add("-classpath");
			options.add(classpath);
			options.add("-d");
			options.add(this.binDir.getAbsolutePath());
			options.add("-Xlint:deprecation");
			
			// Compile the files.
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if(compiler == null) {
				throw new CompileException(this, "No java compiler available. This plugin requires a JDK to run on.");
			}
			StandardJavaFileManager fileManager =
					compiler.getStandardFileManager(null, Locale.US, StandardCharsets.UTF_8);
			CompilationTask compileTask = compiler.getTask(feedbackWriter, fileManager,
					null, options, null, fileManager.getJavaFileObjects(files.toArray(new File[0])));
			try { fileManager.close(); } catch (IOException e) { e.printStackTrace(); } // Never happens.
			boolean success = compileTask.call();
			if(!success) {
				throw new CompileException(this, "Javac compile unsuccessfull.");
			}
			
			// Compilation succeeded, so store the dependencies and copy them into the bin directory.
			this.dependencies = dependencies;
			if(dependenciesFile.exists()) {
				Files.copy(dependenciesFile, new File(this.binDir.getAbsolutePath() + "/dependencies.txt"));
			}
			
		} catch (Exception e) {
			if(e instanceof CompileException) {
				throw (CompileException) e;
			}
			throw new CompileException(this, e);
		}
	}
	
	/**
	 * compile method.
	 * Compiles the JavaProject.
	 * @param feedbackHandler - A feedback handler to send all compile errors/warnings from the java compiler to.
	 * @throws CompileException If an Exception occurs while compiling the project.
	 */
	public void compile(CompilerFeedbackHandler feedbackHandler) throws CompileException {
		
		// Create a writer that divides compiler feedback into a list of warnings/errors.
		final Writer writer = new Writer() {
			private String buff = "";
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				
				// Get the message.
				char[] toWrite = new char[len];
				System.arraycopy(cbuf, off, toWrite, 0, len);
				String message = new String(toWrite).replace("\r", "");
				
				// Divide the message in parts and add them as seperate warnings/errors.
				// The compiler sends entire lines and seperate newlines.
				if(message.equals("\n") || message.startsWith("\t") || message.startsWith(" ")) {
					this.buff += message;
				} else {
					if(!this.buff.isEmpty()) {
						feedbackHandler.compilerFeedback(this.buff);
					}
					this.buff = message;
				}
			}
			@Override
			public void flush() { }
			@Override
			public void close() {
				if(!this.buff.isEmpty()) {
					feedbackHandler.compilerFeedback(this.buff);
					this.buff = null;
				}
			}
		};
		
		// Perform the compile.
		CompileException ex = null;
		try {
			this.compile(writer);
		} catch (CompileException e) {
			ex = e;
		}
		
		// Close the writer.
		try {
			writer.close(); // Potentially sends the last compiler feedback.
		} catch (IOException e) {
			// Never happens.
		}
		
		// Rethrow if an exception has occurred.
		if(ex != null) {
			throw ex;
		}
	}
	
	public static interface CompilerFeedbackHandler {
		void compilerFeedback(String feedback);
	}

	
	/**
	 * load method.
	 * Loads the JavaProject.
	 * @throws LoadException If an Exception occurs while loading the project.
	 */
	public void load() throws LoadException {
		if(this.enabled) {
			return;
		}
		
		// Validate that at least the binary directory exists.
		if(!this.binDir.exists()) {
			throw new LoadException(this, "Project has not been compiled.");
		}
		
		// Load dependencies if they have not been initialized yet.
		if(this.dependencies == null) {
			try {
				this.initDependencies();
			} catch (IOException | DependencyException e) {
				throw new LoadException(this, e.getMessage());
			}
		}
		
		// Get the INCLUDE dependency files for the classloader. Existence of files will be checked by the classloader,
		// but we will validate that JavaProject dependencies that are marked as PROVIDED are loaded here.
		List<File>dependencyFiles = new ArrayList<File>();
		if(this.dependencies != null) {
			for(int i = 0; i < this.dependencies.length; i++) {
				Dependency dependency = this.dependencies[i];
				if(dependency instanceof ProjectDependency) {
					
					// Validate that JavaProject dependencies that are marked as PROVIDED are loaded.
					ProjectDependency projectDependency = (ProjectDependency) dependency;
					if(projectDependency.getScope() == DependencyScope.PROVIDED
							&& !projectDependency.getProject().isEnabled()) {
						throw new LoadException(this,
								"Dependency project not loaded: " + projectDependency.getProject().getName());
					}
					
				} else if(dependency instanceof FileDependency) {
					
					// Add file dependencies marked as INCLUDE.
					if(dependency.getScope() == DependencyScope.INCLUDE) {
						FileDependency fileDependency = (FileDependency) dependency;
						dependencyFiles.add(fileDependency.getFile());
					}
					
				} else {
					// Even though dependencies marked as PROVIDED could be ignored, this will force us to review
					// this part of the code when adding a new dependency implementation.
					throw new LoadException(this,
							"Unsupported dependency implementation: " + dependency.getClass().getName());
				}
			}
		}
		
		// Define the classloader.
		try {
			this.classLoader = new JavaProjectClassLoader(this.binDir, dependencyFiles);
		} catch (FileNotFoundException e) {
			throw new LoadException(this, e.getMessage()); // Dependency file does not exist.
		}
		
		// Load all .class files in the bin directory and get the "main" class.
		ArrayList<Class<?>> mainClasses = new ArrayList<Class<?>>();
		Stack<File> dirStack = new Stack<File>();
		Stack<String> packetStack = new Stack<String>();
		dirStack.push(this.binDir);
		packetStack.push("");
		while(!dirStack.isEmpty()) {
			File[] localFiles = dirStack.pop().listFiles();
			String packetStr = packetStack.pop();
			if(localFiles != null) {
				for(File localFile : localFiles) {
					if(localFile.isDirectory()) {
						dirStack.push(localFile);
						packetStack.push(packetStr + localFile.getName() + ".");
					} else if(localFile.getName().endsWith(".class")) {
						String className =
								packetStr + localFile.getName().substring(0, localFile.getName().length() - 6);
						try {
							Class<?> clazz = this.classLoader.loadClass(className);
							if(JavaLoaderProject.class.isAssignableFrom(clazz)) {
								mainClasses.add(clazz);
							}
//							Class<?>[] interfaces = clazz.getInterfaces();
//							for(int i = 0; i < interfaces.length; i++) {
//								if(interfaces[i].getName().equals(JavaLoaderProject.class.getName())) {
//									mainClasses.add(clazz);
//								}
//							}
						} catch (ClassNotFoundException e) {
							throw new LoadException(this, "Unable to load class while it is certainly"
									+ " in the bin directory (ClassNotFoundException): " + className);
						} catch (NoClassDefFoundError e) {
							throw new LoadException(this, "Unable to load class (NoClassDefFoundError,"
									+ " class contains a reference to an undefined class): " + className);
						}
					}
				}
			}
		}
		if(mainClasses.size() == 0) {
			throw new LoadException(this,
					"No main class found (one class has to extend from " + JavaLoaderProject.class.getName() + ").");
		}
		if(mainClasses.size() > 1) {
			throw new LoadException(this, "Multiple main classes found"
					+ " (only one class may extend from " + JavaLoaderProject.class.getName() + ").");
		}
		Class<?> mainClass = mainClasses.get(0);
		
		// Instantiate the main class.
		try {
			this.projectInstance = (JavaLoaderProject) mainClass.newInstance();
		} catch (InstantiationException e) {
			throw new LoadException(this, "The main class (" + mainClass.getName() + ") could not be instantiated."
					+ " This could be caused by the absence of a"
					+ " nullary constructor in the class or because the class is not an implemented class.");
		} catch (IllegalAccessException e) {
			throw new LoadException(this, "The main class (" + mainClass.getName() + ") could not be accessed."
					+ " Make sure the default constructor is public or no constructors are defined.");
		} catch (NoClassDefFoundError e) {
			throw new LoadException(this, "The main class (" + mainClass.getName() + ") could not be instanciated"
					+ " because a class (or library) it depends on is missing (NoClassDefFoundError). If you have"
					+ " removed a class after the last recompile,"
					+ " executing a recompile will fix this since the project has been unloaded at this point.", e);
		}
		
		// Get the project version (This has to happen before calling the onLoad(...) method on the stateListener).
		try {
			this.version = this.projectInstance.getVersion();
		} catch (Exception e) {
			throw new LoadException(this, "An Exception occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".getVersion(). Is the project up to date?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e));
		}
		
		// Notify the listener if it's set.
		if(this.stateListener != null) {
			try {
				this.stateListener.onLoad(this);
			} catch (LoadException e) {
				throw e;
			} catch (Exception e) {
				throw new LoadException(this, "An unexpected Exception occurred in StateListener's onLoad() method."
						+ " This is likely a bug.", e);
			}
		}
		
		// Start the project.
		try {
			this.projectInstance.onLoad();
			this.enabled = true;
		} catch (Exception e) {
			throw new LoadException(this, "An Exception occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".onLoad(). Is the project up to date?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e));
		} catch (NoClassDefFoundError e) {
			throw new LoadException(this, "A NoClassDefFoundError occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".onLoad()."
					+ " Is the compiled project missing a dependency? Stacktrace:\n" + Utils.getStacktrace(e));
		}
		
	}
	
	/**
	 * Unloads the JavaProject. If UnloadExceptions occur during the process, but they do not prevent the project from
	 * unloading, they are passed to the given exHandler.
	 * When 'method' is {@link UnloadMethod#UNLOAD_DEPENDENTS}, exceptions from unloading the dependents will also be
	 * passed to the given exHandler.
	 * @param method - The unloading method to handle.
	 * @param exHandler - The exception handler or null if you wish to ignore possible exceptions.
	 * @throws UnloadException If an Exception occurs that prevents the project from unloading.
	 * This only happens when 'method' is {@link UnloadMethod#EXCEPTION_ON_LOADED_DEPENDENTS} and one or more
	 * dependents are enabled.
	 * @throws NullPointerException If method is null.
	 */
	public void unload(UnloadMethod method,
			UnloadExceptionHandler exHandler) throws UnloadException, NullPointerException {
		if(!this.enabled) {
			return;
		}
		
		// Handle null method.
		if(method == null) {
			throw new NullPointerException("method may not be null.");
		}
		
		// Check if other loaded projects depend on this project.
		if(method != UnloadMethod.IGNORE_DEPENDENTS) {
			List<JavaProject> dependingProjects = new ArrayList<JavaProject>(0);
			for(JavaProject project : this.manager.getProjects()) {
				if(project.isEnabled() && project != this) {
					for(Dependency dep : project.getDependencies()) {
						if(dep instanceof ProjectDependency
								&& ((ProjectDependency) dep).getProject() == this) {
							dependingProjects.add(project);
						}
					}
				}
			}
			if(!dependingProjects.isEmpty()) {
				if(method == UnloadMethod.UNLOAD_DEPENDENTS) {
					
					// Unload the projects recursively.
					for(JavaProject project : dependingProjects) {
						// Will never throw an UnloadException due to the method being 'UNLOAD_DEPENDENTS'.
						project.unload(UnloadMethod.UNLOAD_DEPENDENTS, exHandler);
					}
					
				} else {
					assert(method == UnloadMethod.EXCEPTION_ON_LOADED_DEPENDENTS);
					
					// Throw an exception about the dependents being enabled and therefore being unable to unload.
					dependingProjects.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
					throw new UnloadException(this, "Project cannot be unloaded while there are projects enabled that"
							+ " depend on it. Depending project" + (dependingProjects.size() == 1 ? "" : "s") + ": "
							+ Utils.glueIterable(dependingProjects, (JavaProject p) -> p.getName(), ", ") + ".");
				}
			}
		}
		
		// Notify the listener if it's set.
		if(this.stateListener != null) {
			try {
				this.stateListener.onUnload(this);
			} catch (UnloadException e) {
				exHandler.handleUnloadException(e);
			} catch (Exception e) {
				// This should never happen.
				exHandler.handleUnloadException(new UnloadException(this, "An unexpected Exception occurred in StateListener's"
						+ " onUnload() method. This is a bug in the platform-dependent implementation of project"
						+ " generation of JavaLoader.", e));
			}
		}
		
		// Unload the project.
		if(this.projectInstance != null) { // Can be null when closing the classloader threw an Exception.
			try {
				this.projectInstance.onUnload();
				this.projectInstance = null;
			} catch (Exception e) {
				exHandler.handleUnloadException(new UnloadException(this,
						"An Exception occurred in " + this.projectDir.getName() + "'s "
						+ this.projectInstance.getClass().getName() + ".onUnload(). Is the project up to date?"
						+ " Stacktrace:\n" + Utils.getStacktrace(e)));
			}
		}
		
		// Close the classloader.
		try {
			this.classLoader.close();
		} catch (IOException e) {
			exHandler.handleUnloadException(new UnloadException(this, "An IOException occurred in JavaLoader while"
					+ " closing the classloader for project: \"" + this.projectDir.getName() + "\".", e));
		}
		this.classLoader = null;
		
		// Mark the project as disabled.
		this.enabled = false;
		this.version = null;
		this.dependencies = null; // The user could swap binaries and load again, so reset them.
	}
	
	/**
	 * The method to handle when unloading a project.
	 * @author P.J.S. Kools
	 */
	public static enum UnloadMethod {
		
		/**
		 * If one or multiple projects exist that depend on the project that's being unloaded,
		 * an exception will be thrown.
		 */
		EXCEPTION_ON_LOADED_DEPENDENTS,
		
		/**
		 * If one or multiple projects exist that depend on the project that's being unloaded, they are recursively
		 * being unloaded as well (using the same unload method).
		 */
		UNLOAD_DEPENDENTS,
		
		/**
		 * The project will simply be unloaded, ignoring its dependents and violating the proper unload order.
		 */
		IGNORE_DEPENDENTS;
		
	}
	
	/**
	 * isEnabled method.
	 * @return True is the java project is loaded. False otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}
	
	/**
	 * getName method.
	 * @return The name of the project.
	 */
	public String getName() {
		return this.projectName;
	}
	
	/**
	 * getProjectDir method.
	 * @return The project directory in the JavaProjects directory. This directory might not exist.
	 */
	public File getProjectDir() {
		return this.projectDir;
	}
	
	/**
	 * getBinDir method.
	 * @return The bin directory of the project (used for .class files). This directory might not exist.
	 */
	public File getBinDir() {
		return this.binDir;
	}
	
	/**
	 * setBinDirName method.
	 * @param binDirName - The new name for the binaries directory (used for .class files).
	 *  This directory is in the project directory.
	 */
	public void setBinDirName(String binDirName) {
		this.binDir = 
				new File(this.projectDir.getAbsolutePath() + "/" + binDirName.replaceAll("(\\.\\.|\\\\|\\/)", " "));
	}
	
	/**
	 * getClassLoader method.
	 * @return The ClassLoader used to load the classes for this project.
	 */
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}
	
	/**
	 * getInstance method.
	 * @return The project main class instance or null when the project is not enabled.
	 */
	public JavaLoaderProject getInstance() {
		return this.projectInstance;
	}
	
	/**
	 * getVersion method.
	 * @return The version of the project, specified by the getVersion() method in the main project class.
	 */
	public String getVersion() {
		return this.version;
	}
	
	/**
	 * Gets the ProjectManager containing this project.
	 * @return The ProjectManager containing this project.
	 */
	public ProjectManager getProjectManager() {
		return this.manager;
	}
	
	/**
	 * Gets te dependencies that will be used to load this JavaProject. To get the dependencies that will be used for
	 * the next compile, use the {@link #getSourceDependencies()} method.
	 * @return The dependencies or null if no dependencies were defined.
	 */
	public Dependency[] getDependencies() {
		return (this.dependencies == null ? null : this.dependencies.clone());
	}
	
	/**
	 * Gets te dependencies that will be used to compile this JavaProject. To get the dependencies that will be used for
	 * loading, use the {@link #getDependencies()} method.
	 * This method reads the dependencies again for every call, since the user might change them between calls.
	 * @return The dependencies or null if no dependencies were defined.
	 * @throws DependencyException If a dependency description in the depencendies file is in an invalid format.
	 * @throws IOException If an I/O error occurred while reading the dependencies file.
	 */
	public Dependency[] getSourceDependencies() throws IOException, DependencyException {
		return this.readDependencies(new File(this.projectDir.getAbsolutePath() + "/dependencies.txt"));
	}
	
	/**
	 * Initializes the {@link #dependencies} field using the dependencies file in the binary directory.
	 * Once initialized, calls to this method will be ignored.
	 * If the user would swap the project's binary files between a call to {@link #initDependencies()} and
	 * {@link JavaProject#load()}, the used dependencies will be out of sync with the new binary files. 
	 * @throws IOException When an I/O error occurs while reading the dependencies descriptor file.
	 * @throws DependencyException When the dependencies file contains an invalid descriptor.
	 */
	public void initDependencies() throws IOException, DependencyException {
		/* Load dependencies from the bin directory if they are not yet loaded.
		 * When the project has not been compiled or does not have dependencies, this simply initializes an empty
		 * Dependency array.
		 */
		if(this.dependencies == null) {
			File dependenciesFile = new File(this.binDir.getAbsolutePath() + "/dependencies.txt");
			try {
				this.dependencies = this.readDependencies(dependenciesFile);
			} catch (IOException e) {
				throw new IOException("An I/O error occurred while reading the dependency descriptor file at: "
						+ dependenciesFile.getAbsolutePath());
			} catch (DependencyException e) {
				throw new DependencyException("Invalid dependency descriptor in compiled code."
						+ " Recompile the project to resolve this issue. Exception message: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Reads all dependencies from the given dependencies file.
	 * @param dependencyFile - The file containing the dependency descriptions.
	 * @return An array of dependencies.
	 * If the dependency file does not exist, an empty dependencies array is returned.
	 * @throws DependencyException If a dependency description is in an invalid format.
	 * @throws IOException If an I/O error occurred while reading the dependencyFile.
	 */
	private Dependency[] readDependencies(File dependencyFile) throws IOException, DependencyException {
		if(!dependencyFile.isFile()) {
			return new Dependency[0]; // No dependencies available.
		}
		byte[] fileBytes = new byte[(int) dependencyFile.length()];
		FileInputStream inStream = new FileInputStream(dependencyFile);
		inStream.read(fileBytes);
		inStream.close();
		String dependencyFileContents = new String(fileBytes, StandardCharsets.UTF_8);
		return this.parseDependencies(dependencyFileContents);
	}
	
	/**
	 * Parses the given dependencies string and returns the resulting Dependency objects.
	 * @param dependencyFileContents - The dependencies file contents to parse.
	 * @return An array of dependencies.
	 * @throws DependencyException If a dependency description is in an invalid format.
	 */
	private Dependency[] parseDependencies(String dependencyFileContents) throws DependencyException {
		
		// Replace cariage returns and tabs with whitespaces.
		dependencyFileContents = dependencyFileContents.replaceAll("[\r\t]", " ");
		
		// Remove leading and trailing whitespaces.
		dependencyFileContents = dependencyFileContents.replaceAll("(?<=(^|\n))[ ]+(?! )", "");
		dependencyFileContents = dependencyFileContents.replaceAll("(?<! )[ ]+(?=(\n|$))", "");
		
		// Remove comments starting with "//" or "#".
		dependencyFileContents = dependencyFileContents.replaceAll("(\\/\\/|\\#)[^$\n]*(?=(\n|$))", "");
		
		// Remove empty lines.
		dependencyFileContents = dependencyFileContents.replaceAll("(?<=(^|\n))[\n]+(?!$)", "");
		
		// Split and add the dependencies.
		if(dependencyFileContents.isEmpty()) {
			return new Dependency[0];
		}
		String[] dependencyStrs = dependencyFileContents.split("\n");
		Dependency[] dependencies = new Dependency[dependencyStrs.length];
		for(int i = 0; i < dependencyStrs.length; i++) {
			
			// Handle JavaProject dependencies ("project projectName").
			if(dependencyStrs[i].toLowerCase().startsWith("project ")) {
				String projectName = dependencyStrs[i].substring("project ".length()).trim();
				dependencies[i] = new ProjectDependency(projectName, this.manager);
				continue;
			}
			
			// Handle .jar file dependencies ("jar -provided ./rel/path/to/dep.jar" or "jar C:/path/to/dep.jar").
			if(dependencyStrs[i].toLowerCase().startsWith("jar ")) {
				String dependency = dependencyStrs[i].substring("jar ".length()).trim();
				
				// Validate the .jar prefix.
				if(!dependency.toLowerCase().endsWith(".jar")) {
					throw new DependencyException("Dependency format invalid."
							+ " Jar dependency does not have a .jar extension: " + dependencyStrs[i]);
				}
				
				// Get the dependency scope (include or provided).
				DependencyScope scope;
				if(dependency.startsWith("-")) {
					int ind = dependency.indexOf(' ');
					String arg = (ind == -1 ? dependency.substring(1) : dependency.substring(1, ind));
					if(ind + 1 < dependency.length()) {
						dependency = dependency.substring(ind + 1);
					}
					dependency = dependency.substring(ind + 1);
					switch(arg.toLowerCase()) {
						case "include": {
							scope = DependencyScope.INCLUDE;
							break;
						}
						case "provided": {
							scope = DependencyScope.PROVIDED;
							break;
						}
						default: {
							throw new DependencyException("Dependency format invalid."
									+ " Expected option \"INCLUDE\" or \"PROVIDED\" in syntax:"
									+ " \"jar -option pathToJar\",  but received option: \"" + arg + "\".");
						}
					}
				} else {
					scope = DependencyScope.INCLUDE;
				}
				
				// Get the dependency's file object, supporting relative paths.
				// Relative paths start with a '.' and are relative to the project's directory.
				if(dependency.startsWith(".") && (dependency.length() == 1
						|| dependency.charAt(1) == '/' || dependency.charAt(1) == '\\')) {
					dependency = this.projectDir.getAbsolutePath() + dependency.substring(1);
				}
				File file = new File(dependency);
				
				// Add the dependency to the array.
				dependencies[i] = new JarDependency(file, scope);
				continue;
			}
			
			// Dependency format not recognised.
			throw new DependencyException("Dependency format invalid: " + dependencyStrs[i]);
		}
		
		// Return the dependencies.
		return dependencies;
	}
	
//	@Override
//	public boolean equals(Object obj) {
//		if(obj instanceof JavaProject) {
//			JavaProject other = (JavaProject) obj;
//			return this.projectName.equals(other.projectName)
//					&& this.projectDir.equals(other.projectDir) && this.manager == other.manager;
//		}
//		return false;
//	}
	
	// TODO - Move exceptions to seperate files.
	@SuppressWarnings("serial")
	public static class JavaProjectException extends Exception {
		
		private final JavaProject project;
		
		public JavaProjectException(JavaProject project) {
			super();
			this.project = project;
		}
		public JavaProjectException(JavaProject project, String message) {
			super(message);
			this.project = project;
		}
		public JavaProjectException(JavaProject project, String message, Throwable cause) {
			super(message, cause);
			this.project = project;
		}
		public JavaProjectException(JavaProject project, Throwable cause) {
			super(cause);
			this.project = project;
		}
		
		public JavaProject getProject() {
			return this.project;
		}
	}
	
	@SuppressWarnings("serial")
	public static class CompileException extends JavaProjectException {
		public CompileException(JavaProject project) {
			super(project);
		}
		public CompileException(JavaProject project, String message) {
			super(project, message);
		}
		public CompileException(JavaProject project, String message, Throwable cause) {
			super(project, message, cause);
		}
		public CompileException(JavaProject project, Throwable cause) {
			super(project, cause);
		}
	}
	
	@SuppressWarnings("serial")
	public static class LoadException extends JavaProjectException {
		public LoadException(JavaProject project) {
			super(project);
		}
		public LoadException(JavaProject project, String message) {
			super(project, message);
		}
		public LoadException(JavaProject project, String message, Throwable cause) {
			super(project, message, cause);
		}
		public LoadException(JavaProject project, Throwable cause) {
			super(project, cause);
		}
	}
	
	@SuppressWarnings("serial")
	public static class UnloadException extends JavaProjectException {
		public UnloadException(JavaProject project) {
			super(project);
		}
		public UnloadException(JavaProject project, String message) {
			super(project, message);
		}
		public UnloadException(JavaProject project, String message, Throwable cause) {
			super(project, message, cause);
		}
		public UnloadException(JavaProject project, Throwable cause) {
			super(project, cause);
		}
	}
	
	@SuppressWarnings("serial")
	public static class DependencyException extends Exception {
		public DependencyException() {
			super();
		}
		public DependencyException(String message) {
			super(message);
		}
		public DependencyException(String message, Throwable cause) {
			super(message, cause);
		}
		public DependencyException(Throwable cause) {
			super(cause);
		}
	}
}
