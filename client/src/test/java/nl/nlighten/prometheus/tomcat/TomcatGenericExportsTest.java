package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.CollectorRegistry;
import org.apache.catalina.util.ServerInfo;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;


public class TomcatGenericExportsTest extends AbstractTomcatMetricsTest {

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTomcat();
        new TomcatGenericExports(true).register();
        doRequest();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutDownTomcat();
    }

    @Test
    public void testTomcatInfo() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_info", new String[]{"version", "build"}, new String[]{ServerInfo.getServerNumber(), ServerInfo.getServerBuilt()}), is(1.0));
    }

    @Test
    public void testRequestProcessorMetrics() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_requestprocessor_received_bytes", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_requestprocessor_sent_bytes", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_requestprocessor_time_seconds", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_requestprocessor_error_count_total", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_requestprocessor_request_count_total", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(1.0));
    }

    @Test
    public void testSessionMetrics() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_active_total", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_rejected_total", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_created_total", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_expired_total", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_alivetime_seconds_avg", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_session_alivetime_seconds_max", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(notNullValue()));
    }


    @Test
    public void testContextStateMetric() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_context_state_started", new String[]{"host", "context"}, new String[]{"localhost", CONTEXT_PATH}), is(1.0));
    }

    @Test
    public void testThreadPoolMetrics() throws Exception {
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_threads_total", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_threads_active_total", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(0.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_threads_max", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(200.0));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_connections_active_total", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_connections_active_max", new String[]{"name"}, new String[]{"http-nio-auto-1"}), is(8192.0));
    }
}
