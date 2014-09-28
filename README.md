osgi-capability-collector
=========================

Capability Collector helps tracking capabilities that appear in an OSGi
container. Capabilities can be OSGi services, Bundle capabilities or any
other objects that can be tracked based on events.

## Requirements

Capability Collectors tracks capabilities for the specified requirements.
Each requirement has the following properties:

 - __requirementId__: The id of the requirement as a String. There cannot
   be two requirements with the same id in one CapabilityCollector at the
   same time.
 - __filter__: An OSGi filter expression to tell which OSGi capabilities
   are accepted for the requirement.
 - __attributes__: Two requirements are identical only if their id, filter
   and attribute map are identical. Any String-Object pairs can be stored
   in the map of attributes, however it is highly advised to use only
   immutable class as the value of the map.

If the filter allows, the same OSGi capability can be attached to multiple
requirements.

## ActionHandler

CapabilityCollector communicates via an ActionHandler. The handler instance
must be provided during the instantiation of CapabilityCollector.

For more information, please see the javadoc of the ActionHandler interface.

## Modes

### Standard mode

In case a capability is removed but there is another one available for a
specific requirement, the following methods will be called on ActionHandler:

 - __unsatisfied__
 - __unbind__: The collector unbinds the old capability from the requirement
 - __bind__: The collector binds the new capability to the requirement
 - __satisfied__
 
### Dynamic mode

In case a capability is removed but there is another one available for a
specific requirement, the following methods will be called on ActionHandler:

 - __bind__: The collector binds the new capability to the requirement. In
   this case the collector remains satisfied (if it was satisfied before)
   and the _unbind_ method is not called at all. Unbind is called only if
   there is no capability that satisfies the requirement anymore. 

## Updating items

It is possible to update the requirements of a collector even if its
opened. This function has two different behaviors based on the mode of
the collector.

### Updating a standard tracker

In case of any change, the Collector calls _unsatisfied_ on the
_actionHandler_ before processing the new requirements. If all of the
requirements are satisfied in the end, the _satisfied_ method of
_ActionHandler_ will be called.

### Updating a dinamyc tracker

The Collector is _satisfied_ until it processes a requirement from the newly
specified requirement list that cannot be satisfied.

[![Analytics](https://ga-beacon.appspot.com/UA-15041869-4/everit-org/osgi-capability-collector)](https://github.com/igrigorik/ga-beacon)
