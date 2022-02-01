[![Version](https://img.shields.io/jetbrains/plugin/v/18535-admstorm.svg)](https://plugins.jetbrains.com/plugin/18535-admstorm)
[![Build](https://github.com/i582/admstorm/actions/workflows/build.yml/badge.svg)](https://github.com/i582/admstorm/actions/workflows/build.yml)

# <img src="src/main/resources/META-INF/pluginIcon.svg"> AdmStorm plugin for PhpStorm

## About

<!-- Plugin description -->

AdmStorm is a PhpStorm plugin that adds VK internal tools directly to the IDE and adds knowledge about the development
server.

> This plugin is intended only for VK employees.

**Features**

- Checking the synchronization of the local repository and the repository on the development server
- Additional actions for push and pull, taking into account hooks on the development server
- Various configurations for launching KPHP
- PHPUnit and PHPLinter run configurations
- Built-in KPHP Playground and Hastebin
- Ability to view the generated C++ code for the entire site

<!-- Plugin description end -->

This plugin aims to reduce the necessary context switches when working. It implements this through synchronization
checks, as well as built-in tools that can now be run directly from the IDE and additional push and pull actions
directly from the IDE, taking into account all the hooks on the development server.

You can find this plugin on the official [JetBrains plugins](https://plugins.jetbrains.com/plugin/18535-admstorm)
website.

## Installation

- Using IDE built-in plugin system:

  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "AdmStorm"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/VKCOM/admstorm/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Details

[Landing page (in Russian)](https://vkcom.github.io/admstorm/)

## For plugin developer

This plugin can be useful for plugin developers who need to solve similar problems. You can find how to work with SSH
there, including sending commands and handling their output. How to keep track of when the connection was dropped and
how to handle this so that the process doesn't get dropped. Also, you can find how you can synchronize two repositories,
how to upload and download files via SFTP.

## License

This project is under the **MIT License**. See the [LICENSE](https://github.com/VKCOM/admstorm/blob/master/LICENSE) file
for the full license text.
