# JBoss EAP

- homepage: http://www.jboss.org/products/eap/overview/

## Usage
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
