# JContainer manager
## Usage
### EAP
```java
final EapConfiguration conf = EapConfiguration.builder().directory($EAP_HOME).profile("standalone-full.xml").xmx("2g").build();

try (Container cont = new EapContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute(":whoami");
	}
}
```
Standalone client:
```java
try (Client client = new EapClient<>(EapConfiguration.builder().build())) {
    client.execute(":whoami");
}
```
### JBoss Fuse
```java
final FuseConfiguration conf = FuseConfiguration.builder().directory($FUSE_HOME).xmx("2g").build();

try (Container cont = new FuseContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute("osgi:info");
	}
}
```
Standalone client:
```java
try (Client client = new FuseClient<>(FuseConfiguration.builder().build())) {
	client.execute("osgi:version");
}
```

### Tomcat
```java
final TomcatConfiguration conf = TomcatConfiguration.builder().directory($TOMCAT_HOME).xmx("512m").build();

try (Container cont = new TomcatContainer<>(conf)) {
	cont.start()
}
```

## Tests

You will need to set some properties:

 - jboss.home
 - eap.home
 - karaf.home
 - fuse.home
 - tomcat.home

You can set them by system property (`-Djboss.home` etc.) or create own copy of `test.properties_template` and set values:

    cp src/test/resources/test.properties_template src/test/resources/test.properties

### Running tests:

    mvn clean test -DskipTests=false

## Contribution:

Before you submit a pull request, please ensure that:

 * New issue is created for your task.
 * `mvn checkstyle:check` is passing.
 * There are no blank spaces.
 * There is new line at end of each file.
 * Commit messages start with '**Issue #ID**', are in the imperative and well formed.
     * See: [A Note About Git Commit Messages](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)
