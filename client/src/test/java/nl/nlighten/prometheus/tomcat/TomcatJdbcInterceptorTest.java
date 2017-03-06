package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.CollectorRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;


public class TomcatJdbcInterceptorTest extends AbstractTomcatMetricsTest {

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

         // global query stats
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_bucket", new String[]{"status", "le"}, new String[]{TomcatJdbcInterceptor.SUCCESS_QUERY_STATUS, "0.01"}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_bucket", new String[]{"status", "le"}, new String[]{TomcatJdbcInterceptor.SUCCESS_QUERY_STATUS, "+Inf"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_count", new String[]{"status"}, new String[]{TomcatJdbcInterceptor.SUCCESS_QUERY_STATUS}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_bucket", new String[]{"status", "le"}, new String[]{TomcatJdbcInterceptor.FAILED_QUERY_STATUS, "0.01"}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_bucket", new String[]{"status", "le"}, new String[]{TomcatJdbcInterceptor.FAILED_QUERY_STATUS, "+Inf"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_query_seconds_count", new String[]{"status"}, new String[]{TomcatJdbcInterceptor.FAILED_QUERY_STATUS}), is(greaterThan(0.0)));

        // slow query stats
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_slowquery_seconds_bucket", new String[]{"query", "le"}, new String[]{"select 1", "1.0"}), is(notNullValue()));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_slowquery_seconds_bucket", new String[]{"query", "le"}, new String[]{"select 1", "+Inf"}), is(greaterThan(0.0)));
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_slowquery_seconds_count", new String[]{"query"}, new String[]{"select 1"}), is(greaterThan(0.0)));

        // failed query stats
        assertThat(CollectorRegistry.defaultRegistry.getSampleValue("tomcat_jdbc_failedquery_total", new String[]{"query"}, new String[]{"select * from NON_EXISTING_TABLE"}), is(greaterThan(0.0)));

    }
}
