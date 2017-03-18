package nl.nlighten.prometheus.tomcat;

import java.util.Map;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolProperties.InterceptorProperty;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.interceptor.AbstractQueryReport;

/**
 * A Tomcat <a href="http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html#JDBC_interceptors">JDBC interceptor</a> that tracks query statistics for
 * applications using the Tomcat <a href="http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html">jdbc-pool</a>. This interceptor will NOT work for
 * any other connection pool (eg DBCP2).
 *
 * The interceptor will create the following metrics:
 *
 * - A histogram with global query response times
 * - A histogram with per query response times for slow queries (optional)
 * - A gauge with per query error counts (optional)
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * <Resource name="jdbc/TestDB"
 *           auth="Container"
 *           type="javax.sql.DataSource"
 *           factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
 *           jdbcInterceptors="nl.nlighten.prometheus.TomcatJdbcInterceptor(logFailed=true,logSlow=true,threshold=1000,buckets=.01|.05,|.1|1|10,slowQueryBuckets=1|10|30)"
 *           username="root"
 *           password="password"
 *           driverClassName="com.mysql.jdbc.Driver"
 *           url="jdbc:mysql://localhost:3306/mysql"/>
 * }
 * </pre>
 *
 * Configuration options are as shown above an have the following meaning:
 * - logFailed: if set to 'true' provide metrics on failed queries
 * - logSlow: if set to 'true' provide metrics on metrics exceeding threshold
 * - threshold: the threshold in ms above which metrics will be provided if logSlow=true
 * - buckets: the buckets separated by a pipe ("|") symbol to be used for the global query response times, defaults to .01|.05|.1|.25|.5|1|2.5|10
 * - slowQueryBuckets: the buckets separated by a pipe ("|") symbol to be used for the global query response times, defaults to 1|2.5|10|30
 *
 * NOTE: enabling logFailed and logSlow may lead to a lot of additional metrics., so be careful !!!
 *
 * Example metrics being exported:
 * <pre>
 *    tomcat_jdbc_query_seconds_bucket{le="0.005",} 48950.0
 *    .....
 *    tomcat_jdbc_query_seconds_bucket{le="+Inf",} 301.0
 *    tomcat_jdbc_query_seconds_count 353501.0
 *    tomcat_jdbc_query_seconds_sum 331875.0
 *    tomcat_jdbc_slowquery_seconds{query="SELECT 1 from DUAL", }
 * </pre>
 */
public class TomcatJdbcInterceptor extends AbstractQueryReport {

    private static Histogram globalQueryStats;
    private static Histogram slowQueryStats;
    private static Gauge failedQueryStats;
    private boolean slowQueryStatsEnabled;
    private boolean failedQueryStatsEnabled;
    private long slowQueryThreshold = 1000;

    public final static String SUCCESS_QUERY_STATUS = "success";
    public final static String FAILED_QUERY_STATUS = "error";



    @Override
    public void setProperties(Map<String, InterceptorProperty> properties) {
      //  super.setProperties(properties);

        InterceptorProperty bucketsProperty = properties.get("buckets");
        double[] buckets;
        if (bucketsProperty != null) {
            String[] bucketParams = bucketsProperty.getValue().split("\\|");
            buckets = new double[bucketParams.length];
            for (int i = 0; i < bucketParams.length; i++) {
                buckets[i] = Double.parseDouble(bucketParams[i]);
            }
        } else {
            buckets = new double[] {.01, .05, .1, .25, .5, 1, 2.5, 10};
        }

        if (globalQueryStats == null) {
            Histogram.Builder builder = Histogram.build()
                    .help("JDBC query duration")
                    .name("tomcat_jdbc_query_seconds")
                    .buckets(buckets)
                    .labelNames("status");
            globalQueryStats = builder.register();
        }

        InterceptorProperty slowQueryBucketsProperty = properties.get("slowQueryBuckets");
        double[] slowQueryBuckets;
        if (slowQueryBucketsProperty != null) {
            String[] bucketParams = slowQueryBucketsProperty.getValue().split("\\|");
            slowQueryBuckets = new double[bucketParams.length];
            for (int i = 0; i < bucketParams.length; i++) {
                slowQueryBuckets[i] = Double.parseDouble(bucketParams[i]);
            }
        } else {
            slowQueryBuckets = new double[] { 1, 2.5, 10, 30};
        }

        InterceptorProperty slowQueryStatsProperty = properties.get("logSlow");
        if (slowQueryStatsProperty != null && slowQueryStatsProperty.getValue().equals("true")) {
            slowQueryStatsEnabled = true;
            if (slowQueryStats == null) {
                Histogram.Builder builder = Histogram.build()
                        .help("JDBC slow query duration in seconds")
                        .name("tomcat_jdbc_slowquery_seconds")
                        .buckets(slowQueryBuckets)
                        .labelNames("query");
                slowQueryStats = builder.register();
            }
        }

        InterceptorProperty slowQueryThresholdProperty = properties.get("threshold");
        if (slowQueryThresholdProperty != null) {
            slowQueryThreshold = Long.parseLong(slowQueryThresholdProperty.getValue());
        }

        InterceptorProperty failedQueryStatsProperty = properties.get("logFailed");
        if (failedQueryStatsProperty != null && failedQueryStatsProperty.getValue().equals("true")) {
            failedQueryStatsEnabled = true;
            if (failedQueryStats == null) {
                Gauge.Builder builder = Gauge.build()
                        .help("Number of errors for give JDBC query")
                        .name("tomcat_jdbc_failedquery_total")
                        .labelNames("query");
                failedQueryStats = builder.register();
            }
        }
    }

    @Override
    protected String reportFailedQuery(String query, Object[] args, String name, long start, Throwable t) {
        String sql = super.reportFailedQuery(query, args, name, start, t);
        long now = System.currentTimeMillis();
        long delta = now - start;
        globalQueryStats.labels(FAILED_QUERY_STATUS).observe(delta/1000);
        if (failedQueryStatsEnabled) {
            failedQueryStats.labels(sql).inc();
        }
        return sql;
    }

    @Override
    protected String reportQuery(String query, Object[] args, final String name, long start, long delta) {
        String sql = super.reportQuery(query, args, name, start, delta);
        globalQueryStats.labels(SUCCESS_QUERY_STATUS).observe(delta/1000);
        if (slowQueryStatsEnabled && delta >= slowQueryThreshold) {
            slowQueryStats.labels(sql).observe(delta/1000);
        }
        return sql;
    }

    @Override
    protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
        String sql = super.reportSlowQuery(query, args, name, start, delta);
        globalQueryStats.labels(SUCCESS_QUERY_STATUS).observe(delta/1000);
        if (slowQueryStatsEnabled && delta >= slowQueryThreshold) {
            slowQueryStats.labels(sql).observe(delta/1000);
        }
        return sql;
    }

    @Override
    public void closeInvoked() {
        // NOOP
    }

    @Override
    public void prepareStatement(String sql, long time) {
        // NOOP
    }

    @Override
    public void prepareCall(String sql, long time) {
        // NOOP
    }

    @Override
    public void poolStarted(ConnectionPool pool) {
        super.poolStarted(pool);
    }

    @Override
    public void poolClosed(ConnectionPool pool) {
        super.poolClosed(pool);
    }

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        super.reset(parent, con);
    }
}
