package io.github.pieter12345.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.HashMap;

/**
 * JavaProjectClassLoader class.
 * This ClassLoader implementation allows one to load classes from a projects bin directory, then the ClassLoader used
 *  to load this class and last an optional list of bin directories and jar files.
 * @author P.J.S. Kools
 */
public class JavaProjectClassLoader extends URLClassLoader {
	
	// Variables & Constants.
	private HashMap<String, Class<?>> classMap = new HashMap<String, Class<?>>();
	private final File binDir;
	private final ProtectionDomain protectionDomain;
	
	/**
	 * Constructor.
	 * Creates a new JavaProjectClassLoader with the given bin directory.
	 * @param binDir - The directory containing the package directories and .class files.
	 */
	public JavaProjectClassLoader(File binDir) {
		super(null);
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
	 * @param binDir - The directory containing the package directories and .class files.
	 * @param dependencies - An array of bin directories, .class files or .jar files.
	 * @throws FileNotFoundException If a dependency file does not exist.
	 */
	public JavaProjectClassLoader(File binDir, File[] dependencies) throws FileNotFoundException {
		super(new java.net.URL[] {
				new Object() {
					java.net.URL url() {
						try {
							return binDir.toURI().toURL();
						} catch(Exception e) {
							return null;
						}
					}
				}.url()
			});
		this.binDir = binDir;
		
		// Add dependencies.
		if(dependencies != null) {
			for(File dependency : dependencies) {
				if(!dependency.exists()) {
					throw new FileNotFoundException("Dependency file not found: " + dependency.getAbsolutePath());
				}
				try {
					this.addURL(dependency.toURI().toURL());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
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
	 *  <br>2. The ClassLoader used to load this ClassLoader.
	 *  <br>3. The projects dependencies.
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
		File classFile = new File(this.binDir + "/" + name.replace(".", "/") + ".class");
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
			} catch(IOException e) {
				throw new ClassNotFoundException(
						"An IOException occured while reading existing class file: " + classFile.getAbsolutePath());
			}
		}
		
		// Check if the class can be loaded using the ClassLoader that loaded this class.
		// Do this before loading from dependencies to prevent loading multiple versions of the same class.
		try {
			Class<?> definedClass = JavaProject.class.getClassLoader().loadClass(name);
			this.classMap.put(name, definedClass);
			return definedClass;
		} catch(ClassNotFoundException e) {
			// Ignore.
		}
		
		// Attempt to load the class using the default loadClass from the parent (URLClassLoader).
		// This loads classes from dependency directories, .class files and .jar files.
		try {
			Class<?> definedClass = super.loadClass(name);
			this.classMap.put(name, definedClass);
			return definedClass;
		} catch(ClassNotFoundException e) {
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
		super.close();
		if(this.classMap != null) {
			this.classMap.clear();
			this.classMap = null;
		}
	}
}
