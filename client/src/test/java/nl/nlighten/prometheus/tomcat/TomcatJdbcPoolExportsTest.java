package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.CollectorRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
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
    public void testRequestProcessorMetrics() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_max", new String[]{"pool"}, new String[]{"jdbc/db"}), is(100.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_active_total", new String[]{"pool"}, new String[]{"jdbc/db"}), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_idle_total", new String[]{"pool"}, new String[]{"jdbc/db"}), is(10.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_connections_total", new String[]{"pool"}, new String[]{"jdbc/db"}), is(10.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_waitingthreads_total", new String[]{"pool"}, new String[]{"jdbc/db"}), is(0.0));
    }
}
