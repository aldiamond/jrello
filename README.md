# jrello

Provides statistics for a trello board including:
* Count of cards completed
* Average cycle time per card
* Cards completed per-week including by card type (Project name, Enhancement or Bug)
* Forecast days required to complete outstanding projects

## Prerequisites

#### 1. Create trello API user and add credentials to system.edn 

Follow the instructions on https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/ to create an API credentials and token.

Create a file `resources/system.edn` from the map below:
1. Add the API key and token.
2. Add a trello boards see instruction below.

```clojure
{:trello-cfg {:key    "changeme"
              :token  "changeme"}
 :trello-boards [{:id   "board id"
                  :name "board name"
                  :trello-lists {:to-do       "id"
                                 :in-progress "id"
                                 :qa          "id"
                                 :done        "id"}}]}
```

#### 2. Setup automation to archive items in done column older than 40 working days

Using the automation power-up in Trello create a Scheduled 
* Select the automation Power-Up on your Trello board and Scheduled
* Click 'Create automation'
* Select 'Trigger' > Every day
* Select 'Action' > Move Cards > archive all the cards 'more than 60 working days' in list 'done'. `Tip: click the hour glass to add days filter`
* Click 'Save'

## Installation

* Install java jdk: `brew install openjdk`
* Install lein: https://leiningen.org/

## Adding a new trello board

1. Get the Board ID from the Trello board URL: https://trello.com/b/<board-id>/<board-name>
2. Run the function and populate the board list ids for To Do, In Progress, QA and Done.
```clojure
(jrello.trello.trello-api/get-lists-on-a-board "board-id")
```
3. Add another board map to the `:trello-boards` vector in `resources/system.edn`

## Build

Build jar:

    $ lein uberjar

## Usage

Run program main function:

    $ java -jar jrello-0.1.0-standalone.jar

Options:

    --save-csv: Save CSV of completed card Trello data for each board

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
