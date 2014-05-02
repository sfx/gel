# Gel

A Clojure implementation of the [Liquid](https://github.com/shopify/liquid) template engine.

## Usage

```clojure
(require '[gel.core :as gel])

(gel/redner "Hi {{ name }}!" {:name "Sophie"})
;=> "Hi Sophie!"
```

## License

Copyright © 2014 Jeremy Heiler, SFX Entertainment

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
