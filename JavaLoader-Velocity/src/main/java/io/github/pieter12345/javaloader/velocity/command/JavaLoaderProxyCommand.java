package io.github.pieter12345.javaloader.velocity.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;

import io.github.pieter12345.javaloader.core.CommandExecutor;
import io.github.pieter12345.javaloader.core.CommandExecutor.CommandSender;
import io.github.pieter12345.javaloader.core.CommandExecutor.MessageType;
import io.github.pieter12345.javaloader.core.ProjectManager;
import io.github.pieter12345.javaloader.core.utils.AnsiColor;
import io.github.pieter12345.javaloader.core.utils.Utils;
import net.kyori.adventure.text.Component;

/**
 * This class represents the "/javaloaderproxy" command.
 * @author P.J.S. Kools
 */
public class JavaLoaderProxyCommand implements SimpleCommand {
	
	private final String infoPrefix;
	private final String errorPrefix;
	private final CommandExecutor commandExecutor;
	private final ProjectManager projectManager;
	
	public JavaLoaderProxyCommand(String infoPrefix, String errorPrefix,
			CommandExecutor commandExecutor, ProjectManager projectManager) {
		this.infoPrefix = infoPrefix;
		this.errorPrefix = errorPrefix;
		this.commandExecutor = commandExecutor;
		this.projectManager = projectManager;
	}
	
	@Override
	public void execute(Invocation invocation) {
		this.commandExecutor.executeCommand(new CommandSender() {
			
			@Override
			public void sendMessage(MessageType messageType, String message) {
				invocation.source().sendMessage(Component.text(
						this.getPrefix(messageType) + message + AnsiColor.RESET));
			}
			
			@Override
			public void sendMessage(MessageType messageType, String... messages) {
				if(messages.length > 0) {
					invocation.source().sendMessage(Component.text(this.getPrefix(messageType)
							+ Utils.glueIterable(Arrays.asList(messages), (str) -> str, "\n") + AnsiColor.RESET));
				}
			}
			
			public String getPrefix(MessageType messageType) {
				switch(messageType) {
					case ERROR:
						return JavaLoaderProxyCommand.this.errorPrefix;
					case INFO:
						return JavaLoaderProxyCommand.this.infoPrefix;
					default:
						throw new Error("Unimplemented "
								+ MessageType.class.getSimpleName() + ": " + messageType.name());
				}
			}
		}, invocation.arguments());
	}
	
	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("javaloader.use");
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		String[] args = invocation.arguments();
		String search = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
		
		// TAB-complete "/javaloaderproxy <arg>".
		if(args.length <= 1) {
			List<String> ret = new ArrayList<String>();
			for(String comp : new String[] {"help", "list", "load", "unload", "recompile"}) {
				if(comp.startsWith(search)) {
					ret.add(comp);
				}
			}
			return ret;
		}
		
		// TAB-complete "/javaloaderproxy <load, unload, recompile> <arg>".
		if(args.length == 2 && (args[0].equalsIgnoreCase("load")
				|| args[0].equalsIgnoreCase("unload") || args[0].equalsIgnoreCase("recompile"))) {
			List<String> ret = new ArrayList<String>();
			for(String comp : JavaLoaderProxyCommand.this.projectManager.getProjectNames()) {
				if(comp.toLowerCase().startsWith(search)) {
					ret.add(comp);
				}
			}
			return ret;
		}
		
		// Subcommand without tabcompleter.
		return Collections.emptyList();
	}
}
