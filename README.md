# JavaLoader ![Travis CI status](https://travis-ci.org/Pieter12345/JavaLoader.svg?branch=master)
A Bukkit plugin which allows loading, unloading and compiling java projects in runtime.

## Discord ![Discord](https://img.shields.io/discord/709884147044188321)
Click [here](https://discord.gg/sGgXTfC) to join our Discord server.

## Permissions
| Permission     | Description |
|----------------|-------------|
| javaloader.use | All usage   |

## Usage (Bukkit)
Place the JavaLoader jar file in the `<server dir>/plugins` directory of your Bukkit server. When you start your server and no projects currently exist, an example project will be put in the `<server dir>/plugins/JavaLoader/JavaProjects` directory. From looking at this example project, you will be able to see how to set up a project for JavaLoader.
#### JavaLoader project properties:
 - The projects main class (and only that class) extends `io.github.pieter12345.javaloader.bukkit.JavaLoaderBukkitProject`. The usual `org.bukkit.plugin.java.JavaPlugin` class functionalities from Bukkit are implemented in the `io.github.pieter12345.javaloader.bukkit.JavaLoaderBukkitProjectPlugin` class which can be obtained using `JavaLoaderBukkitProject`'s `getPlugin()` method.
 - The projects main class has no constructors defined or has the empty constructor defined and is publicly accessible.
 - The projects main class can (but does not have to) override `public void onLoad()` and `public void onUnload()`, which are called when the project is being loaded and unloaded respectively.
 - The projects main class implements `public String getVersion()`, which should return the version of your project (e.g. "0.0.1-SNAPSHOT"). This method is invoked before the `onLoad()` method is invoked and it is advised to hard-code this.

#### Adding commands:
 - The projects main class can override the empty implementation of Bukkit's `CommandExecutor` and `TabCompleter` interfaces for command handling and tab complete handling.
 - Commands can be added through overriding the `public BukkitCommand[] getCommands()` method in the projects main class. These commands will be inserted before the `onLoad()` method is invoked, so it is advised to hard-code these commands.

#### Dependency management
 - The JavaLoader jar file and the Bukkit API (including its implementation) are automatically included in the class path.
 - Additional dependencies can be added by creating the `<server dir>/plugins/JavaLoader/JavaProjects/<your project>/dependencies.txt` file with line-seperated dependency descriptors.
 - Jar dependencies can be included by adding the following line to the dependencies.txt file:
`jar -scope path` where scope is optional and can be `provided` or `include` (defaults to `include`) and path is the path to the .jar file. If the path starts with a dot, the relative path is taken from the project directory.
 - JavaLoader project dependencies can be included by adding the following line to the dependencies.txt file:
`project projectName` where projectName is the name of the project.
 - Bukkit plugin dependencies can be included by adding the following line to the dependencies.txt file:
`plugin pluginName` where pluginName is the name of the plugin as known by Bukkit.
 - Libraries that are bundled with Minecraft (MC 1.18+) can be depended on by adding the following line to the dependencies.txt file:
 `mclib libName` where libName is the name of the bundled library jar (including the version number and excluding the `.jar` file extension).
 - Circular project dependencies are not allowed.
 - In bulk load/unload/compile operations, an order is ensured in which all loaded projects can be certain that their children are loaded as well. So if A depends on B, then B would load before A and A would unload before B.
 - When a class is defined in multiple places, the first found definition is used. The classloading search order is: `project` > `include scope dependencies` > `project dependencies (including their dependencies)` > `Server main ClassLoader (Bukkit classes and possibly Bukkit plugin classes)` > `JavaLoader plugin classloader (Bukkit plugin classes)`.

## Contributing
You can contribute to this project by reporting bugs using the issue tracker on GitHub. Pull requests will not be accepted at this time.
