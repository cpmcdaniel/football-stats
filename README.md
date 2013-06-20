# football-stats

football-stats provides an API for accessing and storing NFL Regular
season statistics. The current target is a Datomic backend. With this
you will be able to perform all sorts of statistical analysis on NFL
game data.

## TODO

* Clean up storage.clj
  * Try to find declarative way to map NFL JSON to Datomic schema.
  * Remove duplication
* Resolve position for each player.
* Optionally store the drive summaries.
* Lamina channel for controlling nfl.com scraper (send season or [season week]).
* Function for reading the JSON off the filesystem and feeding to datomic storage channel.

## Usage

This project is not yet in a usable state and a lot of this is a work
in progress. Check back in the future for usage instructions.

## License

Copyright © 2012 Craig McDaniel

Distributed under the Eclipse Public License, the same as Clojure.
