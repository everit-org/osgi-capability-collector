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

TODO...

For more information, please see the javadoc of the CapabilityConsumer
interface.


## Updating requirements

It is possible to update the requirements of a collector even if its
opened.


[![Analytics](https://ga-beacon.appspot.com/UA-15041869-4/everit-org/osgi-capability-collector)](https://github.com/igrigorik/ga-beacon)
