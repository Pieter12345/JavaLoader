package io.github.pieter12345.javaloader.core.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Utils class.
 * This class contains useful reflection methods.
 * @author P.J.S. Kools
 */
public abstract class ReflectionUtils {
	
	/**
	 * Creates a new instance from the given class, using the supplied arguments.
	 * @param clazz - The class to instantiate.
	 * @param args - The constructor arguments.
	 * @return The new instance.
	 * @throws NoSuchMethodException If the constructor for the given arguments does not exist.
	 * @throws SecurityException
	 * @throws InstantiationException If the class that declares the constructor for the given arguments is
	 * an abstract class.
	 * @throws IllegalAccessException If the underlying constructor is inaccessible.
	 * @throws InvocationTargetException If an exception occurs while invoking the constructor.
	 */
	public static <T> T newInstance(Class<T> clazz, Argument<?>... args) throws NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?>[] classes = new Class<?>[args.length];
		Object[] objects = new Object[args.length];
		for(int i = 0; i < args.length; i++) {
			classes[i] = args[i].clazz;
			objects[i] = args[i].value;
		}
		Constructor<T> constructor = clazz.getDeclaredConstructor(classes);
		constructor.setAccessible(true);
		return constructor.newInstance(objects);
	}
	
	/**
	 * Gets the desired field value for the given class.
	 * @param clazz - The class containing the field.
	 * @param fieldName - The field name.
	 * @param instance - The instance to get the field of. This is ignored when the field is static (can be null).
	 * @return The value stored in the field.
	 * @throws SecurityException
	 * @throws IllegalAccessException If the underlying field is inaccessible.
	 * @throws NoSuchFieldException If a field with the specified name is not found.
	 */
	public static <T> Object getField(Class<T> clazz, String fieldName, T instance)
			throws SecurityException, IllegalAccessException, NoSuchFieldException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}
	
	/**
	 * Represents an argument, used for reflection utility methods.
	 * @author P.J.S. Kools
	 * @param <T> - The type of the argument.
	 */
	public static class Argument<T> {
		protected final Class<T> clazz;
		protected final Object value;
		
		public Argument(Class<T> clazz, T value) {
			this.clazz = clazz;
			this.value = value;
		}
	}
	
}
