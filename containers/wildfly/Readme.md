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

## Compatibility
- Wildfly 10 (EAP 7) + Java 8
	- *Default*
- Wildfly 9 + Java 7 / 8
	- Required properties: 
		- `version.wildfly=1.0.2.Final`
		- `version.surefire=2.18`
