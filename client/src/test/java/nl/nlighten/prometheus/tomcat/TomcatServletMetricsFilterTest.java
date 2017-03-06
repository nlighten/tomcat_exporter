package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.CollectorRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;


public class TomcatServletMetricsFilterTest extends AbstractTomcatMetricsTest {

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTomcat();
        doRequest();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutDownTomcat();
    }

    @Test
    public void testServletRequestMetrics() throws Exception {
        // servlet response times
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("servlet_request_seconds_bucket", new String[]{"context", "method", "le"}, new String[]{CONTEXT_PATH, "GET", "0.01"}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("servlet_request_seconds_bucket", new String[]{"context", "method", "le"}, new String[]{CONTEXT_PATH, "GET", "+Inf"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("servlet_request_seconds_count", new String[]{"context", "method"}, new String[]{CONTEXT_PATH, "GET"}), is(greaterThan(0.0)));

        // concurrent invocation count
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("servlet_request_concurrent_total", new String[]{"context"}, new String[]{CONTEXT_PATH}), is(notNullValue()));
    }
}
