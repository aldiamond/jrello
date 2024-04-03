# jrello

Provides statistics for a Sendle trello board including:
* Count of cards completed
* Average cycle time per card
* Forecast days required to complete outstanding projects

## Prerequisites

#### 1. Create trello API user and add credentials to system.edn 

Follow the instructions on https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/ to create an API credentials and token.

Create a file `resources/system.edn` from the map below and add the API key and token.

```clojure
{:trello-cfg {:key    "changeme"
              :token  "changeme"}
 :trello-lists {:to-do       "64dd46ac74c7e393d202bd6f"
                :in-progress "64dd4b1363b05b68ba9329ba"
                :qa          "64dd46b61846bfbe144cd6f7"
                :done        "64dd46bc45c709a1c4fab85c"}}
```

#### 2. Setup automation to archive items in done column older than 40 working days

Using the automation power-up in Trello create a Scheduled 
* Select the automation Power-Up on your Trello board and Scheduled
* Click 'Create automation'
* Select 'Trigger' > Every day
* Select 'Action' > Move Cards > archive all the cards 'more than 40 working days' in list 'done'. `Tip: click the hour glass to add days filter`
* Click 'Save'

## Installation

* Install java jdk: `brew install openjdk`
* Install lein: https://leiningen.org/

## Adding a new trello board

TODO...

## Build

Build jar:

    $ lein uberjar

## Usage

Run program main function:

    $ java -jar jrello-0.1.0-standalone.jar

### Future Enhancements

* Enhance forecasts to take into consideration time work has been in progress (Cards in lists "In Progress" and "QA") relative to average cycle time.
* Select lists for multiple boards
* Write some tests

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2022 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
