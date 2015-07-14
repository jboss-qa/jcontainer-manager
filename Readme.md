# JContainer manager
## Usage
- [Wildfly](containers/wildfly/Readme.md)
- [JBoss EAP](containers/eap/Readme.md)
- [Apache Karaf](containers/karaf/Readme.md)
- [JBoss Fuse](containers/fuse/Readme.md)
- [Apache Tomcat](containers/tomcat/Readme.md)


## Tests

You will need to set home directories of your containers. You can set them by system property (`-Dwildfly.home` etc.) 
or create own copy of `test.properties_template` and set values:

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
