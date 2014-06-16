# Flare
#### An event-based notification system for Datomic.

## The goal
The goal is to create a library that enables developers to articulate that a
change in the database (a transaction) must notify other systems about said
change, not to add that various third parties might be expecting data that
looks different than our representation of it.

## Using Flare
Flare is on Clojars and you can use the following Leiningen dependency string
in your project.clj file.

```clojure
[org.vlacs/flare "0.1.0"]
```

## Getting started
Making events is very easy. There are are just two things you need to know about
creating events.
* All events must be registered with Flare. Flare must know about the kinds of
events that are being made before it is told about them.
* All events and notifications are transacted with the data the generates it.
    * This is a little unclear, Flare create event and notification data
    associated with a type. It's up to the calling application to transact the
    event and notifications with the data that is creating the event. That is
    what makes the event itself atomic.

### Registering an event type
Registering an event type is very easy. Flare has a 3 argument fn that will
transact the event type into the database for Flare while returning the keyword
that Flare will use to identify the event. The first argument is the Datomic
database connection, the second is a keyword that reflects the application that
the event belongs to, and the third argument is application-specific event name.

```clojure
(flare.event/register! db-conn :flare :test-event)
```

This fn returns a keyword that represents the transacted event-type.

```clojure
:flare.event-type/flare.test-event
```

This event type keyword can be reproduced using the following fn:

```clojure
(flare.event/slam-event-type :application :event-name)
```

### Making events
Making events is almost as easy as creating the event types themselves. Flare's
top level namespace has a wrapper for the event fn in ```flare.event``` so you
can either call ```flare/event``` or ```flare.event/event``` to create event
entities.

The ```flare.event/event``` fn takes 7 arguments, the first of which is always
the database connection. The second is the fully flare-qualified event name
using ```flare.event/slam-event-type``` or from the ```flare.event/register!```
fn. The third is a verison keyword, it's a unique identifier that says that the
version of the event being made is, generally this is updated when changes to
the API are made. The fourth arg is an entity ID of the user responsible for the
event being generated, it may be nil in which case, this attribute doesn't get
transacted into the database. The fifth arg is a list of user entity ids that
this event impacts, this can also be nil and behaves as the prior argument does.
The sixth argument is a human readable message that describes the event, and the
seventh is a payload (in edn) that represents the event being made.

There is an example of the usage of this fn in the ```flare.api.out```
namespace. It looks something like this:

```clojure
(defn make-ping-event!
  "Makes an event to ping third parties to see if they're accepting requests."
  [db-conn]
  (if (flare.db/upserted?
        (d/transact
          db-conn
          (event/event
            db-conn (event/slam-event-type :flare :ping)
            :v1 nil nil "Ping!" (util/->edn {:message "Ping!"}))))
    (do
      (timbre/debug "Internal ping event successfully generated.")
      true)
    (do
      (timbre/debug "Internal ping event failed to assert.")
      false)))
```

### Notifications

TODO: Write this part of the README. :)
