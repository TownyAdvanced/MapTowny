# Pl3xMap-Towny
A Towny and Pl3xMap add-on Spigot plugin that enables towns to show up on the Pl3xMap.

**WARNING: This plugin is still in the alpha release stage, so there may be breaking bugs**. If you encounter any bugs, please report them on the issue tracker.

## Dependencies
This plugin requires you to have the following plugins on your server:
- [Pl3xMap](https://github.com/pl3xgaming/Pl3xMap)
- [Towny](https://github.com/TownyAdvanced/Towny)

## Features
Honestly, nothing uber special about this plugin other than it works, but if you really want to know:
- Pretty Configurable
- Async Processing (may or may not be because the algorithms are a little computationally heavy *shhh*)
- Unit-tested Custom Polygon Outline (contour) Algorithm and Negative Space (contour "hole-finding") Algorithm

## Installing
You can install the plugin from the [releases page](https://github.com/silverwolfg11/Pl3xMap-Towny/releases). Simply put it into your `plugins` directory and start your server.

## Usage
The plugin will immediately render all towns on your Pl3xMap when the server starts up.

The plugin has one main command: `pl3xmaptowny` with aliases `plexmaptowny` and `plextowny`. The general permission to use the command is `pl3xmaptowny.use`.
The command has the following sub-commands:
| Sub-Command | Description | Permission |
| --- | --- | --- |
| `reload` | Reloads the plugin. This includes all files in the plugin directory as well as re-rendering all towns on the map immediately. | `pl3xmaptowny.reload` |
| `render [town]` | Render a specific town on the Pl3xMap. If a town is already rendered, it will re-render the town. | `pl3xmaptowny.render` |
| `unrender [town]` | Un-render (remove) a town from the Pl3xMap. | `pl3xmaptowny.unrender` |

The admin permission to allow access to the command and all sub-commands is `pl3xmaptowny.admin`.

## Configuring
The plugin generates three editable files: `config.yml`, `click_tooltip.html`, and `hover_tooltip.html`. The `config.yml` has comments for each node which should be explanatory.
You may want to edit the `enabled-worlds` section first to support your specific worlds. 
The HTML files are used to generate the information for each town when you click or hover over the claim area on the map.

*A wiki-page is on the way for better explanation.*

## Building
This plugin is a simple maven project, and thus can be built via `mvn clean install` (or `mvn clean package` whatever you prefer).

## Licensing
Currently, this plugin is under full copyright, although an OSS license will be added very soon (most likely MIT).
