import io.github.pieter12345.javaloader.JavaLoaderProject;
import io.github.pieter12345.javaloader.utils.AnsiColor;

/**
 * ExampleProjectStandalone class.
 * This is an example project for the JavaLoader plugin.
 * Its purpose is to show how to create a project for the JavaLoader plugin.
 * @author P.J.S. Kools
 */
public class ExampleProjectStandalone extends JavaLoaderProject {
	
	@Override
	public void onLoad() {
		
		// Print feedback.
		System.out.println(AnsiColor.GREEN + "[DEBUG] ExampleProjectStandalone project loaded." + AnsiColor.RESET);
	}
	
	@Override
	public void onUnload() {
		
		// Print feedback.
		System.out.println(AnsiColor.RED + "[DEBUG] ExampleProjectStandalone project unloaded." + AnsiColor.RESET);
	}
	
	@Override
	public String getVersion() {
		return "0.0.1-SNAPSHOT";
	}
}