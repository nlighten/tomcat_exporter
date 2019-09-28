package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class TomcatJdbcPoolExportsTest extends AbstractTomcatMetricsTest {

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTomcat();
        new TomcatJdbcPoolExports().register();
        doRequest();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutDownTomcat();
    }

    @Test
    public void testJdbcPoolMetrics() throws Exception {
        String[] labels = new String[]{"pool", "context"};
        String[] labelValues = new String[]{"jdbc/db", CONTEXT_PATH};
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_max", labels, labelValues), is(100.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_active_total", labels, labelValues), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_idle_total", labels, labelValues), is(10.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_total", labels, labelValues), is(10.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_waitingthreads_total", labels, labelValues), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_borrowed_total", labels, labelValues), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_returned_total", labels, labelValues), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_created_total", labels, labelValues), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_released_total", labels, labelValues), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_reconnected_total", labels, labelValues), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_removeabandoned_total", labels, labelValues), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_releasedidle_total", labels, labelValues), is(0.0));
    }
}
