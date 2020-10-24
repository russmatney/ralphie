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

TODO: implement feature that writes `docs/features.org` for each command.

Coming soon... `docs/features.org`

# Development

Ralphie expects to be run as a babashka process, but it can be developed as a
jvm app as usual (via cider/nrepl or something similar). That being said, not
all libraries will be BB compatible.

You can quickly test if the app/command works on the command line via:

``` sh
bb -cp $(which -Spath) -m ralphie.core <whatever-args>
```

I've had decent results with connecting to a bb process via nrepl, but it's not
as fully featured as working with the usual cider-nrepl. However, that usually
looks like running:

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```

Then `cider-connect`ing from emacs.

## Creating commands

A command can be added at a new namespace with somethine like:

```
(ns ralphie.my-new-ns
 (:require
  [ralphie.command :refer [defcom]]))

(defn my-new-handler
  ([] (my-new-handler nil nil))
  ([_config _parsed]
    (println "do-the-magic-here")))

(defcom my-new-cmd
  {:name          "my-new-cmd"
   :one-line-desc "Some well written, hopefully cheeky one-liner as a TLDR."
   :description   ["As much context as you feel necessary."]
   :handler       my-new-handler})
```

Note that this new namespace needs to be required somewhere - for now I require
everything in `ralphie.core`.

You can run the command locally via either:

``` sh
bb -cp $(which -Spath) -m ralphie.core my-new-cmd
```

...or by selecting `my-new-cmd` from a 'dev' rofi, similarly:

```
bb -cp $(which -Spath) -m ralphie.core rofi
```

The former is preferred for debugging, as the output is immediately accessible.

If you're confident with your code (because you developed in the clojure repl
anyway), you can also call `ralphie build-and-install-uberscript` (via cli or
global rofi, which is typically key-bound) to update the global uberjar, which
will then make `my-new-cmd` available via your typical `ralphie rofi` as well.

### `defcom` yasnippet

I use a `defcom` snippet to quickly add a new command, which looks something
like:

``` yasnippet
# -*- mode: snippet -*-
# name: defcom
# uuid: defcom
# key: defcom
# condition: t
# --
(:require
   [ralphie.command :refer [defcom]])

(defn $1-handler
  ([] ($1-handler nil nil))
  ([_config _parsed]
   $4))

(defcom $1-cmd
  {:name          "$1"
   :one-line-desc "$2"
   :description ["$3"]
   :handler       $1-handler})
```

This boilerplate should be shrunk over time, but for now I'm just happy to have
something easy to work with in the repl, and to be encouraging docs at a few
levels - the descriptions are intended to be featured in the help text on teh
command-line and in rofi, which helps find commands/other program interfaces
more easily.

If there are thoughts on a minimal way to make this pluggable, I'm all ears!
(Ralphie as a library, unified keybinding to show ralphie commands across
several apps, etc.)

# License

See `LICENSE`.

TLDR: MIT. Go hog-wild.
