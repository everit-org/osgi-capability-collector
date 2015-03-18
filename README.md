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

## CapabilityConsumer

The programmer, who wants to use the library, must implement the
CapabilityConsumer interface. This is a functional interface that catches
the events of the collector:

 - a new capability satisfies an unsatisfied requirement
 - a capability that satisfied a requirement before is removed. In this
   case the requirement becomes
   - unsatisfied
   - or remains satisfied as there is another capability that matches its
     filter

In case the requirement array is empty, the consumer will be notified

 - with satisfied flag true if the collector is opened
 - with satisfied flag false if the collector is closed

## Current implementations

There are two collector implementations in this library:

 - __ServiceReferenceCollector__: Tracks OSGi services
 - __BundleCapabilityCollector__: Tracks bundle capabilities

For more information, please see the javadoc of the CapabilityConsumer
interface.


## Updating requirements

It is possible to update the requirements of a collector even if its
opened. In that case the consumer will be called with the new suitings
(with the right satisfaction flag value).

