# Ralphie

A set of commands for the command line, keybindings, and rofi,
expressed via Clojure and [Babashka](https://github.com/borkdude/babashka/).

## Ralphie?

This library was nearly named `bb-gun` as a reference to `babashka`'s `bb`
command. I left it to squat less on `bb`, as this is more of an application
than library. But be warned, this one will shoot your eye out!

> Ralphie as Adult: [narrating] Meanwhile, I struggled for exactly the right BB
> gun hint. It had to be firm, but subtle.
>
> Ralphie: Flick says he saw some grizzly bears near Pulaski's candy store!
>
> [Everyone stares at Ralphie]
>
> Ralphie as Adult: [narrating] They looked at me as if I had lobsters crawling
> out of my ears.

This mostly speaks to Ralphie knowing he wants to write something in Babashka,
whether there are bears near Pulaski's or not.

# Status

Untested on other machines, and not yet configurable. You could clone and modify
it to work, but this is not a plug-n-play tool yet.

One blocker: a different approach to running the tool is necessary, as it
currently changes into a hard-coded directory to run. I thought Babashaka's
`--uberscript` option would suffice for this, but my attempts have not yet made
that work.

Another blocker: the paths in `ralphie.config` are hard-coded for now, but
could/should be read from a config somewhere.

# Background

Ralphie's main goal is to reduce the overhead of automating common dev tasks.
Babashka has made it possible to write clojure where you might otherwise fall
back to Bash - Ralphie is my attempt to rewrite my bash scripts in a way that is
more maintainable (i.e. in clojure).

Ralphie provides a registry-macro `defcom` which can be used to create a
command. It then exposes all of its commands both on the command line and via
[rofi](https://github.com/davatorium/rofi).

My current setup includes firing ralphie commands from i3, and mapping `ralphie
rofi` to `super+x`. I hope to treat it like `M-x` in Emacs.

I like to think of `ralphie` as a clojure version of the use-case
[Alfred](https://www.alfredapp.com/) filled on OSX.

# Features

This section is generated by `ralphie`'s [build-readme
command](https://github.com/russmatney/ralphie/blob/e67ab9be12731ff0d6418a63357053b6e841f2a4/src/ralphie/readme.clj#L44),
which was an experiment that may have run its course.

This list contains wip-commands as well.

### TODO clean up this feature list

Many of these are wips, or their usage examples are not useful.

## `any-outdated`: Checks if watched repos have any outdated deps.
```sh
ralphie any-outdated
```
## `autojump`: Sends `j <userinput>` to the current workspace's tmux.
Uses j (autojump) to fuzzy-find a directory.
Opens that directory in the workspace terminal.
```sh
ralphie autojump
```
## `build-readme`: build-readme
```sh
ralphie build-readme
```
## `clone`: Clone from your Github Stars
When passed a repo-id, copies it into ~/repo-id.
Depends on `hub` on the command line.
Does not support private repos.
If no repo-id is passed, fetches stars from github.
```sh
ralphie clone
```
## `date`: Prints the date
```sh
ralphie date
```
## `doctor-checkup`: Debug helper for sanity-checking
Runs a sanity check on your built config, and logs a report.
```sh
ralphie doctor-checkup
```
## `export-notes`: Exports an org notes dir as markdown with backlinks.
Parses an org notes dir into `items`.
Writes those items to an out dir as markdown.
Adds handles backlinks to all notes.
```sh
ralphie export-notes
```
## `fire`: Fires a command in the nearest tmux shell.

```sh
ralphie fire
```
## `gprom`: gprom

```sh
ralphie gprom
```
## `help`: Prints help
Prints the known commands and the parsed input.
```sh
ralphie help
```
## `install`: Installs ralphie via symlink.
Symlinks the project's src/ralphie.core.clj into ~/.local/bin/ralphie
```sh
ralphie install
```
## `open-emacs`: Opens emacs in the current workspace
```sh
ralphie open-emacs
```
## `open-term`: Opens a terminal.
Hardcoded to alacritty and tmux.
Opens tmux using the current i3 workspace name.
```sh
ralphie open-term
```
## `rename-workspace`: Updates a workspace to match the passed data
Supports name as the first argument.
```sh
ralphie rename-workspace
```
## `resize-window`: resize-window
Resizes the window according to a few presets.
Depends on i3.
```sh
ralphie resize-window
```
## `restart-i3`: Restarts i3 in place
Pulls workspace config from workspaces.org.
Writes a new i3/config.
Restarts i3.
```sh
ralphie restart-i3
```
## `restart-workspaces`: restart-workspaces
Restarts i3 in place.
Builds an updated config based on workspaces.org
```sh
ralphie restart-workspaces
```
## `restart-yodo`: Restart the locally-running yodo servers.
```sh
ralphie restart-yodo
```
## `rofi`: Select a command to run via rofi.
Open Rofi for each command.
Fires the selected command.
Expects rofi to exist on the path.
```sh
ralphie rofi
```
## `scratchpad-pop`: Shows the next scratchpad in the workspace.
```sh
ralphie scratchpad-pop
```
## `scratchpad-push`: Pushes the focused window to the workspace scratchpad.
```sh
ralphie scratchpad-push
```
## `screenshot`: Take Screenshot
Takes a screenshot.
Not yet implemented.
```sh
ralphie screenshot
```
## `start-workspace`: start-workspace
Creates a new workspace based on workspaces.org and rofi input.
```sh
ralphie start-workspace
```
## `story`: story
Starts a story
```sh
ralphie story
```
## `update-doom`: Fetch and rebuild Doom
Updates doom emacs to the bleeding-edge latest.
Good luck out there, sport!
```sh
ralphie update-doom
```

# Development

I develop ralphie with emacs and cider. To start an nrepl session, you can run:

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```

Then cider-connect from emacs.

# Running

At present, the entrypoint for ralphie is `ralphie.core`. If you [look in
there](https://github.com/russmatney/ralphie/blob/e67ab9be12731ff0d6418a63357053b6e841f2a4/src/ralphie/core.clj#L1)
you'll see a strange, multi-line shebang that has a hard-coded path. I've made
this file executable, and symlinked it into my PATH as `ralphie`.

