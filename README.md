[![Build Status](https://travis-ci.org/sfx/gel.svg?branch=travis-ci)](https://travis-ci.org/sfx/gel)

# Gel

A Clojure implementation of the [Liquid](https://github.com/shopify/liquid) template engine.

## Release

```
[gel "0.1.0-SNAPSHOT"]
```

## Usage

```clojure
(require '[gel.core :as gel])

(gel/render "Hi {{ name }}!" {:name "Sophie"})
;=> "Hi Sophie!"
```

## License

Copyright © 2014 Jeremy Heiler, SFX Entertainment

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
