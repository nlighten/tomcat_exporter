package nl.nlighten.prometheus.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.ServletException;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractTomcatMetricsTest {

    private static Tomcat tomcat;
    final static String CONTEXT_PATH = "/foo";
    final static String SERVLET_NAME = "foo_servlet";


    public static void setUpTomcat() throws LifecycleException, ServletException {
        setUpTomcat("org.apache.tomcat.jdbc.pool.DataSourceFactory");
    }

    public static void setUpTomcat(String dataSourceFactory) throws LifecycleException, ServletException {
        // create a tomcat instance
        tomcat = new Tomcat();
        tomcat.setBaseDir(".");
        tomcat.setPort(0);
        tomcat.enableNaming();

        // create a context with our test servlet
        Context ctx = tomcat.addContext(CONTEXT_PATH, new File(".").getAbsolutePath());
        Tomcat.addServlet(ctx, SERVLET_NAME, new TestServlet());
        ctx.addServletMappingDecoded("/*", SERVLET_NAME);

        // add our metrics filter
        FilterDef def = new FilterDef();
        def.setFilterClass(TomcatServletMetricsFilter.class.getName());
        def.setFilterName("metricsFilter");
        def.addInitParameter("buckets",".01, .05, .1, .25, .5, 1, 2.5, 5, 10, 30");
        ctx.addFilterDef(def);
        FilterMap map = new FilterMap();
        map.setFilterName("metricsFilter");
        map.addURLPattern("/*");
        ctx.addFilterMap(map);

        // create a datasource
        ContextResource resource = new ContextResource();
        resource.setName("jdbc/db");
        resource.setAuth("Container");
        resource.setType("javax.sql.DataSource");
        resource.setScope("Sharable");
        resource.setProperty("name", "foo");
        resource.setProperty("factory", dataSourceFactory);
        resource.setProperty("driverClassName", "org.h2.Driver");
        resource.setProperty("url", "jdbc:h2:mem:dummy");
        resource.setProperty("jdbcInterceptors", "nl.nlighten.prometheus.tomcat.TomcatJdbcInterceptor(logFailed=true,logSlow=true,threshold=0,buckets=.01|.05|.1|1|10,slowQueryBuckets=1|10|30)");
        ctx.getNamingResources().addResource(resource);

        // start instance
        tomcat.init();
        tomcat.start();
    }

    public static void shutDownTomcat() throws LifecycleException {
        if (tomcat.getServer() != null
                && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
            if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
                tomcat.stop();
            }
            tomcat.destroy();
        }
    }

    public static void doRequest() {
        // send GET request
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://localhost:" + tomcat.getConnector().getLocalPort() + CONTEXT_PATH + "/bar").openConnection();
            urlConnection.getInputStream().close();
            urlConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

