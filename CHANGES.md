conqueso-client-java Change History
===================================
This file documents the change history and release notes for the conqueso-client-java artifact.

### Release 0.5.1
February 11, 2014

* Add additional APIs to query the Conqueso server

#### 0.5.1 API Changes
* Added [ConquesoClient.getPropertyValue(String key)](https://github.com/rapid7/conqueso-client-java#querying-an-individual-property) - retrieve an individual property value
* Added RoleInfo object to model data of a Conqueso Role
* Added InstanceInfo object to model data of a Conqueso Instance
* Added [ConquesoClient.getRoles()](https://github.com/rapid7/conqueso-client-java#querying-roles) to retrieve info about all roles
* Added [ConquesoClient.getInstances()](https://github.com/rapid7/conqueso-client-java#querying-instances) to retrieve info about all instances
* Added [ConquesoClient.getRoleInstances(String roleName)](https://github.com/rapid7/conqueso-client-java#querying-instances-of-a-role) - retrieve info about instances of a particular role
* Added [ConquesoClient.getInstancesWithMetadata(String...metadataKeyValuePairs)](https://github.com/rapid7/conqueso-client-java#querying-instances-with-metadata) - retrieve info about instances matching metadata key-value query criteria
* Added [ConquesoClient.getRoleInstancesWithMetadata(String roleName, String...metadataKeyValuePairs)](https://github.com/rapid7/conqueso-client-java#querying-instances-of-a-role-with-metadata) - retrieve info about instances of a particular role matching metadata key-value query criteria
* Added ConquesoClient.parseConquesoDate(String conquesoDateValue) - parse a date value String returned from the Conqueso server to a Java Date


### Release 0.5.0
January 24, 2014

* Initial public release of conqueso-client-java

