import io.github.pieter12345.javaloader.core.utils.AnsiColor;
import io.github.pieter12345.javaloader.velocity.JavaLoaderVelocityProject;
import net.kyori.adventure.text.Component;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

/**
 * ExampleProject class.
 * This is an example project for the JavaLoader plugin.
 * Its purpose is to show how to create a project for the JavaLoader plugin.
 * @author P.J.S. Kools
 */
public class ExampleProject extends JavaLoaderVelocityProject {
	
	@Override
	public void onLoad() {
		
		// Register all event listeners within this class.
		this.getProxyServer().getEventManager().register(this, this);
		
		// Register an example command.
		this.getProxyServer().getCommandManager().register("examplecommand", new SimpleCommand() {
			
			@Override
			public void execute(Invocation invocation) {
				invocation.source().sendMessage(Component.text(
						"[DEBUG] [ExampleProject] Command executed: /" + invocation.alias()));
			}
		});
		
		// Print feedback.
		this.getLogger().info(AnsiColor.GREEN + "[DEBUG] ExampleProject project loaded." + AnsiColor.RESET);
	}
	
	@Override
	public void onUnload() {
		
		// Unregister all listeners from this project.
		this.getProxyServer().getEventManager().unregisterListeners(this);
		
		// Unregister the example command.
		this.getProxyServer().getCommandManager().unregister("examplecommand");
		
		// Print feedback.
		this.getLogger().info(AnsiColor.RED + "[DEBUG] ExampleProject project unloaded." + AnsiColor.RESET);
	}
	
	@Override
	public String getVersion() {
		return "0.0.1-SNAPSHOT";
	}
	
	@Subscribe
	public void onServerConnectedEvent(ServerConnectedEvent event) {
		event.getPlayer().sendMessage(Component.text("[JavaLoader ExampleProject] Welcome to the server."
				+ " This message can be configured in the JavaLoader ExampleProject."));
	}
}
