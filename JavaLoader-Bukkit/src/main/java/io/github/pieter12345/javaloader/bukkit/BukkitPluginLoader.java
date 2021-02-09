package io.github.pieter12345.javaloader.bukkit;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.Warning;
import org.bukkit.Warning.WarningState;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;

/**
 * BukkitPluginLoader class.
 * Used as PluginLoader implementation to create custom Plugin implementations for JavaLoader projects.
 * @author P.J.S. Kools
 */
public class BukkitPluginLoader implements PluginLoader {
	
	// Variables & Constants.
	private final Server server;
	
	public BukkitPluginLoader(Server server) {
		this.server = server;
	}
	
	@Override
	public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
		return null;
	}
	
	@Override
	public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
		return null;
	}
	
	@Override
	public Pattern[] getPluginFileFilters() {
		return null;
	}
	
	// Method code copied from Bukkit's JavaPluginLoader. Returns a map with events and their corresponding listeners,
	// generated from the given Listener.
	@Override
	public Map<Class<? extends Event>, Set<RegisteredListener>>
			createRegisteredListeners(Listener listener, final Plugin plugin) {
		Validate.notNull(plugin, "Plugin can not be null");
		Validate.notNull(listener, "Listener can not be null");
		
		boolean useTimings = server.getPluginManager().useTimings();
		Map<Class<? extends Event>, Set<RegisteredListener>> ret =
				new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
		Set<Method> methods;
		try {
			Method[] publicMethods = listener.getClass().getMethods();
			Method[] privateMethods = listener.getClass().getDeclaredMethods();
			methods = new HashSet<Method>(publicMethods.length + privateMethods.length, 1.0f);
			for(Method method : publicMethods) {
				methods.add(method);
			}
			for(Method method : privateMethods) {
				methods.add(method);
			}
		} catch (NoClassDefFoundError e) {
			plugin.getLogger().severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register"
					+ " events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
			return ret;
		}
		
		for(final Method method : methods) {
			final EventHandler eh = method.getAnnotation(EventHandler.class);
			if(eh == null) {
				continue;
			}
			// Do not register bridge or synthetic methods to avoid event duplication.
			if(method.isBridge() || method.isSynthetic()) {
				continue;
			}
			final Class<?> checkClass;
			if(method.getParameterTypes().length != 1
					|| !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
				plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid"
						+ " EventHandler method signature \""
						+ method.toGenericString() + "\" in " + listener.getClass());
				continue;
			}
			final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
			method.setAccessible(true);
			Set<RegisteredListener> eventSet = ret.get(eventClass);
			if(eventSet == null) {
				eventSet = new HashSet<RegisteredListener>();
				ret.put(eventClass, eventSet);
			}
			
			for(Class<?> clazz = eventClass; Event.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
				// This loop checks for extending deprecated events.
				if(clazz.getAnnotation(Deprecated.class) != null) {
					Warning warning = clazz.getAnnotation(Warning.class);
					WarningState warningState = server.getWarningState();
					if(!warningState.printFor(warning)) {
						break;
					}
					plugin.getLogger().log(
							Level.WARNING,
							String.format(
									"\"%s\" has registered a listener for %s on method \"%s\","
									+ " but the event is Deprecated. \"%s\"; please notify the authors %s.",
									plugin.getDescription().getFullName(),
									clazz.getName(),
									method.toGenericString(),
									(warning != null && warning.reason().length() != 0)
											? warning.reason() : "Server performance will be affected",
									Arrays.toString(plugin.getDescription().getAuthors().toArray())),
							warningState == WarningState.ON ? new AuthorNagException(null) : null);
					break;
				}
			}
			
			EventExecutor executor = new EventExecutor() {
				@Override
				public void execute(Listener listener, Event event) throws EventException {
					try {
						if(!eventClass.isAssignableFrom(event.getClass())) {
							return;
						}
						method.invoke(listener, event);
					} catch (InvocationTargetException ex) {
						throw new EventException(ex.getCause());
					} catch (Throwable t) {
						throw new EventException(t);
					}
				}
			};
			if(useTimings) {
				eventSet.add(
						new TimedRegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
			} else {
				eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
			}
		}
		return ret;
	}
	
	@Override
	public void enablePlugin(Plugin plugin) { }
	
	@Override
	public void disablePlugin(Plugin plugin) { }
	
}
