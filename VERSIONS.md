## 0.3.0 - Postgres Binders - Config and Factories

Implement Binders as Postgres client hence interface compatible with PostgresQL, Amazon 
Aurora, etc. etc.

Mewbase options are now replaced with config more suited to a library, hence using
lightbend config lib.

The config can spec implementations of Binders, EventSource(s) and EventSinks
hence it is possible to switch the builtin implementation or indeed to create new ones 
and dynamically load these, by name, as plug in replacements for the provided ones.





## 0.2.1 - 

## 0.2.0 - Experimental - Extrenal Services

Experiment to replace 'Builtin' EventSource with Nats.io EventStore. 

Extract Interfaces for EventSource and EventSink.

Implement Event Source  


## 0.1.0 - Original Base

The original code base develop by Tim - Form the basis of all the key abstractions for the library going
forward. Including 

* BSON <-> JSON Tools
* EventSources
* Binders
* Projections
* Commands and Queries


 
 