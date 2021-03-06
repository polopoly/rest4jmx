# About
The rest4jmx is a service that exposes a Java MBeanServer as a restful API with JSON or JSONP as data protocol. 

See `web/src/main/webapp/index.html` for API documentation. Or deploy the webapp and surf to the documentation and the JMX ajax GUI.

Build with `mvn install`.

## Developing and running

### Running with embedded Jetty

Move into the `web` directory and set `MAVEN_OPTS` before starting Jetty.
#### With Jrebel
`MAVEN_OPTS="-javaagent:$JREBEL_HOME/jrebel.jar -Dmyproject.root=$PWD/../core -Dcom.sun.management.jmxremote=true"  jetty:run`
#### Without jrebel
`MAVEN_OPTS="-Dcom.sun.management.jmxremote=true" mvn jetty:run`
#### With logging
`MAVEN_OPTS="-javaagent:$JREBEL_HOME/jrebel.jar -Dmyproject.root=$PWD/../core -Dcom.sun.management.jmxremote=true -Djava.util.logging.config.file=src/test/resources/logging.properties" mvn  -Djetty.port=8080  jetty:run`

Then point a browser to http://127.0.0.1:8080/


### Using exec:exec

Move into the `core` directory and execute `mvn exec:exec` you can then access the API using `GET 'http://localhost:9998/rest4jmx/domains'`

## Security
The webapp is secured by default. The embedded Jetty is configured to handle this.

Login as `guest:guest` to get *read* privilages.

Login as `admin:admin` to get *write* privilages.

Configuring for an external container either means:

* Adding a Realm that maps users to the two groups jmx_reader and jmx_writer
* Or comment out the security section in the web.xml

On Tomcat 6, one can for example, edit the conf/tomcat-users.xml and add:

```xml
    <role rolename="jmx_reader"/>
    <role rolename="jmx_writer"/>
    <user username="admin" password="admin" roles="jmx_writer,jmx_reader"/>
    <user username="guest" password="guest" roles="jmx_reader"/>
```

## Examples

Example of command line invocations when running the embedded Jetty

### Get all domains

`GET -C guest:guest 'http://localhost:8080/rest4jmx-web/mbeans/domains'`

###  List all MBeans for a domain

`GET -C guest:guest 'http://localhost:8080/rest4jmx-web/mbeans/domains/java.lang'`

###  List the properties of an MBean

`GET -C guest:guest 'http://localhost:8080/rest4jmx-web/mbeans/java.lang:type=Memory'`

###  Get an attribute

`GET -C guest:guest 'http://localhost:8080/rest4jmx-web/mbeans/java.lang:type=Memory/Verbose'`

###  Put an attribute value

`curl --basic --user admin:admin -v -i -H "Content-type: text/plain" -X PUT --data-binary "false" 'http://localhost:8080/rest4jmx-web/mbeans/java.lang:type=Memory/Verbose'`

###  Invoke a method, possibly with parameters

`curl -r admin:admin -v -i -H "Content-type: application/json" -X POST --data-binary "{params: ['1']}" 'http://localhost:8080/rest4jmx-web/mbeans/java.lang:type=Threading/ops/getThreadAllocatedBytes'`

### `mvn exec:exec`

`curl -v -i -H "Content-type: application/json" -X POST --data-binary "{params: ['1','2']}" 'http://localhost:9998/rest4jmx/testdomain:name=testBean/ops/methodWithParams'`

## Tomcat 6

Tomcat 6 does not handle encoded "/" as part of URL well by default. Since Tomcat itself uses slashes in their MBean names. Setting the propery `JAVA_OPTS="-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true"` in `catalina.sh` solves this.