package io.github.pieter12345.javaloader.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import io.github.pieter12345.graph.Graph;
import io.github.pieter12345.graph.Graph.ChildBeforeParentGraphIterator;
import io.github.pieter12345.graph.Graph.ParentBeforeChildGraphIterator;
import io.github.pieter12345.javaloader.core.JavaProject.CompilerFeedbackHandler;
import io.github.pieter12345.javaloader.core.JavaProject.UnloadMethod;
import io.github.pieter12345.javaloader.core.dependency.Dependency;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependency;
import io.github.pieter12345.javaloader.core.dependency.ProjectDependencyParser;
import io.github.pieter12345.javaloader.core.exceptions.CompileException;
import io.github.pieter12345.javaloader.core.exceptions.DepOrderViolationException;
import io.github.pieter12345.javaloader.core.exceptions.DependencyException;
import io.github.pieter12345.javaloader.core.exceptions.JavaProjectException;
import io.github.pieter12345.javaloader.core.exceptions.LoadException;
import io.github.pieter12345.javaloader.core.exceptions.UnloadException;
import io.github.pieter12345.javaloader.core.exceptions.handlers.LoadExceptionHandler;
import io.github.pieter12345.javaloader.core.exceptions.handlers.ProjectExceptionHandler;
import io.github.pieter12345.javaloader.core.exceptions.handlers.UnloadExceptionHandler;
import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * Manages JavaProject instances and provides 'bulk' operations such as load all, unload all and compile all projects.
 * @author P.J.S. Kools
 */
public class ProjectManager {
	
	// Variables & Constants.
	private final HashMap<String, JavaProject> projects = new HashMap<String, JavaProject>();
	private final File projectsDir;
	private final ProjectDependencyParser dependencyParser;
	
	/**
	 * Creates a new {@link ProjectManager}.
	 * @param projectsDir - The directory containing the projects.
	 * @param dependencyParser - The parser used to parse project dependencies.
	 */
	public ProjectManager(File projectsDir, ProjectDependencyParser dependencyParser) {
		this.projectsDir = projectsDir;
		this.dependencyParser = dependencyParser;
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
		if(project.isLoaded()) {
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
	 * @return The JavaLoaderProject instance or null if no project with the given name is loaded.
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
	 * Checks if a project with the given name is in this ProjectManager.
	 * @param name - The name of the project to look for.
	 * @return True if this project manager contains a project matching the given name, false otherwise.
	 */
	public boolean hasProject(String name) {
		return this.projects.containsKey(name);
	}
	
	/**
	 * Loads all projects in this project manager which are not currently loaded or disabled.
	 * @param exHandler - An exception handler for load exceptions that occur during loading.
	 * @return A LoadAllResult containing a set of loaded projects by this method and a set of error projects.
	 * These sets do not overlap.
	 */
	public LoadAllResult loadAllProjects(LoadExceptionHandler exHandler) {
		
		// Create a set of unloaded enabled projects.
		Set<JavaProject> projects = new HashSet<JavaProject>();
		for(JavaProject project : this.projects.values()) {
			if(!project.isLoaded() && !project.isDisabled()) {
				projects.add(project);
			}
		}
		
		// Generate a graph, representing the projects and how they depend on each other (dependencies as children).
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
					errorProjects.add(removedProjects.get(i));
				}
			}
		}
		
		// Return the projects that have been loaded.
		return new LoadAllResult(loadedProjects, errorProjects);
	}
	
	/**
	 * Represents the result of a load-all operation.
	 * @author P.J.S. Kools
	 */
	public static class LoadAllResult {
		public final Set<JavaProject> loadedProjects;
		public final Set<JavaProject> errorProjects;
		
		public LoadAllResult(Set<JavaProject> loaded, Set<JavaProject> error) {
			this.loadedProjects = loaded;
			this.errorProjects = error;
		}
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
			if(project.isLoaded()) {
				projects.add(project);
			}
		}
		
		// Generate a graph, representing the projects and how they depend on each other (dependencies as children).
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
		
		// Create a graph, representing the projects and how they depend on each other.
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
	
	/**
	 * Represents the result of a graph generation operation.
	 * @author P.J.S. Kools
	 */
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
	 * it was loaded. Direct and indirect loaded dependents of the project will be handled according to the given
	 * {@link DependentsHandling} option.
	 * @param project - The project to compile, unload and load.
	 * @param dependentsHandling - The way to handle loaded dependents of the project.
	 * @param feedbackHandler - The project feedback handler which will receive all thrown exceptions and feedback that
	 * occur during the recompile.
	 * @throws CompileException If an exception occurred during compilation.
	 * If this is thrown, the new binaries have not yet been applied and the project has not been unloaded.
	 * @throws DepOrderViolationException If the given project is loaded and at least one of its dependents is loaded
	 * and {@link DependentsHandling#NONE} is provided.
	 * @throws IllegalArgumentException When {@link project#getProjectManager()} != this or when project is not known
	 * in this project manager.
	 */
	public void recompile(JavaProject project, DependentsHandling dependentsHandling,
			RecompileFeedbackHandler feedbackHandler) throws
			CompileException, DepOrderViolationException, IllegalArgumentException {
		
		// Validate that the project is part of this project manager.
		if(project.getProjectManager() != this) {
			throw new IllegalArgumentException("The given project has a different project manager.");
		} else if(!this.projects.containsValue(project)) {
			throw new IllegalArgumentException("The given project has not been added to this project manager.");
		}
		
		// Get dependents unload, load and recompile order if at least one of the dependents of this project are loaded.
		Iterator<JavaProject> unloadIterator;
		Iterator<JavaProject> recompileIterator;
		Iterator<JavaProject> loadIterator;
		if(project.isLoaded()) {
			Set<JavaProject> loadedDirectDependents = this.getLoadedDependents(project);
			if(loadedDirectDependents.isEmpty()) {
				
				// The project does not depend on other projects, so handle only this project.
				unloadIterator = Arrays.asList(project).iterator();
				recompileIterator = Arrays.asList(project).iterator();
				loadIterator = Arrays.asList(project).iterator();
			} else {
				if(dependentsHandling == DependentsHandling.NONE) {
					
					// Throw an exception about the dependents being enabled and therefore being unable to recompile.
					List<JavaProject> loadedDependentsList = new ArrayList<JavaProject>(loadedDirectDependents.size());
					loadedDependentsList.addAll(loadedDirectDependents);
					loadedDependentsList.sort((JavaProject p1, JavaProject p2) -> p1.getName().compareTo(p2.getName()));
					throw new DepOrderViolationException(project,
							"Project cannot be recompiled while there are projects enabled that depend on it."
							+ " Depending project" + (loadedDependentsList.size() == 1 ? "" : "s") + ": "
							+ Utils.glueIterable(loadedDependentsList, (JavaProject p) -> p.getName(), ", ") + ".");
				} else {
					
					// Create a set of loaded projects.
					Set<JavaProject> loadedProjects = new HashSet<>();
					for(JavaProject proj : this.projects.values()) {
						if(proj.isLoaded()) {
							loadedProjects.add(proj);
						}
					}
					
					// Create a dependency graph of loaded projects.
					Graph<JavaProject> loadedProjectsGraph;
					{
						GraphGenerationResult result = this.generateDependencyGraph(loadedProjects, false);
						loadedProjectsGraph = result.graph;
						for(JavaProjectException ex : result.exceptions) {
							feedbackHandler.handleCompileException(
									new CompileException(ex.getProject(), ex.getMessage()));
						}
						if(!result.exceptions.isEmpty()) {
							throw new CompileException(project, "Project cannot be"
									+ " recompiled because of problems with runtime dependencies of loaded projects.");
						}
					}
					
					// Create a set of loaded (in)direct dependents.
					Set<JavaProject> loadedDependents = new HashSet<>();
					{
						Stack<JavaProject> projStack = new Stack<>();
						projStack.add(project);
						while(!projStack.isEmpty()) {
							JavaProject proj = projStack.pop();
							for(JavaProject dependent : loadedProjectsGraph.getDescendents(proj)) {
								if(loadedDependents.add(dependent)) {
									projStack.push(dependent);
								}
							}
						}
					}
					
					// Create a dependency graph of loaded (in)direct dependents.
					Graph<JavaProject> loadedDependentsGraph;
					{
						GraphGenerationResult result = this.generateDependencyGraph(loadedDependents, false);
						loadedDependentsGraph = result.graph;
						for(JavaProjectException ex : result.exceptions) {
							feedbackHandler.handleCompileException(
									new CompileException(ex.getProject(), ex.getMessage()));
						}
						if(!result.exceptions.isEmpty()) {
							throw new CompileException(project, "Project cannot be"
									+ " recompiled because of problems with runtime dependencies of loaded projects.");
						}
					}
					
					// Create an iterator for unloading projects.
					unloadIterator = loadedDependentsGraph.childBeforeParentIterator();
					
					// Create an iterator for recompiling projects, if they should be recompiled.
					if(dependentsHandling == DependentsHandling.RECOMPILE) {
						GraphGenerationResult result = this.generateDependencyGraph(loadedDependents, true);
						Graph<JavaProject> graph = result.graph;
						for(JavaProjectException ex : result.exceptions) {
							feedbackHandler.handleCompileException(
									new CompileException(ex.getProject(), ex.getMessage()));
						}
						if(!result.exceptions.isEmpty()) {
							throw new CompileException(project, "Project cannot be"
									+ " recompiled because of problems with source dependencies of loaded projects.");
						}
						recompileIterator = graph.parentBeforeChildIterator();
						loadIterator = graph.parentBeforeChildIterator();
						
						// Check for cycles (Projects that depend on themselves are included).
						Set<Set<JavaProject>> cycles = getGraphCycles(graph);
						for(Set<JavaProject> cycle : cycles) {
							assert(cycle.size() != 0);
							if(cycle.size() > 1) {
								
								// Add an exception to all projects in the cycle.
								String projectsStr = Utils.glueIterable(cycle,
										(JavaProject proj) -> proj.getName(), ", ");
								for(JavaProject proj : cycle) {
									feedbackHandler.handleCompileException(new CompileException(proj,
											"Circular dependency detected including projects: " + projectsStr + "."));
								}
								
								// Add an exception to all ancestors of the cycle.
								for(JavaProject proj : graph.getAncestors(cycle.iterator().next())) {
									if(!cycle.contains(proj)) {
										feedbackHandler.handleCompileException(new CompileException(proj,
												"Project depends directly or indirectly on (but is not part of)"
												+ " a circular dependency including projects: " + projectsStr + "."));
									}
								}
								
							} else if(cycle.size() == 1) {
								
								// Add an exception about the project depending on itself.
								JavaProject proj = cycle.iterator().next();
								feedbackHandler.handleCompileException(new CompileException(proj,
										"Project depends on itself (circular dependency): " + proj.getName() + "."));
								
							}
						}
						if(!cycles.isEmpty()) {
							throw new CompileException(project, "Project cannot be"
									+ " recompiled because of problems with source dependencies of loaded projects.");
						}
					} else {
						recompileIterator = Arrays.asList(project).iterator();
						loadIterator = loadedDependentsGraph.parentBeforeChildIterator();
					}
				}
			}
		} else {
			
			// The project is not loaded, so handle only this project.
			unloadIterator = Collections.emptyIterator();
			recompileIterator = Arrays.asList(project).iterator();
			loadIterator = Arrays.asList(project).iterator();
		}
		
		// Recompile the project and its (in)direct dependents.
		List<JavaProject> recompiledProjects = new ArrayList<>();
		boolean success = true;
		for(Iterator<JavaProject> it = recompileIterator; it.hasNext(); ) {
			JavaProject p = it.next();
			if(!p.getBinDir().getName().equals("bin")) {
				throw new IllegalStateException("All projects are expected to have their binary directory name set"
						+ " to \"bin\". But project \"" + p.getName() + "\" had a binary directory named:"
						+ " \"" + p.getBinDir().getName() + "\".");
			}
			p.setBinDirName("bin_new");
			try {
				p.compile(feedbackHandler);
				recompiledProjects.add(p);
			} catch (CompileException e) {
				success = false;
				
				// Remove the newly created binary directory and set the project back to the default bin directory.
				Utils.removeFile(p.getBinDir());
				p.setBinDirName("bin");
				
				// Pass the exception to the handler.
				feedbackHandler.handleCompileException(e);
				
				// Remove this project and all projects that depend on it from the recompile process.
				if(it instanceof ChildBeforeParentGraphIterator) {
					List<JavaProject> removedProjects =
							((ChildBeforeParentGraphIterator<JavaProject>) it).removeAncestors();
					assert(removedProjects != null && removedProjects.get(0) == p);
					
					// Add an exception for all skipped dependents.
					for(int i = 1; i < removedProjects.size(); i++) {
						feedbackHandler.handleCompileException(new CompileException(removedProjects.get(i),
								"Indirect or direct dependency project was not successfully compiled: "
								+ p.getName()));
					}
				}
			}
		}
		if(!success) {
			
			// Remove all newly created binary directories and set the projects back to their default bin directory.
			for(JavaProject p : recompiledProjects) {
				Utils.removeFile(p.getBinDir());
				p.setBinDirName("bin");
			}
			
			// Throw an exception since compilation has failed.
			throw new CompileException(project, "Compilation has failed.");
		}
		
		// Unload the project and its (in)direct dependents.
		for(Iterator<JavaProject> it = unloadIterator; it.hasNext(); ) {
			JavaProject p = it.next();
			
			// Attempt to unload the project. Use IGNORE_DEPENDENTS since we know that the dependents have been handled.
			try {
				p.unload(UnloadMethod.IGNORE_DEPENDENTS, feedbackHandler);
			} catch (UnloadException e) {
				// Never happens due to using the IGNORE_DEPENDENTS method.
				throw new Error(e);
			}
		}
		
		// Replace all binary directories with the new ones.
		for(JavaProject p : recompiledProjects) {
			
			// Validate that a project has its binary directory renamed.
			// Fail the hard way if this is not the case, so that we can be sure to never mess up file removal.
			if(!p.getBinDir().getName().equals("bin_new")) {
				throw new Error("A project did not have its binary directory renamed. This should be impossible.");
			}
			
			// Replace the current binary directory with the new one and remove the new one.
			File newBinDir = p.getBinDir();
			p.setBinDirName("bin");
			if(p.getBinDir().exists() && !Utils.removeFile(p.getBinDir())) {
				feedbackHandler.handleCompileException(new CompileException(p,
						"Failed to replace the old binary directory with the new binary directory because the old"
						+ " binary directory could not be removed for project \"" + p.getName() + "\"."
						+ " This can be fixed manually or by attempting another recompile. The project has already"
						+ " been disabled and some files of the current binary directory might be removed."));
			}
			if(!newBinDir.renameTo(p.getBinDir())) {
				feedbackHandler.handleCompileException(new CompileException(p,
						"Failed to rename the new binary directory to the default binary directory for project \""
						+ p.getName() + "\". This can be fixed manually or by attempting another recompile."
						+ " The project has already been disabled and the current binary directory has been"
						+ " removed."));
			}
		}
		
		// Load the project and its (in)direct dependents.
		for(Iterator<JavaProject> it = loadIterator; it.hasNext(); ) {
			JavaProject p = it.next();
			
			// Attempt to load the project.
			try {
				p.load();
			} catch (LoadException e) {
				feedbackHandler.handleLoadException(e);
			}
		}
	}
	
	private Set<JavaProject> getLoadedDependents(JavaProject project) {
		Set<JavaProject> dependingProjects = new HashSet<JavaProject>();
		for(JavaProject p : this.getProjects()) {
			if(p.isLoaded() && p != project) {
				for(Dependency dep : p.getDependencies()) {
					if(dep instanceof ProjectDependency
							&& ((ProjectDependency) dep).getProjectName().equals(project.getName())) {
						dependingProjects.add(p);
					}
				}
			}
		}
		return dependingProjects;
	}
	
	/**
	 * Recompiles, unloads and loads all projects that are not disabled. Exceptions and compiler feedback is passed to
	 * the given feedbackHandler. If compilation fails for a project, that project will be reloaded using its old
	 * binaries if possible. This method will add new projects from the file system and remove any projects that no
	 * longer exist in the file system.
	 * @param feedbackHandler - The project feedback handler which will receive all thrown exceptions and feedback that
	 * occur during the recompile.
	 * @param projectStateListener - The listener that will be set in newly added projects from the file system.
	 * @return A RecompileAllResult containing a set of all added, removed, compiled, unloaded, loaded and error
	 * projects. If a project is in the 'loaded' set, it was recompiled successfully and will not be in the 'error' set.
	 * If the project is in the 'error' set, it did not recompile successfully and will not be in the 'loaded' set.
	 * The 'error', 'loaded' and 'removed' set combined form a set of all handled projects.
	 * @throws IllegalStateException If one or more projects has its binary directory set to something other than "bin".
	 */
	public RecompileAllResult recompileAllProjects(RecompileFeedbackHandler feedbackHandler,
			ProjectStateListener projectStateListener) throws IllegalStateException {
		
		// Create a set of enabled projects.
		Set<JavaProject> projects = new HashSet<JavaProject>();
		for(JavaProject project : this.projects.values()) {
			if(!project.isDisabled()) {
				projects.add(project);
			}
		}
		
		// Add new projects from the file system.
		Set<JavaProject> addedProjects = this.addProjectsFromProjectDirectory(projectStateListener);
		
		// Validate that all binary directories are set to "bin" as we use this assumption below.
		for(JavaProject project : projects) {
			if(!project.getBinDir().getName().equals("bin")) {
				throw new IllegalStateException("All projects are expected to have their binary directory name set to"
						+ " \"bin\". But project \"" + project.getName() + "\" had a binary directory named:"
						+ " \"" + project.getBinDir().getName() + "\".");
			}
		}
		
		// Generate a graph, representing the projects and how they depend on each other (dependencies as children).
		GraphGenerationResult result = this.generateDependencyGraph(projects, true);
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
					feedbackHandler.handleCompileException(new CompileException(project,
							"Indirect or direct dependency project was not successfully compiled: "
							+ removedProjects.get(i).getName()));
					errorProjects.add(removedProjects.get(i));
				}
			}
		}
		
		// Unload all projects.
		Set<JavaProject> unloadedProjects = this.unloadAllProjects(feedbackHandler);
		
		// Remove deleted projects.
		Set<JavaProject> removedProjects = this.removeUnloadedProjectsIfDeleted();
		
		// Replace all binary directories with the new ones for non-error projects.
		for(JavaProject project : projects) {
			if(!errorProjects.contains(project)) {
				
				// Validate that a project is either in ErrorProjects or has its binary directory renamed.
				// Fail the hard way if this is not the case, so that we can be sure to never mess up file removal.
				if(!project.getBinDir().getName().equals("bin_new")) {
					throw new Error("A non-error project did not have its binary directory renamed."
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
		for(JavaProject project : projects) {
			if(!project.getBinDir().getName().equals("bin")) {
				throw new Error("All projects are known to have their binary directory name set to"
						+ " \"bin\" at this point. Yet, project \"" + project.getName() + "\" has a binary directory"
						+ " named: \"" + project.getBinDir().getName() + "\".");
			}
		}
		
		// Load all projects. Projects that have caused errors might fail, but might also work using their old binaries.
		LoadAllResult loadAllResult = this.loadAllProjects(feedbackHandler);
		Set<JavaProject> loadedProjects = loadAllResult.loadedProjects;
		errorProjects.addAll(loadAllResult.errorProjects);
		
		// Return the result.
		return new RecompileAllResult(addedProjects, removedProjects,
				compiledProjects, unloadedProjects, loadedProjects, errorProjects);
	}
	
	/**
	 * Represents the result of a recompile-all operation.
	 * @author P.J.S. Kools
	 */
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
						JavaProject project = new JavaProject(
								projectDir.getName(), projectDir, this, this.dependencyParser, projectStateListener);
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
	 * or if a project with an equal name was already added to the project manager.
	 */
	public JavaProject addProjectFromProjectDirectory(String projectName, ProjectStateListener projectStateListener) {
		
		// Return null if the project was already added or if no projects directory is set.
		if(this.projects.containsKey(projectName) || this.projectsDir == null) {
			return null;
		}
		
		// Get the project directory.
		File projectDir = new File(this.projectsDir.getAbsoluteFile(), projectName);
		
		// Validate that the projectName did not contain file path modifying characters.
		if(!projectDir.getAbsoluteFile().getParent().equals(this.projectsDir.getAbsolutePath())
				|| !projectDir.getName().equals(projectName)) {
			return null;
		}
		
		// Create the project if it was found.
		if(projectDir.getName().equals(projectName) && projectDir.isDirectory()
				&& !projectDir.getName().toLowerCase().endsWith(".disabled")) {
			JavaProject project = new JavaProject(
					projectDir.getName(), projectDir, this, this.dependencyParser, projectStateListener);
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
	public Set<JavaProject> removeUnloadedProjectsIfDeleted() {
		Set<JavaProject> removedProjects = new HashSet<JavaProject>();
		for(Iterator<JavaProject> it = this.projects.values().iterator(); it.hasNext(); ) {
			JavaProject project = it.next();
			if(!project.isLoaded() && !project.getProjectDir().exists()) {
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
	public JavaProject removeUnloadedProjectIfDeleted(String projectName) {
		JavaProject project = this.projects.get(projectName);
		if(project != null && !project.isLoaded() && !project.getProjectDir().exists()) {
			this.projects.remove(projectName);
			return project;
		}
		return null;
	}
	
	/**
	 * Unloads and removes the given project if its project directory which would usually contain the "src" and "bin"
	 * directory no longer exists. The unload happens recursively, meaning that any projects that depend on this
	 * project will also be unloaded (but not removed).
	 * @param projectName - The name of the project to remove if its project directory was deleted.
	 * @param exHandler - An exception handler for UnloadExceptions that might occur while unloading the project and
	 * its dependencies recursively.
	 * @return A list of unloaded projects in an order where depending projects always have a lower index than their
	 * dependents. Returns an empty list when the project was removed, but not loaded. Returns null when the project
	 * was not removed.
	 */
	public List<JavaProject> unloadAndRemoveProjectIfDeleted(String projectName, UnloadExceptionHandler exHandler) {
		
		// Get the project.
		JavaProject project = this.projects.get(projectName);
		
		// Check if the project exists in the project manager and has been removed.
		if(project != null && !project.getProjectDir().exists()) {
			
			// Attempt to unload the project if it is loaded.
			List<JavaProject> unloadedProjects;
			if(project.isLoaded()) {
				try {
					unloadedProjects = project.unload(UnloadMethod.UNLOAD_DEPENDENTS, exHandler);
				} catch (UnloadException e) {
					// This exception should never be thrown due to using the UNLOAD_DEPENDENTS unload method.
					throw new Error(e);
				}
			} else {
				unloadedProjects = Collections.emptyList();
			}
			
			// Remove the project from the project manager.
			this.projects.remove(projectName);
			
			// Return the unloaded projects.
			return unloadedProjects;
		}
		
		// The project either does not exist or exists and has a project directory.
		return null;
	}
	
	/**
	 * Unloads all projects and removes them from the projects list.
	 * @param exHandler - An exception handler for unload exceptions that occur during unloading.
	 */
	public void clear(UnloadExceptionHandler exHandler) {
		this.unloadAllProjects(exHandler);
		this.projects.clear();
	}
	
	/**
	 * Represents a {@link ProjectExceptionHandler} combined with a {@link CompilerFeedbackHandler}.
	 * @author P.J.S. Kools
	 */
	public static interface RecompileFeedbackHandler extends ProjectExceptionHandler, CompilerFeedbackHandler {
	}
}
