# MapTowny
A Towny add-on Spigot plugin that enables towns to show up on a web-map.

**Supported web-map plugins are Pl3xMap, squaremap, dynmap, and BlueMap**.

## Dependencies
This plugin requires Towny to be installed on your server.
- [Towny](https://github.com/TownyAdvanced/Towny) 0.97.1 (or later)

The plugin also requires one of the following web-map plugins to be installed on your server:
- [Pl3xMap](https://github.com/pl3xgaming/Pl3xMap) v1 (use this [fork](https://github.com/NeumimTo/Pl3xMap) for 1.18+) or v2 (alpha 1.19.2-319 or later)
- [squaremap](https://github.com/jpenilla/squaremap)
- [dynmap](https://www.spigotmc.org/resources/dynmap%C2%AE.274/)
- [BlueMap](https://www.spigotmc.org/resources/bluemap.83557/) v3.3 (or later) (limited support)

## Features
Honestly, nothing uber special about this plugin other than it works, but if you really want to know:
- Configurable Marker Options
- Async Processing
- Support multiple web-map plugins
- Unit-tested Custom Polygon Outline (contour) Algorithm and Negative Space (contour "hole-finding") Algorithm

## Installing
Install the plugin from the [releases page](https://github.com/TownyAdvanced/MapTowny/releases). Simply put the jar file into your `plugins` directory and start your server.

## Usage and Configuration
The plugin should be ready for use out of the box. The one thing that may need to be adjusted is the `enabled-worlds` property in the `config.yml` to add the world names that you want town claims to show up on. For more information about the plugin's commands and configuring the plugin, see the [wiki](https://github.com/TownyAdvanced/MapTowny/wiki).

## Plugin API:
See [this wiki page](https://github.com/TownyAdvanced/MapTowny/wiki/MapTowny-API) for more info.

## Building
This plugin is a standard maven project that utilizes the [maven toolchains plugin](https://maven.apache.org/plugins/maven-toolchains-plugin/) in order to build against multiple JDK versions. In order to use the maven toolchains plugin, you must have a `toolchains.xml` file in your `.m2` folder properly configured with at least JDK versions 1.8 and 16. See [this guide](https://maven.apache.org/guides/mini/guide-using-toolchains.html#using-toolchains-in-your-project) for more info on setting up a `toolchains.xml` file. 

Once that is setup, the project can be compiled with the command `mvn clean package` or `mvn clean install` ran in the project directory.

## Licensing
This plugin is licensed under the MIT license. While highly permissible, I do kindly hope that you create pull-requests for any bug fixes or useful features so they can help the entire community.

\*All pull-requests made to the repository agree to license the code contributed under the MIT license.
