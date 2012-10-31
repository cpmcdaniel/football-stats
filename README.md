# football-stats

football-stats provides an API for accessing and storing NFL Regular
season statistics. The current target is a Datomic backend. With this
you will be able to perform all sorts of statistical analysis on NFL
game data.

## TODO

* Flatten the game entity.
* Create a tx function that looks up the player by nflid and
  adds the appropriate ref attribute.
* Store game stats for players.
* Resolve position for each player.
* Apply same procedure to store visitor team stats.
* Optionally store the scoring summary.
* Optionally store the drive summaries.


## Usage

This project is not yet in a usable state and a lot of this is a work
in progress. Check back in the future for usage instructions.

## License

Copyright © 2012 Craig McDaniel

Distributed under the Eclipse Public License, the same as Clojure.
