package io.github.pieter12345.javaloader.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.pieter12345.javaloader.core.utils.Utils;

/**
 * JavaProjectClassLoader class.
 * This ClassLoader implementation allows one to load classes from a projects bin directory, then the ClassLoader used
 *  to load this class and last an optional list of bin directories and jar files.
 * @author P.J.S. Kools
 */
public class JavaProjectClassLoader extends URLClassLoader {
	
	// Variables & Constants.
	private HashMap<String, Class<?>> classMap = new HashMap<String, Class<?>>();
	private List<ClassLoader> dependencyClassLoaders;
	private final File binDir;
	private final ProtectionDomain protectionDomain;
	
	/**
	 * Constructor.
	 * Creates a new JavaProjectClassLoader with the given bin directory.
	 * @param platformClassLoader - The extra platform specific {@link ClassLoader} to use for resolving platform
	 * specific class references, or {@code null} to use none.
	 * @param binDir - The directory containing the package directories and .class files.
	 */
	public JavaProjectClassLoader(ClassLoader platformClassLoader, File binDir) {
		super(new java.net.URL[] {Utils.fileToURL(binDir)}, platformClassLoader);
		this.binDir = binDir;
		
		// Initialize ProtectionDomain.
		java.security.CodeSource codeSource =
				new java.security.CodeSource(null, (java.security.cert.Certificate[]) null);
		java.security.Permissions permissions = new java.security.Permissions();
		permissions.add(new java.security.AllPermission());
		this.protectionDomain = new java.security.ProtectionDomain(codeSource, permissions);
	}
	
	/**
	 * Constructor.
	 * Creates a new JavaProjectClassLoader with the given bin directory and dependency files.
	 * @param platformClassLoader - The extra platform specific {@link ClassLoader} to use for resolving platform
	 * specific class references, or {@code null} to use none.
	 * @param binDir - The directory containing the package directories and .class files.
	 * @param dependencies - A list of bin directories, .class files or .jar files.
	 * @throws FileNotFoundException If a dependency file does not exist.
	 */
	public JavaProjectClassLoader(
			ClassLoader platformClassLoader, File binDir, List<File> dependencies) throws FileNotFoundException {
		this(platformClassLoader, binDir, dependencies, null);
	}
	
	/**
	 * Constructor.
	 * Creates a new JavaProjectClassLoader with the given bin directory and dependency files.
	 * @param platformClassLoader - The extra platform specific {@link ClassLoader} to use for resolving platform
	 * specific class references, or {@code null} to use none.
	 * @param binDir - The directory containing the package directories and .class files.
	 * @param dependencies - A list of bin directories, .class files or .jar files.
	 * @param dependencyClassLoaders - A list of classloaders from dependencies.
	 * @throws FileNotFoundException If a dependency file does not exist.
	 */
	public JavaProjectClassLoader(ClassLoader platformClassLoader, File binDir, List<File> dependencies,
			List<ClassLoader> dependencyClassLoaders) throws FileNotFoundException {
		super(new java.net.URL[] {Utils.fileToURL(binDir)}, platformClassLoader);
		this.binDir = binDir;
		this.dependencyClassLoaders = (dependencyClassLoaders == null
				? null : new ArrayList<ClassLoader>(dependencyClassLoaders));
		
		// Add dependencies.
		if(dependencies != null) {
			for(File dependency : dependencies) {
				if(!dependency.exists()) {
					throw new FileNotFoundException("Dependency file not found: " + dependency.getAbsolutePath());
				}
				this.addURL(Utils.fileToURL(dependency));
			}
		}
		
		// Initialize ProtectionDomain.
		java.security.CodeSource codeSource =
				new java.security.CodeSource(null, (java.security.cert.Certificate[]) null);
		java.security.Permissions permissions = new java.security.Permissions();
		permissions.add(new java.security.AllPermission());
		this.protectionDomain = new java.security.ProtectionDomain(codeSource, permissions);
	}
	
	/**
	 * loadClass method.
	 * Loads the class with given name. If a class exists in multiple places, it is loaded in this order:
	 *  <br>1. The bin directory of the project.
	 *  <br>2. The passed projects dependencies (only INCLUDE dependencies).
	 *  <br>3. The ClassLoaders of project dependencies (when depending on other JavaLoader projects).
	 *  <br>4. The parent ClassLoader.
	 *  <br>5. The ClassLoader used to load this ClassLoader.
	 * @param name - The binary name of the class (Example: "my.package.MyClass").
	 * @return The resulting Class object.
	 * @throws ClassNotFoundException If the class was not found.
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		
		// Throw an Exception when the ClassLoader was already closed.
		if(this.classMap == null) {
			throw new ClassNotFoundException("This classloader has been closed.");
		}
		
		// Return classes from the classMap if they have already been loaded.
		if(this.classMap.containsKey(name)) {
			return this.classMap.get(name);
		}
		
		// Check if the classfile exists in the projects bin directory.
		File classFile = new File(this.binDir, name.replace(".", "/") + ".class");
		if(classFile.isFile()) {
			try {
				FileInputStream fis = new FileInputStream(classFile);
				ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
				int i;
				byte[] buffer = new byte[(int) classFile.length()];
				while((i = fis.read(buffer)) != -1) {
					byteArrayOutStream.write(buffer, 0, i);
				}
				fis.close();
				byte[] bytes = byteArrayOutStream.toByteArray();
				Class<?> definedClass = this.defineClass(name, bytes, 0, bytes.length, this.protectionDomain);
				this.classMap.put(name, definedClass);
				return definedClass;
			} catch (IOException e) {
				throw new ClassNotFoundException(
						"An IOException occured while reading existing class file: " + classFile.getAbsolutePath());
			}
		}
		
		// Attempt to load the class using the URLClassLoader URLs, bypassing a lookup in the parent classloader.
		// This loads classes from dependency directories, .class files and .jar files.
		try {
			Class<?> definedClass = super.findClass(name);
			this.classMap.put(name, definedClass);
			return definedClass;
		} catch (ClassNotFoundException e) {
			// Ignore.
		}
		
		// Attempt to load the class using classloaders from dependencies.
		if(this.dependencyClassLoaders != null) {
			for(ClassLoader classLoader : this.dependencyClassLoaders) {
				try {
					Class<?> definedClass = classLoader.loadClass(name);
					this.classMap.put(name, definedClass);
					return definedClass;
				} catch (ClassNotFoundException e) {
					// Ignore.
				}
			}
		}
		
		// Attempt to load the class using the parent classloader.
		try {
			Class<?> definedClass = super.loadClass(name);
			this.classMap.put(name, definedClass);
			return definedClass;
		} catch (ClassNotFoundException e) {
			// Ignore.
		}
		
		// Attempt to load the class using the ClassLoader that loaded this ClassLoader.
		// This is necessary to resolve JavaLoader classes for platforms on which JavaLoader is loaded using a child
		// classloader of the platform specific parent classloader, or if that parent classloader has not been set.
		try {
			Class<?> definedClass = JavaProjectClassLoader.class.getClassLoader().loadClass(name);
			this.classMap.put(name, definedClass);
			return definedClass;
		} catch (ClassNotFoundException e) {
			// Ignore.
		}
		
		// Throw a ClassNotFoundException since the class was not found.
		throw new ClassNotFoundException("Class not found: " + name);
	}
	
	/**
	 * addCustomClass method.
	 * Puts the given class in this classloaders cache if no class with the same name and package already exists.
	 * @param clazz - The class.
	 * @return True if the class was put in the cache, false if a class with the same name and package was already
	 *  cached.
	 *  @throws RuntimeException If this method is called after the close() method is called.
	 */
	public boolean addCustomClass(Class<?> clazz) {
		if(this.classMap == null) {
			throw new RuntimeException("This classloader has been closed.");
		}
		if(!this.classMap.containsKey(clazz.getName())) {
			this.classMap.put(clazz.getName(), clazz);
			return true;
		}
		return false;
	}
	
	@Override
	public void close() throws IOException {
		if(this.classMap != null) {
			this.classMap.clear();
			this.classMap = null;
			this.dependencyClassLoaders = null;
		}
		super.close();
	}
}
