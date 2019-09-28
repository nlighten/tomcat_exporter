package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Exports Tomcat <a href="https://tomcat.apache.org/tomcat-8.5-doc/jndi-datasource-examples-howto.html#Database_Connection_Pool_(DBCP_2)_Configurations">DBCP2-pool</a> metrics.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new TomcatDbcp2PoolExports().register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *    tomcat_dpcp2_connections_max{pool="jdbc/mypool"} 20.0
 *    tomcat_dbcp2_connections_active_total{pool="jdbc/mypool"} 2.0
 *    tomcat_dbcp2_connections_idle_total{pool="jdbc/mypool"} 6.0
 * </pre>
 */

public class TomcatDbcp2PoolExports extends Collector {

    private static final Log log = LogFactory.getLog(TomcatDbcp2PoolExports.class);

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("Tomcat:class=javax.sql.DataSource,type=DataSource,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelList = Arrays.asList("pool", "context");

                GaugeMetricFamily maxActiveConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_dbcp2_connections_max",
                        "Maximum number of active connections that can be allocated from this pool at the same time",
                        labelList);

                GaugeMetricFamily activeConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_dbcp2_connections_active_total",
                        "Number of active connections allocated from this pool",
                        labelList);

                GaugeMetricFamily idleConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_dbcp2_connections_idle_total",
                        "Number of idle connections in this pool",
                        labelList);

                String[] poolAttributes = new String[]{"maxTotal", "numActive", "numIdle"};


                for (final ObjectInstance mBean : mBeans) {
                    if (mBean.getObjectName().getKeyProperty("connectionpool") == null) {
                        List<String> labelValueList = Arrays.asList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""), mBean.getObjectName().getKeyProperty("context"));
                        if (mBean.getObjectName().getKeyProperty("connections") == null) {  // Tomcat 8.5.33 ignore PooledConnections
                            AttributeList attributeList = server.getAttributes(mBean.getObjectName(), poolAttributes);

                            for (Attribute attribute : attributeList.asList()) {
                                switch (attribute.getName()) {
                                    case "maxTotal":
                                        maxActiveConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                        mfs.add(maxActiveConnectionsGauge);
                                        break;
                                    case "numActive":
                                        activeConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                        mfs.add(activeConnectionsGauge);
                                        break;
                                    case "numIdle":
                                        idleConnectionsGauge.addMetric(labelValueList, ((Integer) attribute.getValue()).doubleValue());
                                        mfs.add(idleConnectionsGauge);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Error retrieving metric:" + e.getMessage());
        }
        return mfs;
    }

    public static boolean isDbcp2Used() {
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
