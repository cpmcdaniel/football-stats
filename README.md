# football-stats

football-stats provides an API for accessing and storing NFL Regular
season statistics. The current target is a Datomic backend. With this
you will be able to perform all sorts of statistical analysis on NFL
game data.

## TODO

* Player enrichment
  * Player status (Active/IR/Retired/etc.) 
  * Position
  * NFL.com IDs. (change attr names)
* Clean up storage.clj
  * Try to find declarative way to map NFL JSON to Datomic schema.
  * Remove duplication
* Resolve position for each player (from profile page.)
* files->datomic should not create dupes (use tx attribute).

## Usage

This project is not yet in a usable state and a lot of this is a work
in progress. Check back in the future for usage instructions.

## License

Copyright © 2012 Craig McDaniel

Distributed under the Eclipse Public License, the same as Clojure.
