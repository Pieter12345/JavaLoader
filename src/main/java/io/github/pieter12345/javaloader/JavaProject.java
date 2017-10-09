package io.github.pieter12345.javaloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import io.github.pieter12345.javaloader.utils.Utils;

import javax.tools.JavaCompiler.CompilationTask;

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
	private boolean enabled = false;
	private String version = null;
	private ProjectStateListener stateListener;
	
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
	 * Constructor.
	 * Creates a new JavaProject with the given parameters.
	 * @param projectName - The name of the project.
	 * @param projectDir - The project directory (containing the src directory).
	 * @param stateListener - The ProjectStateListener which will be called on load/unload. This can be used to
	 * execute Minecraft server implementation dependent code such as injecting and removing commands and listeners.
	 */
	public JavaProject(String projectName, File projectDir, ProjectStateListener stateListener) {
		this.projectName = projectName;
		this.projectDir = projectDir;
		this.binDir = new File(this.projectDir.getAbsolutePath() + "/bin");
		this.srcDir = new File(this.projectDir.getAbsolutePath() + "/src");
		this.stateListener = stateListener;
	}
	
	/**
	 * Constructor.
	 * Creates a new JavaProject with the given parameters.
	 * @param projectName - The name of the project.
	 * @param projectDir - The project directory (containing the src directory).
	 */
	public JavaProject(String projectName, File projectDir) {
		this(projectName, projectDir, null);
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
			
			// Get the dependencies.
			String[] dependencies = this.getDependencies();
			
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
				throw new CompileException("No sourcefiles found.");
			}
			
			// Remove the bin directory.
			if(this.binDir.exists() && !Utils.removeFile(this.binDir)) {
				throw new CompileException("Unable to remove bin directory at: " + this.binDir.getAbsolutePath());
			}
			
			// Create the new bin directory.
			if(!this.binDir.mkdir()) {
				throw new CompileException("Unable to create bin directory at: " + this.binDir.getAbsolutePath());
			}
			
			// Get the name of the .jar file of this plugin
			// (Using the JavaLoaderProject class because that one is required for all projects).
			java.security.CodeSource codeSource = JavaLoaderProject.class.getProtectionDomain().getCodeSource();
			if(codeSource == null || codeSource.getLocation().getFile().isEmpty()) {
				throw new CompileException("Unable to include this"
						+ " plugins .jar file to the classpath because the CodeSource returned null.");
			}
			String pluginJarFilePath = codeSource.getLocation().toURI().getPath();
			
			// Get the complete classpath (including the binDir and passed classpath entries such as jar file paths).
			String classpath = System.getProperty("java.class.path") + CLASSPATH_SEPERATOR + pluginJarFilePath;
			for(String dependency : dependencies) {
				classpath = classpath + ";" + dependency;
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
				throw new CompileException("No java compiler available. This plugin requires a JDK to run on.");
			}
			StandardJavaFileManager fileManager =
					compiler.getStandardFileManager(null, Locale.US, StandardCharsets.UTF_8);
			CompilationTask compileTask = compiler.getTask(feedbackWriter, fileManager,
					null, options, null, fileManager.getJavaFileObjects(files.toArray(new File[0])));
			try { fileManager.close(); } catch (IOException e) { e.printStackTrace(); } // Never happens.
			boolean success = compileTask.call();
			if(!success) {
				throw new CompileException("Javac compile unsuccessfull.");
			}
			
		} catch (Exception e) {
			if(e instanceof CompileException) {
				throw (CompileException) e;
			}
			throw new CompileException(e);
		}
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
		
		// Get the dependencies.
		File[] dependencies;
		try {
			String[] dependencyStrs = this.getDependencies();
			dependencies = new File[dependencyStrs.length];
			for(int i = 0; i < dependencyStrs.length; i++) {
				dependencies[i] = new File(dependencyStrs[i]);
			}
		} catch (FileNotFoundException e) {
			throw new LoadException(e);
		} catch (IOException e) {
			throw new LoadException("An IOException occurred while getting dependencies.", e);
		}
		
		// Define the classloader.
		try {
			this.classLoader = new JavaProjectClassLoader(this.binDir, dependencies);
		} catch (FileNotFoundException e) {
			throw new LoadException(e);
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
							throw new LoadException("Unable to load class while it is certainly"
									+ " in the bin directory (ClassNotFoundException): " + className);
						} catch (NoClassDefFoundError e) {
							throw new LoadException("Unable to load class "
									+ "(NoClassDefFoundError, class contains a reference to an undefined class): " + className, e);
						}
					}
				}
			}
		}
		if(mainClasses.size() == 0) {
			throw new LoadException(
					"No main class found (one class has to extend from " + JavaLoaderProject.class.getName() + ").");
		}
		if(mainClasses.size() > 1) {
			throw new LoadException("Multiple main classes found"
					+ " (only one class may extend from " + JavaLoaderProject.class.getName() + ").");
		}
		Class<?> mainClass = mainClasses.get(0);
		
		// Instantiate the main class.
		try {
			this.projectInstance = (JavaLoaderProject) mainClass.newInstance();
		} catch (InstantiationException e) {
			throw new LoadException("The main class (" + mainClass.getName() + ") could not be instantiated."
					+ " This could be caused by the absence of a"
					+ " nullary constructor in the class or because the class is not an implemented class.");
		} catch (IllegalAccessException e) {
			throw new LoadException("The main class (" + mainClass.getName() + ") could not be accessed."
					+ " Make sure the default constructor is public or no constructors are defined.");
		} catch (NoClassDefFoundError e) {
			throw new LoadException("The main class (" + mainClass.getName() + ") could not be instanciated because a"
					+ " class (or library) it depends on is missing (NoClassDefFoundError). If you have removed a class"
					+ " after the last recompile,"
					+ " executing a recompile will fix this since the project has been unloaded at this point.", e);
		}
		
		// Get the project version (This has to happen before calling the onLoad(...) method on the stateListener).
		try {
			this.version = this.projectInstance.getVersion();
		} catch (Exception e) {
			throw new LoadException("An Exception occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".getVersion(). Is the project up to date?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e), e);
		}
		
		// Notify the listener if it's set.
		if(this.stateListener != null) {
			try {
				this.stateListener.onLoad(this);
			} catch (Exception e) {
				throw new LoadException("An unexpected Exception occurred in StateListener's onLoad() method."
						+ " This is likely a bug.", e);
			}
		}
		
		// Start the project.
		try {
			this.projectInstance.onLoad();
			this.enabled = true;
		} catch (Exception e) {
			throw new LoadException("An Exception occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".onLoad(). Is the project up to date?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e), e);
		} catch (NoClassDefFoundError e) {
			throw new LoadException("A NoClassDefFoundError occured in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".onLoad(). Is the compiled project missing a dependency?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e), e);
		}
		
	}
	
	/**
	 * unload method.
	 * Unloads the JavaProject.
	 * @throws UnloadException If an Exception occurs while unloading the project.
	 */
	public void unload() throws UnloadException {
		if(!this.enabled) {
			return;
		}
		
		// Notify the listener if it's set.
		if(this.stateListener != null) {
			try {
				this.stateListener.onUnload(this);
			} catch (Exception e) {
				// This should never happen.
				throw new UnloadException("An unexpected Exception occurred in StateListener's onUnload() method."
						+ " This is likely a bug.", e);
			}
		}
		
		// Unload the project.
		try {
			this.projectInstance.onUnload();
			this.enabled = false;
			this.version = null;
		} catch (Exception e) {
			throw new UnloadException("An Exception occurred in " + this.projectDir.getName() + "'s "
					+ this.projectInstance.getClass().getName() + ".onUnload(). Is the project up to date?"
					+ " Stacktrace:\n" + Utils.getStacktrace(e), e);
		}
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
	 * @return The project main class instance or null when the project has not been loaded.
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
	
	private String[] getDependencies() throws IOException, FileNotFoundException {
		File dependencyFile = new File(this.projectDir.getAbsolutePath() + "/dependencies.txt");
		if(!dependencyFile.isFile()) {
			return new String[0]; // No dependencies available.
		}
		byte[] fileBytes = new byte[(int) dependencyFile.length()];
		FileInputStream inStream = new FileInputStream(dependencyFile);
		inStream.read(fileBytes);
		inStream.close();
		String fileStr = new String(fileBytes, StandardCharsets.UTF_8);

		// Remove cariage returns and tabs.
		fileStr = fileStr.replaceAll("[\r\t]", "");
		
		// Remove leading and trailing whitespaces.
		fileStr = fileStr.replaceAll("(?<=(^|\n))[ ]+(?! )", "");
		fileStr = fileStr.replaceAll("(?<! )[ ]+(?=(\n|$))", "");
		
		// Remove comments starting with "//" or "#".
		fileStr = fileStr.replaceAll("(\\/\\/|\\#)[^$\n]*(?=(\n|$))", "");
		
		// Remove empty lines.
		fileStr = fileStr.replaceAll("(?<=(^|\n))[\n]+(?!$)", "");
		
		// Split and verify the dependencies.
		if(fileStr.isEmpty()) {
			return new String[0];
		}
		String[] dependencies = fileStr.split("\n");
		for(int i = 0; i < dependencies.length; i++) {
			
			// Change "project ProjectName" to the bin directory of that project.
			if(dependencies[i].toLowerCase().startsWith("project ")) {
				String projectName = dependencies[i].substring("project ".length()).trim();
				// TODO - Match the project with all known projects and get their bin directory through them
				// (without depending on the Bukkit package).
				// TODO - Also check for circular dependencies.
				// TODO - Base compile order on project dependencies.
				dependencies[i] = this.projectDir.getParentFile().getAbsolutePath() + "/" + projectName + "/bin";
			}
			
			// Make relative paths relative to the project directory.
			if(dependencies[i].startsWith(".") && (dependencies[i].length() == 1
					|| dependencies[i].charAt(1) == '/' || dependencies[i].charAt(1) == '\\')) {
				dependencies[i] = this.projectDir.getAbsolutePath() + dependencies[i].substring(1);
			}
			
			// Verify that the dependencies exist.
			File depFile = new File(dependencies[i]);
			if(!depFile.exists()) {
				throw new FileNotFoundException("Dependency file not found: " + dependencies[i]);
			}
		}
		
		// Return the dependencies.
		return dependencies;
	}
	
	@SuppressWarnings("serial")
	public static class CompileException extends Exception {
		public CompileException() {
			super();
		}
		public CompileException(String message) {
			super(message);
		}
		public CompileException(String message, Throwable cause) {
			super(message, cause);
		}
		public CompileException(Throwable cause) {
			super(cause);
		}
	}
	
	@SuppressWarnings("serial")
	public static class LoadException extends Exception {
		public LoadException() {
			super();
		}
		public LoadException(String message) {
			super(message);
		}
		public LoadException(String message, Throwable cause) {
			super(message, cause);
		}
		public LoadException(Throwable cause) {
			super(cause);
		}
	}
	
	@SuppressWarnings("serial")
	public static class UnloadException extends Exception {
		public UnloadException() {
			super();
		}
		public UnloadException(String message) {
			super(message);
		}
		public UnloadException(String message, Throwable cause) {
			super(message, cause);
		}
		public UnloadException(Throwable cause) {
			super(cause);
		}
	}
}
