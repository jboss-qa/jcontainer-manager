# Jboss Data Grid

- homepage: https://www.redhat.com/it/technologies/jboss-middleware/data-grid

## Usage
```java
final JdgConfiguration conf = JdgConfiguration.builder().httpPort(11222).build();

try (Container cont = new JdgContainer<>(conf)) {
	cont.start()
}
```

## Compatibility
- Jdg 8+
