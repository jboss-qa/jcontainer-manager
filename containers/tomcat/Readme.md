# Apache Tomcat

- homepage: http://tomcat.apache.org/

## Usage
```java
final TomcatConfiguration conf = TomcatConfiguration.builder().directory($TOMCAT_HOME).xmx("512m").build();

try (Container cont = new TomcatContainer<>(conf)) {
	cont.start()
}
```