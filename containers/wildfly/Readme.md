# Wildfly

- homepage: http://wildfly.org/

## Usage
```java
final WildflyConfiguration conf = WildflyConfiguration.builder().directory($WILDFLY_HOME).profile("standalone-full.xml").xmx("2g").build();

try (Container cont = new WildflyContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute(":whoami");
	}
}
```
Standalone client:
```java
try (Client client = new WildflyClient<>(WildflyConfiguration.builder().build())) {
    client.execute(":whoami");
}
```