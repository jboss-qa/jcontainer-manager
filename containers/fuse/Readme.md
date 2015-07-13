# JBoss Fuse

- homepage: http://www.jboss.org/products/fuse/overview/

## Usage
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
