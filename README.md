# flare

To reproduce our voom issue:

1. clone this repo
2. git checkout voom-boom
3. lein voom freshen
 1. Works. Yay!
4. lein voom build-deps
 1. Does not work. Sad. :(

In order to make it work, it appears that I need to:
1. Move :plugins [[lein-voom "0.1.0-SNAPSHOT" :exclusions [org.clojure/clojure]]] from project.clj to ~/.lein/profiles.clj
2. Move the helmsman dep from {:profiles {:dev {:dependencies ...}}} to the
   top-level project map.

## Releases and Dependency Information

* Releases are published to TODO_LINK

* Latest stable release is TODO_LINK

* All released versions TODO_LINK

[Leiningen] dependency information:

    [flare "0.1.0-SNAPSHOT"]

[Maven] dependency information:

    <dependency>
      <groupId>flare</groupId>
      <artifactId>flare</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>

[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/



## Usage

TODO



## Change Log

* Version 0.1.0-SNAPSHOT



## Copyright and License

Copyright © 2014 TODO_INSERT_NAME

TODO: [Choose a license](http://choosealicense.com/)
