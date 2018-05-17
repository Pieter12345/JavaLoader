package io.github.pieter12345.javaloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.pieter12345.graph.Graph;
import io.github.pieter12345.graph.Graph.ChildBeforeParentGraphIterator;
import io.github.pieter12345.graph.Graph.ParentBeforeChildGraphIterator;
import io.github.pieter12345.javaloader.JavaProject.CompileException;
import io.github.pieter12345.javaloader.JavaProject.CompilerFeedbackHandler;
import io.github.pieter12345.javaloader.JavaProject.DependencyException;
import io.github.pieter12345.javaloader.JavaProject.JavaProjectException;
import io.github.pieter12345.javaloader.JavaProject.LoadException;
import io.github.pieter12345.javaloader.JavaProject.UnloadException;
import io.github.pieter12345.javaloader.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.dependency.Dependency;
import io.github.pieter12345.javaloader.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.utils.Utils;

public class ProjectManager {
	
	// Variables & Constants.
	private final HashMap<String, JavaProject> projects = new HashMap<String, JavaProject>();;
	private final File projectsDir;
	
	public ProjectManager(File projectsDir) {
		this.projectsDir = projectsDir;
	}
	
	/**
	 * Adds the given project to this project manager. If a project with an equal name already exists, nothing happens.
	 * @param project - The project to add.
	 * @throws IllegalStateException When the given project was initialized with a different project manager.
	 * This means that {@link JavaProject#getProjectManager()} returned a manager that was not equal to this.
	 */
	protected void addProject(JavaProject project) throws IllegalStateException {
		if(project.getProjectManager() != this) {
			throw new IllegalStateException("The given project was initialized with a different project manager.");
		}
		if(!this.projects.containsKey(project.getName())) {
			this.projects.put(project.getName(), project);
		}
	}
	
	/**
	 * Removed the given project from this project manager. If the project did not exist in this project manager,
	 * nothing happens.
	 * @param project - The project to remove.
	 * @throws IllegalStateException When the given project is currently enabled according to
	 * {@link JavaProject#isEnabled()}. This is thrown regardless of whether this project manager contains the project
	 * or not.
	 * @return True if the project was removed, false otherwise.
	 */
	protected boolean removeProject(JavaProject project) throws IllegalStateException {
		if(project.isEnabled()) {
			throw new IllegalStateException("Cannot remove a loaded project.");
		}
		return this.projects.remove(project.getName(), project);
	}
	
	/**
	 * Gets the projects directory.
	 * @return The directory containing all JavaProjects.
	 */
	public File getProjectsDir() {
		return this.projectsDir;
	}
	
	/**
	 * Gets a JavaProject by name.
	 * @param name - The name of the JavaLoader project.
	 * @return The JavaProject or null if no project with the given name exists.
	 */
	public JavaProject getProject(String name) {
		return this.projects.get(name);
	}
	
	/**
	 * getProjectInstance method.
	 * @param name - The name of the JavaLoader project.
	 * @return The JavaLoaderProject instance or null if no project with the given name exists.
	 */
	public JavaLoaderProject getProjectInstance(String name) {
		JavaProject project = this.projects.get(name);
		return (project == null ? null : project.getInstance());
	}
	
	/**
	 * getProjectInstances method.
	 * @return An array containing all loaded JavaLoader project instances.
	 */
	public JavaLoaderProject[] getProjectInstances() {
		ArrayList<JavaLoaderProject> instances = new ArrayList<JavaLoaderProject>(this.projects.size());
		for(JavaProject project : this.projects.values()) {
			JavaLoaderProject instance = project.getInstance();
			if(instance != null) {
				instances.add(instance);
			}
		}
		return instances.toArray(new JavaLoaderProject[instances.size()]);
	}
	
	/**
	 * Gets all JavaProject projects in this ProjectManager.
	 * @return An array containing all loaded JavaLoader projects.
	 */
	public JavaProject[] getProjects() {
		return this.projects.values().toArray(new JavaProject[0]);
	}
	
	/**
	 * Gets the names of all JavaLoader projects in this ProjectManager.
	 * @return An array containing all loaded JavaLoader project names.
	 */
	public String[] getProjectNames() {
		return this.projects.keySet().toArray(new String[0]);
	}
	
	/**
	 * Loads all projects in this project manager.
	 * @param exHandler - An exception handler for load exceptions that occur during loading.
	 * @return The loaded projects.
	 */
	public Set<JavaProject> loadAllProjects(LoadExceptionHandler exHandler) {
		
		// Create a set of unloaded projects.
		Set<JavaProject> projects = new HashSet<JavaProject>();
		for(JavaProject project : this.projects.values()) {
			if(!project.isEnabled()) {
				projects.add(project);
			}
		}
		
		// Generate a graph, representing the projects and how they depend on eachother (dependencies as children).
		GraphGenerationResult result = this.generateDependencyGraph(projects, false);
		Graph<JavaProject> graph = result.graph;
		Set<JavaProject> errorProjects = new HashSet<JavaProject>();
		for(JavaProjectException ex : result.exceptions) {
			if(ex.getProject() != null) {
				errorProjects.add(ex.getProject());
			}
			exHandler.handleLoadException(new LoadException(ex.getProject(), ex.getMessage()));
		}
		
		// Check for cycles (Projects that depend on themselves are included).
		Set<Set<JavaProject>> cycles = getGraphCycles(graph);
		for(Set<JavaProject> cycle : cycles) {
			assert(cycle.size() != 0);
			if(cycle.size() > 1) {
				
				// Add an exception to all projects in the cycle.
				String projectsStr = Utils.glueIterable(cycle, (JavaProject project) -> project.getName(), ", ");
				for(JavaProject project : cycle) {
					exHandler.handleLoadException(new LoadException(project,
							"Circular dependency detected including projects: " + projectsStr + "."));
					errorProjects.add(project);
				}
				
				// Add an exception to all ancestors of the cycle. These won't be iterated over for loading later.
				for(JavaProject project : graph.getAncestors(cycle.iterator().next())) {
					if(!cycle.contains(project)) {
						exHandler.handleLoadException(new LoadException(project,
								"Project depends directly or indirectly on (but is not part of)"
								+ " a circular dependency including projects: " + projectsStr + "."));
						errorProjects.add(project);
					}
				}
				
			} else if(cycle.size() == 1) {
				
				// Add an exception about the project depending on itself.
				JavaProject project = cycle.iterator().next();
				exHandler.handleLoadException(new LoadException(project,
						"Project depends on itself (circular dependency): " + project.getName() + "."));
				errorProjects.add(project);
				
			}
		}
		
		// Iterate over the graph, loading all projects.
		Set<JavaProject> loadedProjects = new HashSet<JavaProject>();
		for(ChildBeforeParentGraphIterator<JavaProject> it = graph.childBeforeParentIterator(); it.hasNext(); ) {
			JavaProject project = it.next();
			
			// Attempt to load the project if it is not an error project.
			boolean isErrorProject = errorProjects.contains(project);
			if(!isErrorProject) {
				try {
					project.load();
					loadedProjects.add(project);
				} catch (LoadException e) {
					exHandler.handleLoadException(e);
					isErrorProject = true;
					// TODO - Remove unnecessary errorProjects add or return error projects somehow.
					errorProjects.add(project);
				}
			}
			
			// Remove the project and all projects that depend on it if the project could not be loaded.
			if(isErrorProject) {
				List<JavaProject> removedProjects = it.removeAncestors();
				assert(removedProjects != null && removedProjects.get(0) == project);
				
				// The project should already have an exception for its failure, add one for its dependents.
				for(int i = 1; i < removedProjects.size(); i++) {
					exHandler.handleLoadException(new LoadException(project, "Indirect or direct"
							+ " dependency project could not be loaded: " + removedProjects.get(i).getName()));
					// TODO - Remove unnecessary errorProjects add or return error projects somehow.
					errorProjects.add(removedProjects.get(i));
				}
			}
		}
		
		// Return the projects that have been loaded.
		return loadedProjects;
	}
	
	/**
	 * Unloads all projects in this project manager.
	 * @param exHandler - An exception handler for unload exceptions that occur during unloading.
	 * @return The unloaded projects.
	 */
	public Set<JavaProject> unloadAllProjects(UnloadExceptionHandler exHandler) {
		
		// Create a set of loaded projects.
		Set<JavaProject> projects = new HashSet<JavaProject>();
		for(JavaProject project : this.projects.values()) {
			if(project.isEnabled()) {
				projects.add(project);
			}
		}
		
		// Generate a graph, representing the projects and how they depend on eachother (dependencies as children).
		GraphGenerationResult result = this.generateDependencyGraph(projects, false);
		Graph<JavaProject> graph = result.graph;
		for(JavaProjectException ex : result.exceptions) {
			exHandler.handleUnloadException(new UnloadException(ex.getProject(), ex.getMessage()));
		}
		
		// Check for cycles (Projects that depend on themselves are included). It should be impossible to load projects
		// with circular dependencies. Since the graph only contains loaded projects, there cannot be cycles.
		assert(getGraphCycles(graph).size() == 0);
		
		// Iterate over the graph, unloading all projects.
		Set<JavaProject> unloadedProjects = new HashSet<JavaProject>();
		for(ParentBeforeChildGraphIterator<JavaProject> it = graph.iterator(); it.hasNext(); ) {
			JavaProject project = it.next();
			
			// Attempt to unload the project. Use IGNORE_DEPENDENTS since we know that the dependents have been handled.
			try {
				project.unload(UnloadMethod.IGNORE_DEPENDENTS, exHandler);
				unloadedProjects.add(project);
			} catch (UnloadException e) {
				// Never happens due to using the IGNORE_DEPENDENTS method.
				assert(false);
				exHandler.handleUnloadException(e);
			}
		}
		
		// Return the projects that have been unloaded.
		return unloadedProjects;
	}
	
	/**
	 * Generates a dependency graph from the given projects, based on their dependencies. The graph will only contain
	 * the given projects, any other projects that are referred to through dependencies are simply ignored. In the
	 * graph, projects that depend on other projects are added to those other projects as children.
	 * @param projects - The projects for which to generate a dependency graph.
	 * @param useSourceDependencies - If this is true, the source dependencies will be used, rather than the
	 * dependencies matching the current compiled project.
	 * @return The graph and a list of exceptions that have occurred while generating it.
	 * @throws IllegalArgumentException If one or more of the given projects have a project manager other than this.
	 */
	private GraphGenerationResult generateDependencyGraph(
			Collection<JavaProject> projects, boolean useSourceDependencies) throws IllegalArgumentException {
		final List<JavaProjectException> exceptions = new ArrayList<JavaProjectException>();
		
		// Create a graph, representing the projects and how they depend on eachother.
		final Graph<JavaProject> graph = new Graph<JavaProject>(projects);
		for(JavaProject project : projects) {
			
			// Validate that the projects are loaded from this project manager.
			if(project.getProjectManager() != this) {
				throw new IllegalArgumentException("Project is managed"
						+ " by a different project manager: " + project.getName());
			}
			
			// Get the dependencies of the project.
			Dependency[] dependencies;
			if(useSourceDependencies) {
				try {
					dependencies = project.getSourceDependencies();
				} catch (IOException | DependencyException e) {
					exceptions.add(new JavaProjectException(project, e.getMessage()));
					continue;
				}
			} else {
				dependencies = project.getDependencies();
				if(dependencies == null) {
					try {
						project.initDependencies();
					} catch (IOException | DependencyException e) {
						exceptions.add(new JavaProjectException(project, e.getMessage()));
						continue;
					}
					dependencies = project.getDependencies();
				}
			}
			
			// Add edges in the graph from 'project' to all known projects that depend on it.
			for(Dependency dep : dependencies) {
				if(dep instanceof ProjectDependency) {
					JavaProject projDepProject = ((ProjectDependency) dep).getProject();
					
					// Validate that the dependency project uses this project manager.
					if(((ProjectDependency) dep).getProjectManager() != this) {
						exceptions.add(new JavaProjectException(project, "Dependency project is managed"
								+ " by a different project manager: " + projDepProject.getName()));
						continue;
					}
					
					// Add an exception for unknown project dependencies.
					if(projDepProject == null) {
						exceptions.add(new JavaProjectException(project, "Dependency project does not exist in the"
								+ " project manager: " + ((ProjectDependency) dep).getProjectName()));
						continue;
					}
					
					// Add an edge from 'project' to its dependency 'projDepProject' if its in the passed collection.
					if(projects.contains(projDepProject)) {
						graph.addDirectedEdge(project, projDepProject);
					}
				}
			}
		}
		
		// Return the generated graph and occurred exceptions.
		return new GraphGenerationResult(graph, exceptions);
	}
	
	/**
	 * Returns a set containing all cycles in the given graph as sets.
	 * @param graph - The graph which to detect cycles for.
	 * @return A set containing one set per cycle, which contains all projects in that cycle.
	 */
	private static Set<Set<JavaProject>> getGraphCycles(Graph<JavaProject> graph) {
		Set<Set<JavaProject>> sccs = graph.getStronglyConnectedComponents();
		for(Iterator<Set<JavaProject>> it = sccs.iterator(); it.hasNext(); ) {
			Set<JavaProject> scc = it.next();
			assert(scc.size() != 0);
			if(scc.size() == 1) {
				JavaProject project = scc.iterator().next();
				if(!graph.hasDirectedEdge(project, project)) {
					it.remove();
				}
			}
		}
		return sccs;
	}
	
	private static class GraphGenerationResult {
		public final Graph<JavaProject> graph;
		public final List<JavaProjectException> exceptions;
		public GraphGenerationResult(Graph<JavaProject> graph, List<JavaProjectException> exceptions) {
			this.graph = graph;
			this.exceptions = exceptions;
		}
	}
	
	/**
	 * Compiles, unloads (if loaded) and loads the given project. Compilation happens in a temporary directory, so
	 * if a CompileException occurs, the temporary directory is simply removed and the project will stay loaded if
	 * it was loaded. If the project directory no longer exists, the project will be unloaded and removed from this
	 * project manager. If the project does not exist in this project manager, but it does in the file system,
	 * it will be added to this project manager.
	 * @param projectName - The name of the project to compile, unload and load.
	 * @param feedbackHandler - The project feedback handler which will receive all thrown exceptions and feedback that
	 * does not cause the overall process to fail during the recompile.
	 * @param projectStateListener - The listener that will be set in the new JavaProject if it had not been added to
	 * this project manager. If the project was already added, this argument is simply ignored.
	 * @throws CompileException - If an exception occurred during compilation.
	 * If this is thrown, the new binaries have not yet been applied and the project is not unloaded.
	 * @throws UnloadException - If an exception occurred while unloading which prevents the project from unloading.
	 * If this is thrown, the new binaries have not yet been applied and the project is not unloaded.
	 * @throws LoadException - If an exception occurred during the loading of the new compiled binaries.
	 * If this is thrown, the new binaries have been applied and the project has been unloaded, but not reloaded due
	 * to the reason given in this exception.
	 */
	public void recompile(String projectName, RecompileFeedbackHandler feedbackHandler,
			ProjectStateListener projectStateListener) throws CompileException, UnloadException, LoadException {
		
		// Get the project from this project manager or from the file system.
		JavaProject project = this.projects.get(projectName);
		if(project == null) {
			
			// Get the project from the file system.
			project = this.addProjectFromProjectDirectory(projectName, projectStateListener);
			if(project != null) {
				feedbackHandler.actionPerformed(RecompileStatus.ADDED_FROM_FILE_SYSTEM);
			} else {
				// TODO - Change this exception type? So far, this is the only null project passed to it.
				throw new CompileException(null, "Project does not exist: " + projectName);
			}
			
		} else {
			
			// Check if the project has been removed.
			if(!project.getProjectDir().exists()) {
				
				// Attempt to unload the project.
				try {
					/* TODO - Print feedback about unloaded dependents? This can be done by making unload() return
					 * a collection of unloaded projects and passing them to a new method in the feedbackHandler.
					 */
					project.unload(UnloadMethod.UNLOAD_DEPENDENTS, feedbackHandler);
					feedbackHandler.actionPerformed(RecompileStatus.UNLOADED);
				} catch (UnloadException e) {
					// This exception should never be thrown due to using the UNLOAD_DEPENDENTS unload method.
					assert(!project.isEnabled());
					assert(false);
					if(project.isEnabled()) {
						// There's nothing we can do at this point. Something is preventing the project from unloading.
						throw e;
					}
					feedbackHandler.handleUnloadException(e);
				}
				
				// Remove the project from the project manager.
				this.projects.remove(projectName);
				feedbackHandler.actionPerformed(RecompileStatus.REMOVED);
				return;
			}
		}
		
		// Prevent a recompile if this and at least one of the dependents of this project are loaded.
		if(project.isEnabled()) {
			List<JavaProject> dependingProjects = new ArrayList<JavaProject>(0);
			for(JavaProject p : this.getProjects()) {
				if(p.isEnabled() && p != project) {
					for(Dependency dep : p.getDependencies()) {
						if(dep instanceof ProjectDependency
								&& ((ProjectDependency) dep).getProject() == project) {
							dependingProjects.add(p);
						}
					}
				}
			}
			if(!dependingProjects.isEmpty()) {
				
				// Throw an exception about the dependents being enabled and therefore being unable to recompile.
				dependingProjects.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
				throw new CompileException(project, "Project cannot be recompiled while there are projects enabled"
						+ " that depend on it. Depending project" + (dependingProjects.size() == 1 ? "" : "s") + ": "
						+ Utils.glueIterable(dependingProjects, (JavaProject p) -> p.getName(), ", ") + ".");
			}
		}
		
		// Compile the project in the "bin_new" directory.
		project.setBinDirName("bin_new");
		try {
			project.compile(feedbackHandler);
			feedbackHandler.actionPerformed(RecompileStatus.COMPILED);
		} catch (CompileException e) {
			
			// Remove the newly created bin directory and set it back to the old one.
			Utils.removeFile(project.getBinDir());
			project.setBinDirName("bin");
			
			// Rethrow, compilation failed.
			throw e;
		}
		
		// Set the project bin directory back to the old one.
		File newBinDir = project.getBinDir();
		project.setBinDirName("bin");
		
		// Unload the project if it was loaded. The IGNORE_DEPENDENTS unload method is used because we already
		// checked that none of the dependents are enabled.
		if(project.isEnabled()) {
			try {
				project.unload(UnloadMethod.IGNORE_DEPENDENTS, feedbackHandler);
				feedbackHandler.actionPerformed(RecompileStatus.UNLOADED);
			} catch (UnloadException e) {
				// This exception should never be thrown due to using the UNLOAD_DEPENDENTS unload method.
				assert(!project.isEnabled());
				assert(false);
				if(project.isEnabled()) {
					// There's nothing we can do at this point. Something is preventing the project from unloading.
					Utils.removeFile(newBinDir);
					throw e;
				}
				feedbackHandler.handleUnloadException(e);
			}
		}
		
		// Replace the current "bin" directory with "bin_new" and remove "bin_new".
		if(project.getBinDir().exists() && !Utils.removeFile(project.getBinDir())) {
			throw new CompileException(project,
					"Failed to rename \"bin_new\" to \"bin\" because the \"bin\""
					+ " directory could not be removed for project \"" + project.getName() + "\"."
					+ " This can be fixed manually or by attempting another recompile. The project has"
					+ " already been disabled and some files of the \"bin\" directory might be removed.");
		}
		if(!newBinDir.renameTo(project.getBinDir())) {
			throw new CompileException(project,
					"Failed to rename \"bin_new\" to \"bin\" for project \"" + project.getName() + "\"."
					+ " This can be fixed manually or by attempting another recompile."
					+ " The project has already been disabled and the \"bin\" directory has been removed.");
		}
		
		// Load the project.
		project.load();
		feedbackHandler.actionPerformed(RecompileStatus.LOADED);
		
		// Send feedback.
		feedbackHandler.actionPerformed(RecompileStatus.SUCCESS);
		
	}
	
	/**
	 * Recompiles, unloads and loads all projects. Exceptions and compiler feedback is passed to the given
	 * feedbackHandler. If compilation fails for a project, that project will be reloaded using its old binaries if
	 * possible. This method will add new projects from the file system and remove any projects that no longer exist
	 * in the file system.
	 * @param feedbackHandler - The project feedback handler which will receive all thrown exceptions and feedback that
	 * occur during the recompile.
	 * @param projectStateListener - The listener that will be set in newly added projects from the file system.
	 * @throws IllegalStateException If one or more projects has its binary directory set to something other than "bin".
	 */
	public RecompileAllResult recompileAllProjects(RecompileFeedbackHandler feedbackHandler,
			ProjectStateListener projectStateListener) throws IllegalStateException {
		
		// Add new projects from the file system.
		Set<JavaProject> addedProjects = this.addProjectsFromProjectDirectory(projectStateListener);
		
		// Validate that all binary directories are set to "bin" as we use this assumption below.
		for(JavaProject project : this.projects.values()) {
			if(!project.getBinDir().getName().equals("bin")) {
				throw new IllegalStateException("All projects are expected to have their binary directory name set to"
						+ " \"bin\". But project \"" + project.getName() + "\" had a binary directory named:"
						+ " \"" + project.getBinDir().getName() + "\".");
			}
		}
		
		// Generate a graph, representing the projects and how they depend on eachother (dependencies as children).
		GraphGenerationResult result = this.generateDependencyGraph(this.projects.values(), true);
		Graph<JavaProject> graph = result.graph;
		Set<JavaProject> errorProjects = new HashSet<JavaProject>();
		for(JavaProjectException ex : result.exceptions) {
			if(ex.getProject() != null) {
				errorProjects.add(ex.getProject());
			}
			feedbackHandler.handleCompileException(new CompileException(ex.getProject(), ex.getMessage()));
		}
		
		// Check for cycles (Projects that depend on themselves are included).
		Set<Set<JavaProject>> cycles = getGraphCycles(graph);
		for(Set<JavaProject> cycle : cycles) {
			assert(cycle.size() != 0);
			if(cycle.size() > 1) {
				
				// Add an exception to all projects in the cycle.
				String projectsStr = Utils.glueIterable(cycle, (JavaProject project) -> project.getName(), ", ");
				for(JavaProject project : cycle) {
					feedbackHandler.handleCompileException(new CompileException(project,
							"Circular dependency detected including projects: " + projectsStr + "."));
					errorProjects.add(project);
				}
				
				// Add an exception to all ancestors of the cycle. These won't be iterated over for loading later.
				for(JavaProject project : graph.getAncestors(cycle.iterator().next())) {
					if(!cycle.contains(project)) {
						feedbackHandler.handleCompileException(new CompileException(project,
								"Project depends directly or indirectly on (but is not part of)"
								+ " a circular dependency including projects: " + projectsStr + "."));
						errorProjects.add(project);
					}
				}
				
			} else if(cycle.size() == 1) {
				
				// Add an exception about the project depending on itself.
				JavaProject project = cycle.iterator().next();
				feedbackHandler.handleCompileException(new CompileException(project,
						"Project depends on itself (circular dependency): " + project.getName() + "."));
				errorProjects.add(project);
				
			}
		}
		
		// Iterate over the graph, compiling all projects.
		Set<JavaProject> compiledProjects = new HashSet<JavaProject>();
		for(ChildBeforeParentGraphIterator<JavaProject> it = graph.childBeforeParentIterator(); it.hasNext(); ) {
			JavaProject project = it.next();
			
			// Attempt to compile the project if it is not an error project.
			boolean isErrorProject = errorProjects.contains(project);
			if(!isErrorProject) {
				project.setBinDirName("bin_new");
				try {
					project.compile(feedbackHandler);
					compiledProjects.add(project);
				} catch (CompileException e) {
					
					// Remove the newly created binary directory and set the project back to the default bin directory.
					Utils.removeFile(project.getBinDir());
					project.setBinDirName("bin");
					
					feedbackHandler.handleCompileException(e);
					isErrorProject = true;
					errorProjects.add(project);
				}
			}
			
			// Remove the project and all projects that depend on it if the project could not be compiled.
			if(isErrorProject) {
				List<JavaProject> removedProjects = it.removeAncestors();
				assert(removedProjects != null && removedProjects.get(0) == project);
				
				// The project should already have an exception for its failure, add one for its dependents.
				for(int i = 1; i < removedProjects.size(); i++) {
					feedbackHandler.handleCompileException(new CompileException(project, "Indirect or direct"
							+ " dependency project was not successfully compiled: " + removedProjects.get(i).getName()));
					errorProjects.add(removedProjects.get(i));
				}
			}
		}
		
		// Unload all projects.
		Set<JavaProject> unloadedProjects = this.unloadAllProjects(
				(UnloadException ex) -> feedbackHandler.handleUnloadException(ex));
		
		// Remove deleted projects.
		Set<JavaProject> removedProjects = this.removeDeletedProjects();
		
		// Replace all binary directories with the new ones for non-error projects.
		for(JavaProject project : this.projects.values()) {
			if(!errorProjects.contains(project)) {
				
				// Validate that a project is either in ErrorProjects or has its binary directory renamed.
				// Fail the hard way if this is not the case, so that we can be sure to never mess up file removal.
				if(!project.getBinDir().getName().equals("bin_new")) {
					throw new InternalError("A non-error project did not have its binary directory renamed."
							+ " This should be impossible.");
				}
				
				// Replace the current binary directory with the new one and remove the new one.
				File newBinDir = project.getBinDir();
				project.setBinDirName("bin");
				if(project.getBinDir().exists() && !Utils.removeFile(project.getBinDir())) {
					feedbackHandler.handleCompileException(new CompileException(project,
							"Failed to replace the old binary directory with the new binary directory because the old"
							+ " binary directory could not be removed for project \"" + project.getName() + "\"."
							+ " This can be fixed manually or by attempting another recompile. The project has already"
							+ " been disabled and some files of the current binary directory might be removed."));
				}
				if(!newBinDir.renameTo(project.getBinDir())) {
					feedbackHandler.handleCompileException(new CompileException(project,
							"Failed to rename the new binary directory to the default binary directory for project \""
							+ project.getName() + "\". This can be fixed manually or by attempting another recompile."
							+ " The project has already been disabled and the current binary directory has been"
							+ " removed."));
				}
			}
		}
		
		// Validate that all binary directories are set back to "bin" here.
		// Note that we can only know this due to the earlier validation check in this method.
		for(JavaProject project : this.projects.values()) {
			if(!project.getBinDir().getName().equals("bin")) {
				throw new InternalError("All projects are known to have their binary directory name set to"
						+ " \"bin\" at this point. Yet, project \"" + project.getName() + "\" has a binary directory"
						+ " named: \"" + project.getBinDir().getName() + "\".");
			}
		}
		
		// Load all projects. Projects that have caused errors might fail, but might also work using their old binaries.
		Set<JavaProject> loadedProjects = this.loadAllProjects(
				(LoadException ex) -> feedbackHandler.handleLoadException(ex));
		
		// Return the result.
		return new RecompileAllResult(addedProjects, removedProjects,
				compiledProjects, unloadedProjects, loadedProjects, errorProjects);
	}
	
	public static class RecompileAllResult {
		public final Set<JavaProject> addedProjects;
		public final Set<JavaProject> removedProjects;
		public final Set<JavaProject> compiledProjects;
		public final Set<JavaProject> unloadedProjects;
		public final Set<JavaProject> loadedProjects;
		public final Set<JavaProject> errorProjects;
		
		public RecompileAllResult(Set<JavaProject> added, Set<JavaProject> removed,
				Set<JavaProject> compiled, Set<JavaProject> unloaded, Set<JavaProject> loaded, Set<JavaProject> error) {
			this.addedProjects = added;
			this.removedProjects = removed;
			this.compiledProjects = compiled;
			this.unloadedProjects = unloaded;
			this.loadedProjects = loaded;
			this.errorProjects = error;
		}
	}
	
	/**
	 * Reads through the projects directory (as defined in the constructor and accessible through
	 * {@link #getProjectsDir()}) and adds a new project from every directory within this projects directory that
	 * does not end with ".disabled". All new projects will be added to this project manager.
	 * @param projectStateLister - A listener to add to all newly created projects. This may be null.
	 * @return The added projects.
	 */
	public Set<JavaProject> addProjectsFromProjectDirectory(ProjectStateListener projectStateListener) {
		Set<JavaProject> newProjects = new HashSet<JavaProject>();
		if(this.projectsDir != null) {
			File[] projectDirs = this.projectsDir.listFiles();
			if(projectDirs != null) {
				for(File projectDir : projectDirs) {
					if(projectDir.isDirectory() && !projectDir.getName().toLowerCase().endsWith(".disabled")
							&& !this.projects.containsKey(projectDir.getName())) {
						JavaProject project = new JavaProject(projectDir.getName(), projectDir, this, projectStateListener);
						this.projects.put(project.getName(), project);
						newProjects.add(project);
					}
				}
			}
		}
		return newProjects;
	}
	
	/**
	 * Adds and returns a JavaProject if the given projectName has a matching directory within the projects directory
	 * and has not yet been added to this project manager.
	 * @param projectName - The name of the project to attempt to find and add.
	 * @param projectStateLister - A listener to add to the newly created project. This may be null.
	 * @return The added project or null if no project matched the projectName
	 * or if a project with an equal name was already loaded.
	 */
	public JavaProject addProjectFromProjectDirectory(String projectName, ProjectStateListener projectStateListener) {
		
		// Get the project directory.
		if(this.projectsDir != null) {
			return null; // No projects directory set -> Project not found.
		}
		File projectDir = new File(this.projectsDir.getAbsolutePath() + "/" + projectName);
		
		// Validate that the projectName did not contain file path modifying characters.
		if(!projectDir.getAbsoluteFile().getParent().equals(this.projectsDir.getAbsolutePath())) {
			return null;
		}
		
		// Create the project if it was found.
		if(projectDir.getName().equals(projectName) && projectDir.isDirectory()
				&& !projectDir.getName().toLowerCase().endsWith(".disabled")
				&& !this.projects.containsKey(projectDir.getName())) {
			JavaProject project = new JavaProject(projectDir.getName(), projectDir, this, projectStateListener);
			this.projects.put(project.getName(), project);
			return project;
		}
		
		// Project not found.
		return null;
	}
	
	/**
	 * Removes unloaded deleted projects from this project manager. Deleted means that the project directory
	 * which would usually contain the "src" and "bin" directory no longer exists.
	 * @return The removed projects.
	 */
	public Set<JavaProject> removeDeletedProjects() {
		Set<JavaProject> removedProjects = new HashSet<JavaProject>();
		for(Iterator<JavaProject> it = this.projects.values().iterator(); it.hasNext(); ) {
			JavaProject project = it.next();
			if(!project.isEnabled() && !project.getProjectDir().exists()) {
				it.remove();
				removedProjects.add(project);
			}
		}
		return removedProjects;
	}
	
	/**
	 * Removes the given project if it's unloaded and its project directory which would usually contain the
	 * "src" and "bin" directory no longer exists.
	 * @param projectName - The name of the project to remove if it's unloaded and its project directory was deleted.
	 * @return The removed project or null if the project was not removed.
	 */
	public JavaProject removeDeletedProject(String projectName) {
		JavaProject project = this.projects.get(projectName);
		if(project != null && !project.isEnabled() && !project.getProjectDir().exists()) {
			this.projects.remove(projectName);
			return project;
		}
		return null;
	}
	
	/**
	 * Unloads all projects and removes them from the projects list.
	 * @param exHandler - An exception handler for unload exceptions that occur during unloading.
	 */
	public void clear(UnloadExceptionHandler exHandler) {
		this.unloadAllProjects((UnloadException ex) -> exHandler.handleUnloadException(ex));
		this.projects.clear();
	}
	
	public static enum RecompileStatus {
		ADDED_FROM_FILE_SYSTEM,
		REMOVED,
		COMPILED,
		UNLOADED,
		LOADED,
		SUCCESS;
	}
	
	public static interface RecompileFeedbackHandler extends ProjectExceptionHandler, CompilerFeedbackHandler {
		void actionPerformed(RecompileStatus status);
	}
	
}
