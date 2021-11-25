# Pl3xMap-Towny
A Towny and Pl3xMap add-on Spigot plugin that enables towns to show up on the Pl3xMap.

## Dependencies
This plugin requires you to have the following plugins on your server:
- [Pl3xMap](https://github.com/pl3xgaming/Pl3xMap)
- [Towny](https://github.com/TownyAdvanced/Towny) 0.97.1 (or later)

## Pl3xMap Archival
Pl3xMap has been archived and will no longer be receiving any updates. Thus, for versions past 1.17.1, there is no gurantee that this plugin or Pl3xMap will continue to work.
Any bugs specifically attributed towards this plugin will still be fixed, however, there will be no more major feature additions.

## Features
Honestly, nothing uber special about this plugin other than it works, but if you really want to know:
- Pretty Configurable
- Async Processing (may or may not be because the algorithms are a little computationally heavy *shhh*)
- Unit-tested Custom Polygon Outline (contour) Algorithm and Negative Space (contour "hole-finding") Algorithm

## Installing
You can install the plugin from the [releases page](https://github.com/silverwolfg11/Pl3xMap-Towny/releases). Simply put it into your `plugins` directory and start your server.

## Usage and Configuration
The plugin should be ready for use out of the box. The one thing that may need to be adjusted is the `enabled-worlds` property in the `config.yml` to add the world names that you want town claims to show up on. For more information about the plugin's commands and configuring the plugin, see the [wiki](https://github.com/silverwolfg11/Pl3xMap-Towny/wiki).

## Plugin API:
See [this wiki page](https://github.com/silverwolfg11/Pl3xMap-Towny/wiki/Pl3xMap-Towny-API) for more info.

## Building
This plugin is a simple maven project, and thus can be built via `mvn clean install` (or `mvn clean package` whatever you prefer).

## Licensing
This plugin is licensed under the MIT license. While highly permissible, I do kindly hope that you create pull-requests for any bug fixes or useful features so they can help the entire community.
