# Ralphie

A set of tools for cli and keybindings, written in
Clojure/[[https://github.com/borkdude/babashka/][Babashka]].

# Ralphie?

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

# Features
## `autojump`: Sends `j <userinput>` to the current workspace's tmux.
Uses j (autojump) to fuzzy-find a directory.
Opens that directory in the workspace terminal.
```sh
ralphie autojump
```
```
bindsym  exec --no-startup-id ralphie autojump
```
## `date`: Prints the date
```sh
ralphie date
```
```
bindsym  exec --no-startup-id ralphie date
```
## `help`: Prints help
Prints the known commands and the parsed input.
```sh
ralphie help
```
```
bindsym  exec --no-startup-id ralphie help
```
## `screenshot`: Take Screenshot
Takes a screenshot.
Not yet implemented.
```sh
ralphie screenshot
```
```
bindsym  exec --no-startup-id ralphie screenshot
```
## `rofi`: Select a command to run via rofi.
Open Rofi for each command.
Fires the selected command.
Expects rofi to exist on the path.
```sh
ralphie rofi
```
```
bindsym  exec --no-startup-id ralphie rofi
```
## `open-term`: Opens a terminal.
Hardcoded to alacritty and tmux.
Opens tmux using the current i3 workspace name.
```sh
ralphie open-term
```
```
bindsym  exec --no-startup-id ralphie open-term
```
## `open-emacs`: Opens emacs in the current workspace
```sh
ralphie open-emacs
```
```
bindsym  exec --no-startup-id ralphie open-emacs
```
## `story`: story
Starts a story
```sh
ralphie story
```
```
bindsym  exec --no-startup-id ralphie story
```
## `clone`: Clone from your Github Stars
When passed a repo-id, copies it into ~/repo-id.
Depends on `hub` on the command line.
Does not support private repos.
If no repo-id is passed, fetches stars from github.
```sh
ralphie clone
```
```
bindsym  exec --no-startup-id ralphie clone
```
## `doctor-checkup`: Debug helper for sanity-checking
Runs a sanity check on your built config, and logs a report.
```sh
ralphie doctor-checkup
```
```
bindsym  exec --no-startup-id ralphie doctor-checkup
```
## `find-deps`: find-deps
Looks up clojars libs for the passed str.
```sh
ralphie find-deps
```
```
bindsym  exec --no-startup-id ralphie find-deps
```
## `install`: Installs ralphie via symlink.
Symlinks the project's src/ralphie.core.clj into ~/.local/bin/ralphie
```sh
ralphie install
```
```
bindsym  exec --no-startup-id ralphie install
```
## `update-doom`: Fetch and rebuild Doom
Updates doom emacs to the bleeding-edge latest.
Good luck out there, sport!
```sh
ralphie update-doom
```
```
bindsym  exec --no-startup-id ralphie update-doom
```
## `build-readme`: build-readme
```sh
ralphie build-readme
```
```
bindsym  exec --no-startup-id ralphie build-readme
```
## `workspace-upsert`: Updates a workspace to match the passed data
Supports :name.
Not yet implemented.
```sh
ralphie workspace-upsert
```
```
bindsym  exec --no-startup-id ralphie workspace-upsert
```## TODO
### Resize monitor resolution
### Restart i3
### Git Clone
### Doctor scripts
### Take Screenshot with metadata
### Universal Focus Movement
### Open Emacs for current workspace
### Toggle scratchpad for current workspace
### Toggle global scratchpad
# Development

Run it

``` sh
bb --classpath src --main ralphie.core -- -h
```

Run an nrepl server to connect to.

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```


