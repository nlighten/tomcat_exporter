package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Exports Tomcat <a href="http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html">jdbc-pool</a> metrics.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new TomcatJdbcPoolExports().register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *    tomcat_jdbc_connections_max{context="/foo",pool="jdbc/mypool"} 20.0
 *    tomcat_jdbc_connections_active_total{context="/foo",pool="jdbc/mypool"} 2.0
 *    tomcat_jdbc_connections_idle_total{context="/foo",pool="jdbc/mypool"} 6.0
 *    tomcat_jdbc_connections_total{context="/foo",pool="jdbc/mypool"} 8.0
 *    tomcat_jdbc_connections_threadswaiting_total{context="/foo",pool="jdbc/mypool"} 0.0
 * </pre> * </pre>
 */

public class TomcatJdbcPoolExports extends Collector {

    private static final Log log = LogFactory.getLog(TomcatJdbcPoolExports.class);

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("tomcat.jdbc:class=org.apache.tomcat.jdbc.pool.DataSource,type=ConnectionPool,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelList = Arrays.asList("pool", "context");

                GaugeMetricFamily maxActiveConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_max",
                        "Maximum number of active connections that can be allocated from this pool at the same time",
                        labelList);

                GaugeMetricFamily activeConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_active_total",
                        "Number of active connections allocated from this pool",
                        labelList);

                GaugeMetricFamily idleConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_idle_total",
                        "Number of idle connections in this pool",
                        labelList);

                GaugeMetricFamily totalConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_total",
                        "Total number of connections in this pool",
                        labelList);

                GaugeMetricFamily waitingThreadsCountGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_waitingthreads_total",
                        "Number of threads waiting for connections from this pool",
                        labelList);

                GaugeMetricFamily borrowedConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_borrowed_total",
                        "Number of connections borrowed from this pool",
                        labelList);

                GaugeMetricFamily returnedConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_returned_total",
                        "Number of connections returned to this pool",
                        labelList);

                GaugeMetricFamily createdConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_created_total",
                        "Number of connections created by this pool",
                        labelList);
                GaugeMetricFamily releasedConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_released_total",
                        "Number of connections released by this pool",
                        labelList);

                GaugeMetricFamily reconnectedConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_reconnected_total",
                        "Number of reconnected connections by this pool",
                        labelList);

                GaugeMetricFamily removeAbandonedConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_removeabandoned_total",
                        "Number of abandoned connections that have been removed",
                        labelList);

                GaugeMetricFamily releasedIdleConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_releasedidle_total",
                        "Number of idle connections that have been released",
                        labelList);

                String[] poolAttributes = new String[]{"MaxActive", "Active", "Idle", "Size", "WaitCount", "BorrowedCount", "ReturnedCount", "CreatedCount", "ReleasedCount", "ReconnectedCount", "RemoveAbandonedCount", "ReleasedIdleCount"};

                for (final ObjectInstance mBean : mBeans) {
                    List<String> labelValueList = Arrays.asList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""), Optional.ofNullable(mBean.getObjectName().getKeyProperty("context")).orElse("global"));
                    if (mBean.getObjectName().getKeyProperty("connections") == null) {  // Tomcat 8.5.33 ignore PooledConnections
                        AttributeList attributeList = server.getAttributes(mBean.getObjectName(), poolAttributes);

                        for (Attribute attribute : attributeList.asList()) {
                            switch (attribute.getName()) {
                                case "MaxActive":
                                    maxActiveConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                    break;
                                case "Active":
                                    activeConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                    break;
                                case "Idle":
                                    idleConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                    break;
                                case "Size":
                                    totalConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                    break;
                                case "WaitCount":
                                    waitingThreadsCountGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                    break;
                                case "BorrowedCount":
                                    borrowedConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "ReturnedCount":
                                    returnedConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "CreatedCount":
                                    createdConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "ReleasedCount":
                                    releasedConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "ReconnectedCount":
                                    reconnectedConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "RemoveAbandonedCount":
                                    removeAbandonedConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                                    break;
                                case "ReleasedIdleCount":
                                    releasedIdleConnectionsGauge.addMetric(labelValueList, ((Long) attribute.getValue()).doubleValue());
                            }
                        }
                    }
                }
                mfs.add(maxActiveConnectionsGauge);
                mfs.add(activeConnectionsGauge);
                mfs.add(idleConnectionsGauge);
                mfs.add(totalConnectionsGauge);
                mfs.add(waitingThreadsCountGauge);
                mfs.add(borrowedConnectionsGauge);
                mfs.add(returnedConnectionsGauge);
                mfs.add(createdConnectionsGauge);
                mfs.add(releasedConnectionsGauge);
                mfs.add(reconnectedConnectionsGauge);
                mfs.add(removeAbandonedConnectionsGauge);
                mfs.add(releasedIdleConnectionsGauge);
            }
        } catch (Exception e) {
            log.error("Error retrieving metric:" + e.getMessage());
        }
        return mfs;
    }

    public static boolean isTomcatJdbcUsed() {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("tomcat.jdbc:class=org.apache.tomcat.jdbc.pool.DataSource,type=ConnectionPool,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);
            return !mBeans.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
