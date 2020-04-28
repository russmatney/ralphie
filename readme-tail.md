# Development

Run it

``` sh
bb --classpath src --main ralphie.core -- -h
```

Run an nrepl server to connect to.

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```


