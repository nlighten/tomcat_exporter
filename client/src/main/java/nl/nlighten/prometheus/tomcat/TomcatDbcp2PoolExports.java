package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("Tomcat:class=javax.sql.DataSource,type=DataSource,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelList = Collections.singletonList("pool");

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


                for (final ObjectInstance mBean : mBeans) {
                    if (mBean.getObjectName().getKeyProperty("connectionpool") == null){
                        List<String> labelValueList = Collections.singletonList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""));

                        maxActiveConnectionsGauge.addMetric(
                                labelValueList,
                                ((Integer) server.getAttribute(mBean.getObjectName(), "maxTotal")).doubleValue());

                        activeConnectionsGauge.addMetric(
                                labelValueList,
                                ((Integer) server.getAttribute(mBean.getObjectName(), "numActive")).doubleValue());

                        idleConnectionsGauge.addMetric(
                                labelValueList,
                                ((Integer) server.getAttribute(mBean.getObjectName(), "numIdle")).doubleValue());

                    }

                    mfs.add(maxActiveConnectionsGauge);
                    mfs.add(activeConnectionsGauge);
                    mfs.add(idleConnectionsGauge);
                }
            }
        } catch (javax.management.AttributeNotFoundException e) {
            // Can happen in exception cases where TomcatDbcp2PoolExports is configured with a TomcatJdcpPool configured
        }
        catch (Exception e) {
            System.out.println ("####### " + e.getLocalizedMessage());
            e.printStackTrace();
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
