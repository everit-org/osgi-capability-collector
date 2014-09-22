osgi-reference-tracker
======================

ReferenceTracker helps tracking OSGi services for zero or more items. The
aim of this library to add simplify the tracking functionality in Everit
Component Registry or in any technology that wants to build OSGi components.

## Items

Reference tracker tracks services for its items. Each item has the following
properties:

 - __itemId__: The id of the item as a String. There cannot be two items
   with the same ReferenceTracker at the same time.
 - __filter__: An OSGi filter to tell which OSGi services are accepted for
   the item.
 - __attributes__: Two items are identical only if their id, filter and
   attributes map are identical. Any String-Object pairs can be stored
   in the map of attributes, however it is highly advised to use only
   immutable classes as the value of the map.

If the filter allows, the same OSGi service can be attached to multiple
items.

## ActionHandler

ReferenceTracker communicates via an ActionHandler. The handler instance
must be provided during calling the constructor of ReferenceTracker.

See the javadoc of the ReferenceActionHandler interface.

## Modes

### Standard mode

In case an OSGi service is removed but there is another available for a
specific item, the following methods will be called on ActionHandler:

 - __unsatisfied__
 - __unbind__: The tracker unbinds the old service from the item
 - __bind__: The tracker binds the new service for the item
 - __satisfied__
 
### Dynamic mode

In case an OSGi service is removed but there is another available for a
specific item, the following methods will be called on ActionHandler:

 - __bind__: The reference binds the new service to the item. In this case
   the reference remains satisfied (if it was satisfied before) and there
   is no unbind with the old service. Unbind is called only if there is no
   other service available. 

## Updating items

It is possible to update the items of a reference tracker even if its
opened. This function has two different behaviors based on the mode of
the tracker.

### Updating a standard tracker

In case of any change, the Tracker calls _unsatisfied_ on the _actionHandler_
before processing the new items. If all of the items are satisfied in the
end, the _satisfied_ method of _actionHandler_ be called.

### Updating a dinamyc tracker

The tracker is _satisfied_ until it processes an item from the new item list
that cannot be satisfied.

[![Analytics](https://ga-beacon.appspot.com/UA-15041869-4/everit-org/osgi-reference-tracker)](https://github.com/igrigorik/ga-beacon)
