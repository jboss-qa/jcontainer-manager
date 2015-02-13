# JContainer manager
## Usage
### EAP
```java
final EapConfiguration conf = EapConfiguration.builder().directory($EAP_HOME).profile("standalone-full.xml").build();

try (Container cont = new EapContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute(":whoami");
	}
}
```
### JBoss Fuse
```java
final FuseConfiguration conf = FuseConfiguration.builder().directory($FUSE_HOME).maxPermSize("512m").build();

try (Container cont = new FuseContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute("osgi:info");
	}
}
```

## Tests

You will need to set some properties:

 - jboss.home
 - eap.home
 - karaf.home
 - fuse.home

You can set them by system property (`-Djboss.home` etc.) or create own copy of `test.properties_template` and set values:

    cp src/test/resources/test.properties_template src/test/resources/test.properties

### Running tests:

    mvn clean test -DskipTests=false
