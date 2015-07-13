# Apache Karaf

- homepage: http://karaf.apache.org/

## Usage
```java
final KarafConfiguration conf = KarafConfiguration.builder().directory($KARAF_HOME).xmx("2g").build();

try (Container cont = new KarafContainer<>(conf)) {
	cont.start()
	try(Client cli = container.getClient()) {
		cli.execute("info");
	}
}
```
Standalone client:
```java
try (Client client = new KarafClient<>(KarafConfiguration.builder().build())) {
	client.execute("version");
}
```
