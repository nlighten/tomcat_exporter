# Prometheus Tomcat Client
A set of collectors that can be used to monitor Tomcat instances.


### Available metrics
The following Tomcat related metrics are provided:

* Thread pool metrics
* Session metrics
* Request processor metrics
* Database connection pool metrics
* Tomcat version info
* Servlet response time metrics 
* Database response time metrics

### Using
If you are running Tomcat in the conventional non-embedded setup the recommended way is to add the following jars to dependencies (see `pom.xml`) to the `$CATALINA_BASE/lib` directory or another directory on the `common.loader` path.
Using the `common.loader` is important as we need to make sure that all metrics are registered using the same class loader.

* simpleclient
* simpleclient_common
* simpleclient_servlet
* simpleclient_hotspot
* simpleclient_tomcat

Next, create a servlet with the exporters you want, package it as `metrics.war` and add it to the webapps directory. Typically you will extend the MetricsServlet and register the hotspot metrics, the generic tomcat metrics and metrics for the database connection pool.

The following example assumes that you are using the [Tomcat JDBC Pool](http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html). If you are using DBCP2 pool you should change the exporter to TomcatDbcp2PoolExports class.

```java
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import javax.servlet.ServletConfig;

@WebServlet("/")
public class TomcatMetricsServlet extends MetricsServlet {

    @Override
    public void init(ServletConfig config) {
        DefaultExports.initialize();
        new TomcatGenericExports(false).register();
        new TomcatJdbcPoolExports().register();
    }
}
```

> NOTE: Make sure that the `metrics.war` does not contain any of the jars mentioned above as they need to be on Tomcats `common.loader` classpath.

### Servlet response time metrics
If you want servlet response time metrics you can configure the `TomcatServletMetricsFilter` by adding it to the $CATALINA_BASE/lib/web.xml as shown below. There is no need to modify already deployed applications.

```xml
<filter>
  <filter-name>ServletMetricsFilter</filter-name>
  <filter-class>TomcatServletMetricsFilter</filter-class>
  <async-supported>true</async-supported>
  <init-param>
    <param-name>buckets</param-name>
    <param-value>.01, .05, .1, .25, .5, 1, 2.5, 5, 10, 30</param-value>
  </init-param>
</filter>

<filter-mapping>
  <filter-name>ServletMetricsFilter</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```


### Database response time metrics
Database response time metrics are only available when using the [Tomcat JDBC Pool](http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html) as this collector uses an interceptor mechanism that is only available for this type of pool.

The interceptor will collect the following metrics:

* A histogram with global query response times
* A histogram with per query response times for slow queries (optional)
* A gauge with per query error counts (optional) 

Configuration is usually done in Tomcat's `server.xml` or `context.xml`

```xml
<Resource name="jdbc/TestDB"
           auth="Container"
           type="javax.sql.DataSource"
           factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
           jdbcInterceptors="TomcatJdbcInterceptor(logFailed=true,logSlow=true,threshold=1000,buckets=.01|.05|.1|1|10,slowQueryBuckets=1|10|30)"
           username="root"
           password="password"
           driverClassName="com.mysql.jdbc.Driver"
           url="jdbc:mysql://localhost:3306/mysql"/>
```

Configuration options of the interceptor are as shown above and have the following meaning:
- logFailed: if set to 'true' collect metrics on failed queries
- logSlow: if set to 'true' collect metrics on metrics exceeding threshold
- threshold: the threshold in ms above which metrics will be collected if logSlow=true
- buckets: the buckets separated by a pipe ("|") symbol to be used for the global query response times, defaults to .01|.05|.1|.25|.5|1|2.5|10
- slowQueryBuckets: the buckets separated by a pipe ("|") symbol to be used for the global query response times, defaults to 1|2.5|10|30

> NOTE: enabling logFailed and logSlow may lead to a lot of additional metrics., so be careful !!!  
 

### Embedded mode
If you run Tomcat in embedded mode, please look at the `AbstractTomcatMetricsTest` for an example on how to configure the various exporters when running embedded.

### Javadocs
There are canonical examples defined in the class definition Javadoc of the client packages.

Documentation can be found at the [Java Client
Github Project Page](http://prometheus.github.io/client_java/simpleclient_tomcat).

