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
## `date`: Prints the date
```sh
ralphie date
```
```
bindsym  exec --no-startup-id ralphie date
```
## `help`: Prints help
Takes a screenshot
```sh
ralphie help
```
```
bindsym  exec --no-startup-id ralphie help
```
## `screenshot`: Take Screenshot
Takes a screenshot
```sh
ralphie screenshot
```
```
bindsym  exec --no-startup-id ralphie screenshot
```
## `rofi`: Open Rofi
Open Rofi for each command
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
## ``
```sh
ralphie 
```
```
bindsym  exec --no-startup-id ralphie 
```
## `install`: Installs ralphie. Currently hard-coded :(
```sh
ralphie install
```
```
bindsym  exec --no-startup-id ralphie install
```
## `build-readme`: build-readme
```sh
ralphie build-readme
```
```
bindsym  exec --no-startup-id ralphie build-readme
```
## `workspace-upsert`: Updates a workspace to match the passed data
Supports :name
```sh
ralphie workspace-upsert
```
```
bindsym  exec --no-startup-id ralphie workspace-upsert
```# Development

Run it

``` sh
bb --classpath src --main ralphie.core -- -h
```

Run an nrepl server to connect to.

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```


