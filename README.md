conqueso-client-java
====================
A library to connect your Java applications to [Conqueso](https://github.com/rapid7/conqueso). Everything's better... Conqueso.

Conqueso is a central repository for managing dynamic property values for the [Archaius](https://github.com/Netflix/archaius) library. 

### Motivation
The conqueso-client-java serves three purposes:

1. Gather and transmit instance metadata about your application to the Conqueso server
2. Gather and transmit info about what Archaius dynamic properties are used by your application to the Conqueso server
3. Provide an API to query the Conqueso server for property values and instance metadata

### License
The Conqueso server and the conqueso-client-java library are licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

### Getting conqueso-client-java
You can download the conqueso-client-java binaries from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cconqueso).

To add conqueso-client-java to your Maven project, use the following
```xml
<dependency>
  <groupId>com.rapid7.conqueso</groupId>
  <artifactId>conqueso-client-java</artifactId>
  <version>0.5.1</version>
</dependency>
```

### Usage

#### Configuring Archaius to use Conqueso
The primary purpose of Conqueso is to act as a central repository for managing dynamic property values for the [Archaius](https://github.com/Netflix/archaius) library. To make use of Conqueso, you should configure Archaius to refer to the Conqueso server as a [configuration source](https://github.com/Netflix/archaius/wiki/Getting-Started#using-multiple-urls-as-the-configuration-sources):
```
-Darchaius.configurationSource.additionalUrls=http://<myconquesoserver>/api/roles/<rolename>/properties
```
*&lt;rolename&gt;* is the definition of your Java application's role, which will be used to keep your distinct applications' properties separate and group instances of the same application together.

Another important Archaius configuration is the polling interval:
```
-Darchaius.fixedDelayPollingScheduler.delayMills=<millis>
```
This setting will determine how frequently your application checks for updates of property values with the Conqueso server. The default frequency if unconfigured is every minute (60000 milliseconds). The Conqueso server will also use this polling frequency to determine when your application instance is no longer checking in with the Conqueso server.

#### ConquesoClient
At some point during the startup of your Java application, code should be added to initialize an instance of the ConquesoClient.

The ConquesoClient Initializer supports many configuration options to customize how the connection, instance metadata
and Archaius dynamic properties used by your application are specified.

Here's an example with minimal configuration:
```java
ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
```

Most of the configuration options for the ConquesoClient are based on convention and reasonable defaults, with the option to customize as needed. Let's explore the options for each type of configuration.

#### Conqueso Server URL
By default, the URL to communicate with the Conqueso server is read from the Archaius additionalUrls system property:
```
-Darchaius.configurationSource.additionalUrls=http://<myconquesoserver>/api/roles/<rolename>/properties
```
You can override this default using the withConquesoUrl method:
```java
ConquesoClient.initializer()
   .withConquesoUrl("http://<anotherconquesoserver>/api/roles/<rolename>/properties")
   .withConfigurationClasses(AppConfig.class)
   .initialize();
```

#### Instance Metadata
On initialization of the ConquesoClient, instance metadata about your application instance is gathered and transmitted as part of the initial communication with the Conqueso server. This instance metadata is a simple map of String key/value pairs of data that uniquely identifies your instance and can be used to later query the Conqueso server based on metadata values.

By default, the instance metadata is produced by from reading EC2 metadata (if your application is running on AWS EC2) and system properties.

##### EC2 Metadata
If your application instance is running on AWS EC2, selected values from the Amazon EC2 Metadata are included in the instance metadata:
* ami-id
* instance-id
* instance-type
* local-hostname
* local-ipv4
* public-hostname
* public-ipv4
* availability-zone
* security-groups

See the [Amazon EC2 User Guide](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html) for more information about Amazon EC2 Metadata.

##### System Properties
Instance metadata key/values can be specified as command-line options for your application using Java system properties. All system properties with keys with the prefix *conqueso.metadata.* will be included in the instance metadata. For example, if your application is launched with:
```
-Dconqueso.metadata.app.name=reporting-service
```
the key/value pair *app.name=reporting-service* will be included in the instance metadata.

##### Programmatic Metadata
Instance metadata can be specified programmatically with the ConquesoClient Initializer. If you want to set all of the metadata values yourself, you can do the following:
```java
Map<String, String> myMetadata = new HashMap<>();
... populate myMetadata

ConquesoClient.initializer()
   .withInstanceData(myMetadata)
   .withConfigurationClasses(AppConfig.class)
   .initialize();
```
If you want to include the EC2 metadata and system properties values as described above you can do the following:
```java
Map<String, String> myMetadata = new HashMap<>();
... populate myMetadata

InstanceMetadataProvider provider = new CompositeInstanceMetadataProvider(
   ConquesoClient.Initializer.createDefaultInstanceDataProvider(),
   new CustomInstanceMetadataProvider(myMetadata));

ConquesoClient.initializer()
   .withInstanceData(provider)
   .withConfigurationClasses(AppConfig.class)
   .initialize();
```
##### Skipping Metadata
If you don't want metadata about your application instance to be transmitted to the Conqueso server, initialize with
the following:
```java
ConquesoClient.initializer()
   .skipReportingInstanceData()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
```

#### Archaius Dynamic Properties
On initialization of the ConquesoClient, information about the Archaius dynamic properties used by your application is gathered and transmitted as part of the initial communication with the Conqueso server. This is needed to pre-populate the Conqueso server with the property keys, property type and default property values for the application's role.

There are several configuration options available for detecting the properties.

##### Introspection of Specific Configuration Classes
You can tell the ConquesoClient about the classes within your application that contain the Archaius dynamic properties. 
```java
ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class, DbConfig.class, QueueConfig.class)
   .initialize();
```
These classes will be scanned for static field declarations of Archaius types. Any properties found in these classes
will be included in the set of properties sent.
```java
public class ExampleConfigClass {
    
    @ConquesoDescription("This is the string1 property")
    private static final DynamicStringProperty STRING1 = 
            DynamicPropertyFactory.getInstance().getStringProperty("string1", "foo");
    
    public static final DynamicIntProperty INT1 = 
            DynamicPropertyFactory.getInstance().getIntProperty("int1", 42);
    
    public static final DynamicStringListProperty STRING_LIST1 =
        new DynamicStringListProperty("stringList1", ImmutableList.of("foo", "bar", "baz"));
...
}
```
If you add the ```@ConquesoDescription``` annotation to your Archaius property fields the value will be used to describe the meaning of this property when viewing and editing it in the Conqueso server interface.

##### Scanning for Marker Annotations
You can tell the ConquesoClient to scan your application's classpath for classes containing marker annotations on the type. The marker annotation can be a custom type you specify, or the default `@ConquesoConfig` annotation. 
```java
ConquesoClient.initializer()
   .withConfigurationScan("com.myapp.package1", "com.myapp.package2")
   .initialize();
```
Or with a custom annotation type:
```java
ConquesoClient.initializer()
   .withConfigurationScan(Configuration.class, "com.myapp.package1", "com.myapp.package2")
   .initialize();
```

```java
@ConquesoConfig
public class ExampleConfigClass {
    
    @ConquesoDescription("This is the string1 property")
    private static final DynamicStringProperty STRING1 = 
            DynamicPropertyFactory.getInstance().getStringProperty("string1", "foo");
    
    public static final DynamicIntProperty INT1 = 
            DynamicPropertyFactory.getInstance().getIntProperty("int1", 42);
    
    public static final DynamicStringListProperty STRING_LIST1 = 
        new DynamicStringListProperty("stringList1", ImmutableList.of("foo", "bar", "baz"));
...
}
```

The classes discovered by this scan will be introspected to find the Archaius properties as described above.

##### JSON Property Definitions
Properties can also be defined in JSON files, specified using the *conqueso.properties.jsonUrls* system property. The system property value is a comma-separated list of URLS for retrieving the property definition JSON files.
```
-Dconqueso.properties.jsonUrls=file:/path/to/props1.json,http://path/to/props2.json
```
The format of the JSON file is as follows:
```json
[
    {
       "name":"exampleString",
       "type":"STRING",
       "value":"foo",
       "description":"This is the exampleString property"
    },
    {
       "name":"exampleStringList",
       "type":"STRING_LIST",
       "value":"foo,bar,baz"
    }
]
```
The values for the type field are defined in the [PropertyType](https://github.com/rapid7/conqueso-client-java/blob/master/src/main/java/com/rapid7/conqueso/client/PropertyType.java) enum in the conqueso-client-java artifact. The value field provides the default value for the property (but not necessarily the value returned by the Conqueso server if modified).
The value of the optional description field will be used to describe the meaning of the property when viewing and editing it in the Conqueso server interface.

##### Property Default Value Overrides
The default values of properties are read from the object declarations when introspecting configuration classes and from the JSON file definition as described above. These default values can also be overridden using external Java Properties files, specified by a system property:
```
-Dconqueso.properties.overridePropertiesUrls=file:/path/to/props1.properties,http://path/to/props2.properties
```
These properties files will be read and the values of any keys matching already defined properties will override the property's default value. This is useful for dynamically overriding property default values differently at development / test / production time.

#### Querying the Conqueso Server
The ConquesoClient instance returned by a successful initialization can be used to query the Conqueso server.
##### Querying Instance Properties
The complete set of the current properties value can be retrieved from the Conqueso server, external to the use by
any Archaius code.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
Properties props = client.getLatestProperties();
```
##### Querying an Individual Property
An individual property value can be queried for your application's role. All values retrieved by this method are returned as Strings.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
String myValue = client.getPropertyValue("myKey");
```
##### Querying Roles
Information about all the roles registered with the Conqueso server can be queried.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
List<RoleInfo> roles = client.getRoles();
for (RoleInfo role : roles) {
   System.out.println("Role: " + role.getName());
   System.out.println("Instance count: " + role.getInstances());
}
```
##### Querying Instances
Information about the active instances registered with the Conqueso server can be queried.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
List<InstanceInfo> instances = client.getInstances();
for (InstanceInfo instance : instances) {
   System.out.println("Instance role: " + instance.getRole());
   System.out.println("Instance IP: " + instance.getIpAddress());
   System.out.println("Metadata: " + instance.getMetadata());
}
```
##### Querying Instances of a Role
Information about the active instances of roles registered with the Conqueso server can be queried.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
List<InstanceInfo> instances = client.getRoleInstances("reporting-app");
for (InstanceInfo instance : instances) {
   System.out.println("Instance IP: " + instance.getIpAddress());
   System.out.println("Metadata: " + instance.getMetadata());
}
```
##### Querying Instances With Metadata
Information about active instances with metadata matching a series of key/value pairs can be queried.
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
List<InstanceInfo> instances = client.getInstancesWithMetadata("availabilty-zone", "us-east-1c", 
   "instance-type", "m1.small");
for (InstanceInfo instance : instances) {
   System.out.println("Instance role: " + instance.getRole());
   System.out.println("Instance IP: " + instance.getIpAddress());
   System.out.println("Metadata: " + instance.getMetadata());
}
```
The above code will display the information about all instances with instance metadata containing 
availability-zone=us-east-1c and instance-type=m1.small.

##### Querying Instances of a Role With Metadata
```java
ConquesoClient client = ConquesoClient.initializer()
   .withConfigurationClasses(AppConfig.class)
   .initialize();
   
List<InstanceInfo> instances = client.getRoleInstancesWithMetadata("reporting-app", "availabilty-zone", "us-east-1c", 
   "instance-type", "m1.small");
for (InstanceInfo instance : instances) {
   System.out.println("Instance IP: " + instance.getIpAddress());
   System.out.println("Metadata: " + instance.getMetadata());
}
```
The above code will display the information about reporting-app instances with instance metadata containing 
availability-zone=us-east-1c and instance-type=m1.small.

### Logging
conqueso-client-java uses SLF4J (http://www.slf4j.org/) for logging. SLF4J is a facade over logging that allows you to plug in any (or no) logging framework. See the SLF4J website for details.
