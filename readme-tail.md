
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

