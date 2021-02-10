package io.github.pieter12345.javaloader.velocity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import io.github.pieter12345.javaloader.core.CommandExecutor;
import io.github.pieter12345.javaloader.core.JavaLoaderProject;
import io.github.pieter12345.javaloader.core.JavaProject;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.ProjectStateListener;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.ProjectManager.LoadAllResult;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.utils.AnsiColor;
import io.github.pieter12345.javaloader.core.utils.Utils;
import io.github.pieter12345.javaloader.velocity.command.JavaLoaderCommand;

/**
 * JavaLoaderVelocityPlugin class.
 * This is the main class that will be loaded by Velocity.
 * @author P.J.S. Kools
 */
@Plugin(id = "javaloader", name = "JavaLoader", version = JavaLoaderVelocityPlugin.VERSION,
		authors = {JavaLoaderVelocityPlugin.AUTHOR}, description = "A plugin for the Velocity Minecraft proxy that"
				+ " allows you to compile, enable, disable and hotswap Java projects in runtime.")
public class JavaLoaderVelocityPlugin {
	
	// Variables & Constants.
	private static final String PREFIX_INFO =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.GREEN + " ";
	private static final String PREFIX_ERROR =
			AnsiColor.YELLOW + "[" + AnsiColor.CYAN + "JavaLoader" + AnsiColor.YELLOW + "]" + AnsiColor.RED + " ";
	
	protected static final String VERSION = "0.0.6-SNAPSHOT"; // TODO - Replace with "${version}" + Maven handling
	protected static final String AUTHOR = "Pieter12345/Woesh0007";
	
	private static final int COMPILER_FEEDBACK_LIMIT = 5; // The max amount of warnings/errors to print per recompile.
	
	private final ProxyServer proxy;
	private final Logger logger;
	
	private boolean enabled = false;
	
	private ProjectManager projectManager;
	private final File projectsDir = new File(
			"plugins" + File.separator + "JavaLoader" + File.separator + "JavaProjects").getAbsoluteFile();
	private ProjectStateListener projectStateListener;
	
	@Inject
	public JavaLoaderVelocityPlugin(ProxyServer proxy, Logger logger) {
		this.proxy = proxy;
		this.logger = logger;
	}
	
	public ProxyServer getProxyServer() {
		return this.proxy;
	}
	
	public Logger getLogger() {
		return this.logger;
	}
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		this.enable();
	}
	
	public void enable() {
		
		// Return if the plugin is already enabled.
		if(this.enabled) {
			return;
		}
		
		// Check if a JDK is available, otherwise disable the plugin.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			this.logger.error("No java compiler available."
					+ " This plugin requires the server to be started from a JDK version (Java Development Kit),"
					+ " rather than a JRE version (Java Runtime Environment). Disabling plugin.");
			return;
		}
		
		// Create the "/plugins/JavaLoader/JavaProjects" directory if it doesn't exist
		// and initialize it with an example.
		if(!this.projectsDir.exists()) {
			this.projectsDir.mkdirs();
			this.createExampleProject();
		}
		
		// Create the project manager.
		this.projectManager = new ProjectManager(this.projectsDir, new ProjectDependencyParser());
		
		// Initialize project state listener.
		this.projectStateListener = new ProjectStateListener() {
			
			@Override
			public void onLoad(JavaProject project) throws LoadException {
				
				// Initialize the project instance class with the ProxyServer and a new Logger.
				if(project.getInstance() instanceof JavaLoaderVelocityProject) {
					JavaLoaderVelocityProject veloProjectInstance = (JavaLoaderVelocityProject) project.getInstance();
					veloProjectInstance.initialize(
							project, JavaLoaderVelocityPlugin.this.proxy, LoggerFactory.getLogger(project.getName()));
				} else {
					project.getInstance().initialize(project);
				}
			}
			
			@Override
			public void onUnload(JavaProject project) throws UnloadException {
			}
		};
		
		// Register "/javaloader" command.
		CommandExecutor commandExecutor = new CommandExecutor(this.projectManager, this.projectStateListener, null,
				"/javaloader", Arrays.asList(AUTHOR), VERSION,
				(String str) -> AnsiColor.colorize(str), COMPILER_FEEDBACK_LIMIT);
		this.proxy.getCommandManager().register("javaloader",
				new JavaLoaderCommand(PREFIX_INFO, PREFIX_ERROR, commandExecutor, this.projectManager));
		
		// Loop over all project directories and add them as a JavaProject.
		this.projectManager.addProjectsFromProjectDirectory(this.projectStateListener);
		
		// Load all projects.
		LoadAllResult loadAllResult = this.projectManager.loadAllProjects((LoadException ex) -> {
			this.logger.error("A LoadException occurred while loading"
					+ " java project \"" + ex.getProject().getName() + "\":"
					+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
		});
		
		// Set enabled state.
		this.enabled = true;
		
		// Print feedback.
		JavaProject[] projects = this.projectManager.getProjects();
		this.logger.info("JavaLoader " + VERSION + " enabled. "
				+ loadAllResult.loadedProjects.size() + "/" + projects.length + " projects loaded.");
	}
	
	public void disable() {
		
		// Return if the plugin is already disabled.
		if(!this.enabled) {
			return;
		}
		
		// Unregister "/javaloader" command.
		this.proxy.getCommandManager().unregister("javaloader");
		
		// Unload all loaded projects and remove them from the project manager.
		if(this.projectManager != null) {
			this.projectManager.clear((UnloadException ex) -> {
				this.logger.error("An UnloadException occurred while unloading"
						+ " java project \"" + ex.getProject().getName() + "\":"
						+ (ex.getCause() == null ? " " + ex.getMessage() : "\n" + Utils.getStacktrace(ex)));
			});
		}
		this.projectManager = null;
		this.projectStateListener = null;
		
		// Set enabled state.
		this.enabled = false;
		
		// Print feedback.
		this.logger.info("JavaLoader " + VERSION + " disabled.");
	}
	
	private void createExampleProject() {
		try {
			CodeSource codeSource = JavaLoaderVelocityPlugin.class.getProtectionDomain().getCodeSource();
			if(codeSource != null) {
				
				// Select a source to copy from (directory (IDE) or jar (production)).
				if(codeSource.getLocation().getPath().endsWith("/")) {
					
					// The code is being ran from a non-jar source. Get the projects base directory.
					File exampleProjectsBaseDir = new File(
							URLDecoder.decode(codeSource.getLocation().getPath(), "UTF-8")
							+ "exampleprojects/velocity");
					
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
						if(name.startsWith("exampleprojects/velocity/")) {
							File targetFile = new File(this.projectsDir,
									name.substring("exampleprojects/velocity/".length()));
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
			this.logger.error("Failed to create example projects."
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
}
