# AdmStorm Changelog

## [Unreleased]

## 2.0.8 - 17.07.2025

- Added option to select a remote synchronization folder
- Added possibility to specify custom path to `OpenSC` provider
- Fixed access to closed api ui in commit synchronization check window
- Fixed an exception when SSH channel could not be opened without Yubikey key

## 2.0.7 - 01.03.2025

- Adapt code for PhpStorm 2024.3

## 2.0.6 - 16.09.2024

- Adapt code for PhpStorm 2024.2

## 2.0.5 - 28.06.2024

- More human readable ssh errors
- Notification about not availability on Windows
- Fixed not correct yubikey reset
- New ssh exception handler

## 2.0.4 - 02.05.2024

- Adapt code for PhpStorm 2024.1

## 2.0.3 — 14.02.2024

- Adapt code for PhpStorm 2023.3
- Jira tickets are now highlighted again
- Added yubikey PIN characters validator
- Added notifications about new versions of plugins

## 2.0.2 — 21.08.2023

- Adapt code for PhpStorm 2023.2

## 2.0.1 — 14.08.2023

- Adapt code for PhpStorm 2023.1

## 2.0.0 — 11.05.2023

- Second stable version

### Added

- `@admstorm-marker` - allowing you to see the values directly in the IDE

## 1.3.5 — 07.03.2023

### Fixed

- Bug when checking sync branches

## 1.3.4 — 23.12.2022

### Fixed

- Bug when checking the diff by file

## 1.3.3 — 15.12.2022

- Adapt code for PhpStorm 2022.3

### Removed

- Links for Jira tickets in PHP, JS, and CSS comments

## 1.3.2 — 31.08.2022

### Fixed

- Exception in 2022.2.1 about `GitUtils.getCurrentRepository()`
- Bug with API tests for PHPUnit

## 1.3.1 — 29.06.2022

### Added

- Action on the Help menu to send logs to Sentry.
- Debug log console like yarn watch

### Other improvements

- Users can now run PHPUnit tests from the context menu of a file.
- Users can now run ktest benchmarks from the context menu of a file.
- On the installation page, the point about opening the project has been clarified.
- On the installation page, the section about Yubikey has been updated,
- Added a clarification about the settings page.
- Added settings to enable/disable widgets.
- Updated api tests.
- Added checking of the new plugin version available.

### Fixed

- Freeze when running a PHPUnit test, or a ktest benchmark.
- Freeze when connection is lost for yarn watch.
- Launch of a separate benchmark method, now if a method with a name that's a prefix for the name of other methods is
  launched, then other methods won't be launched.
- Exception when the plugin automatically opens a new SSH tunnel.
- Performance warning for line marker for ktest.
- Yarn watch no longer appears in non-vkcom projects.
- Order of arguments for new api tests.
- Server name on the plugin settings page to the correct one Dev-server.
- Bug when mark yarn watch as running when connection is lost.

## 1.3.0 — 22.06.2022

### Added

- Full support for ktest benchmarks:
    - Running a benchmark on an entire class or a single method
    - Launching benchmark for memory allocations
    - Comparison of two classes and two methods
    - Checking the name of the benchmark class
- Yarn Watch support
- Ability to share code from KPHP Playground
- Ability to view KPHP compilation output in Playground
- Links for Jira tickets in PHP, JS, and CSS comments

### Other improvements

- Number of commits that are shown during synchronization is reduced to 30
  Now if more than 30 commits require synchronization, then only the first 30 and the ellipsis at the end will be shown.
- Settings page has become more descriptive, comments have been added to some settings.
- Added sending errors to Sentry
- Added a welcome message for each update with a link to the whats-new page.

### Changed

- Now, when resetting the yubikey, the "Remember" checkbox will be selected by default in the yubikey password reset
  dialog

### Fixed

- Function of determining that this is vkcom project, now it does not consider kphp-polyfills to be the right project to
  enable the plugin
- Bug when, for third-party projects, when committing, the plugin tried to push changes on the server.
- OpenSC path, now automatic collection of yubikey will work on Linux.
- Problems with opening SSH tunnels.
- Password saving when reset yubikey.
- Bug, when updating the size in the call, the size in the link wasn't updated.

## 1.2.0 — 11.05.2022

Debugging for PHP scripts and PHPUnit tests right in the IDE, new action "Run Anything on Server", support for new API
tests, automatic import of classes when creating KPHP Playground from code and bug fixes.

### Added

- Support for debug PHP scripts
- Support for debug PHPUnit tests
- Support for new API tests
- New "Run Anything on Server" action
- Auto import classes for KPHP Playground
- Gutter icons for debug log calls

### Other improvements

- Better handling of git pull problems

### Fixed

- Several bugs with tests run configuration
- Exception about impossibility to get text from binary file

## 1.1.1 — 11.05.2022

Adapt code for PhpStorm 2022.1

## 1.1.0 — 21.02.2022

New KPHP Playground, automatic reset of Yubikey in case of connection problems, automatic push to the server after a
commit, generation of builders directly in the IDE, new options for synchronizing autogenerated files, and other minor
improvements and fixes.

### Added

- New KPHP Playground, now it does not depend on repository files and will not interfere with pushing to Gitlab;
- Action `Execute Selected in Playground` to the editor context menu, now you can create a new KPHP Playground from
  selected piece of code;
- Action in notifications about the inability to connect via SSH, which resets the Yubikey and connects again;
- Commit and push to Gitlab button in the commit window;
- Now, after creating a commit, it is automatically pushed to the development server;
- Run configuration for generating builders with automatic download of modified files;
- Hastebin from selected text only;
- Action `Sync Autogenerated Files` to the `Tool | server`, which makes it easy to sync autogenerated files after
  executing some command on server.

  > If the file was deleted on the server, it will be deleted locally as well.

### Other improvements

- Improved file sync dialog;
    - Files are now displayed sorted (by state);
    - For files that are only in the local repository, added the ability to delete them;
    - Now the full path to the file is displayed starting from `~/`;
    - Now after clicking the button to download a file from the server, it will be started immediately, and not after
      closing the dialog;
    - Now if the file is located only locally or only on the server, when viewing its contents, it will be displayed in
      a simple viewer, and not in a diff viewer with two editors;
    - Fixed a bug when deleting a file from the list, the focus did not go to the next one, and it was necessary to
      select the file manually.

### Fixed

- `AlreadyDisposedException` when closing a project and opening other projects. Also thanks to this, now when switching
  projects, the synchronization check does not break when focusing on the IDE;
- Bug with slashes during file synchronization for Windows users;
- Bug when, during KPHP execution, closing one tab would close another;
- Bug when launching PHPUnit from an icon created new duplicate configurations.

<br>

## 1.0.0 — 07.02.2022

First stable version.

### Features

- Checking the synchronization of the local repository and the repository on the development server;
- Additional actions for push and pull, taking into account hooks on the development server;
- Various configurations for launching KPHP;
- PHPUnit and PHPLinter run configurations;
- Built-in KPHP Playground and Hastebin;
- Ability to view the generated C++ code for the entire site.
