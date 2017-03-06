package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.catalina.util.ServerInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Exports Tomcat metrics applicable to most most applications:
 *
 * - http session metrics
 * - request processor metrics
 * - thread pool metrics
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new TomcatGenericExports(false).register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *     tomcat_info{version="7.0.61.0",build="Apr 29 2015 14:58:03 UTC",} 1.0
 *     tomcat_session_active_total{context="/foo",host="default",} 877.0
 *     tomcat_session_rejected_total{context="/foo",host="default",} 0.0
 *     tomcat_session_created_total{context="/foo",host="default",} 24428.0
 *     tomcat_session_expired_total{context="/foo",host="default",} 23832.0
 *     tomcat_session_alivetime_seconds_avg{context="/foo",host="default",} 633.0
 *     tomcat_session_alivetime_seconds_max{context="/foo",host="default",} 9883.0
 *     tomcat_requestprocessor_received_bytes{name="http-bio-0.0.0.0-8080",} 0.0
 *     tomcat_requestprocessor_sent_bytes{name="http-bio-0.0.0.0-8080",} 5056098.0
 *     tomcat_requestprocessor_time_seconds{name="http-bio-0.0.0.0-8080",} 127386.0
 *     tomcat_requestprocessor_error_count{name="http-bio-0.0.0.0-8080",} 0.0
 *     tomcat_requestprocessor_request_count{name="http-bio-0.0.0.0-8080",} 33709.0
 *     tomcat_threads_total{pool="http-bio-0.0.0.0-8080",} 10.0
 *     tomcat_threads_active_total{pool="http-bio-0.0.0.0-8080",} 2.0
 *     tomcat_threads_active_max{pool="http-bio-0.0.0.0-8080",} 200.0
 *  </pre>
 */

public class TomcatGenericExports extends Collector {

    private static final Log log = LogFactory.getLog(TomcatGenericExports.class);
    private String jmxDomain = "Catalina";

    public TomcatGenericExports(boolean embedded) {
        if (embedded) {
            jmxDomain = "Tomcat";
        }
    }
    private void addRequestProcessorMetrics(List<MetricFamilySamples> mfs) {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName(jmxDomain + ":type=GlobalRequestProcessor,name=*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelNameList = Collections.singletonList("name");

                GaugeMetricFamily requestProcessorBytesReceivedGauge = new GaugeMetricFamily(
                        "tomcat_requestprocessor_received_bytes",
                        "Number of bytes received by this request processor",
                        labelNameList);

                GaugeMetricFamily requestProcessorBytesSentGauge = new GaugeMetricFamily(
                        "tomcat_requestprocessor_sent_bytes",
                        "Number of bytes sent by this request processor",
                        labelNameList);

                GaugeMetricFamily requestProcessorProcessingTimeGauge = new GaugeMetricFamily(
                        "tomcat_requestprocessor_time_seconds",
                        "The total time spend by this request processor",
                        labelNameList);

                CounterMetricFamily requestProcessorErrorCounter = new CounterMetricFamily(
                        "tomcat_requestprocessor_error_count",
                        "The number of error request served by this request processor",
                        labelNameList);

                CounterMetricFamily requestProcessorRequestCounter = new CounterMetricFamily(
                        "tomcat_requestprocessor_request_count",
                        "The number of request served by this request processor",
                        labelNameList);

                for (final ObjectInstance mBean : mBeans) {
                    List<String> labelValueList = Collections.singletonList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""));

                    requestProcessorBytesReceivedGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "bytesReceived")).doubleValue());

                    requestProcessorBytesSentGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "bytesSent")).doubleValue());

                    requestProcessorProcessingTimeGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "processingTime")).doubleValue() / 1000.0);

                    requestProcessorErrorCounter.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "errorCount")).doubleValue());

                    requestProcessorRequestCounter.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "requestCount")).doubleValue());
                }

                mfs.add(requestProcessorBytesReceivedGauge);
                mfs.add(requestProcessorBytesSentGauge);
                mfs.add(requestProcessorProcessingTimeGauge);
                mfs.add(requestProcessorRequestCounter);
                mfs.add(requestProcessorErrorCounter);
            }
        } catch (Exception e) {
            log.error("Error retrieving metric.", e);
        }
    }


    private void addSessionMetrics(List<MetricFamilySamples> mfs) {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName(jmxDomain + ":type=Manager,context=*,host=*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelNameList = Arrays.asList("context", "host");

                GaugeMetricFamily activeSessionCountGauge = new GaugeMetricFamily(
                        "tomcat_session_active_total",
                        "Number of active sessions",
                        labelNameList);

                GaugeMetricFamily rejectedSessionCountGauge = new GaugeMetricFamily(
                        "tomcat_session_rejected_total",
                        "Number of sessions rejected due to maxActive being reached",
                        labelNameList);

                GaugeMetricFamily createdSessionCountGauge = new GaugeMetricFamily(
                        "tomcat_session_created_total",
                        "Number of sessions created",
                        labelNameList);

                GaugeMetricFamily expiredSessionCountGauge = new GaugeMetricFamily(
                        "tomcat_session_expired_total",
                        "Number of sessions that expired",
                        labelNameList);

                GaugeMetricFamily sessionAvgAliveTimeGauge = new GaugeMetricFamily(
                        "tomcat_session_alivetime_seconds_avg",
                        "Average time an expired session had been alive",
                        labelNameList);

                GaugeMetricFamily sessionMaxAliveTimeGauge = new GaugeMetricFamily(
                        "tomcat_session_alivetime_seconds_max",
                        "Maximum time an expired session had been alive",
                        labelNameList);

                GaugeMetricFamily contextStateGauge = new GaugeMetricFamily(
                        "tomcat_context_state_started",
                        "Indication if the lifecycle state of this context is STARTED",
                        labelNameList);

                for (final ObjectInstance mBean : mBeans) {
                    List<String> labelValueList = Arrays.asList(mBean.getObjectName().getKeyProperty("host"), mBean.getObjectName().getKeyProperty("context"));

                    activeSessionCountGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "activeSessions")).doubleValue());

                    rejectedSessionCountGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "rejectedSessions")).doubleValue());

                    createdSessionCountGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "sessionCounter")).doubleValue());

                    expiredSessionCountGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "expiredSessions")).doubleValue());

                    sessionAvgAliveTimeGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "sessionAverageAliveTime")).doubleValue());

                    sessionMaxAliveTimeGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "sessionMaxAliveTime")).doubleValue());

                    if (server.getAttribute(mBean.getObjectName(), "stateName").equals("STARTED")) {
                        contextStateGauge.addMetric(labelValueList, 1.0);
                    } else {
                        contextStateGauge.addMetric(labelValueList, 0.0);
                    }
                }

                mfs.add(activeSessionCountGauge);
                mfs.add(rejectedSessionCountGauge);
                mfs.add(createdSessionCountGauge);
                mfs.add(expiredSessionCountGauge);
                mfs.add(sessionAvgAliveTimeGauge);
                mfs.add(sessionMaxAliveTimeGauge);
                mfs.add(contextStateGauge);
            }
        } catch (Exception e) {
            log.error("Error retrieving metric.", e);
        }
    }


    private void addThreadPoolMetrics(List<MetricFamilySamples> mfs) {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName(jmxDomain + ":type=ThreadPool,name=*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelList = Collections.singletonList("name");

                GaugeMetricFamily threadPoolCurrentCountGauge = new GaugeMetricFamily(
                        "tomcat_threads_total",
                        "Number threads in this pool.",
                        labelList);

                GaugeMetricFamily threadPoolActiveCountGauge = new GaugeMetricFamily(
                        "tomcat_threads_active_total",
                        "Number of active threads in this pool.",
                        labelList);

                GaugeMetricFamily threadPoolMaxThreadsGauge = new GaugeMetricFamily(
                        "tomcat_threads_max",
                        "Maximum number of threads allowed in this pool.",
                        labelList);

                GaugeMetricFamily threadPoolConnectionCountGauge = new GaugeMetricFamily(
                        "tomcat_connections_active_total",
                        "Number of connections served by this pool.",
                        labelList);

                GaugeMetricFamily threadPoolMaxConnectionGauge = new GaugeMetricFamily(
                            "tomcat_connections_active_max",
                        "Maximum number of concurrent connections served by this pool.",
                        labelList);

                for (final ObjectInstance mBean : mBeans) {
                    List<String> labelValueList = Collections.singletonList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""));

                    threadPoolCurrentCountGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "currentThreadCount")).doubleValue());

                    threadPoolActiveCountGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "currentThreadsBusy")).doubleValue());

                    threadPoolMaxThreadsGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "maxThreads")).doubleValue());

                    threadPoolConnectionCountGauge.addMetric(
                            labelValueList,
                            ((Long) server.getAttribute(mBean.getObjectName(), "connectionCount")).doubleValue());

                    threadPoolMaxConnectionGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "maxConnections")).doubleValue());

                }

                mfs.add(threadPoolCurrentCountGauge);
                mfs.add(threadPoolActiveCountGauge);
                mfs.add(threadPoolMaxThreadsGauge);
                mfs.add(threadPoolConnectionCountGauge);
                mfs.add(threadPoolMaxConnectionGauge);
            }
        } catch (Exception e) {
            log.error("Error retrieving metric.", e);
        }
    }


    private void addVersionInfo(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily tomcatInfo = new GaugeMetricFamily(
                "tomcat_info",
                "tomcat version info",
                Arrays.asList("version", "build"));
        tomcatInfo.addMetric(Arrays.asList(ServerInfo.getServerNumber(), ServerInfo.getServerBuilt()), 1);
        mfs.add(tomcatInfo);
    }


    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        addSessionMetrics(mfs);
        addThreadPoolMetrics(mfs);
        addRequestProcessorMetrics(mfs);
        addVersionInfo(mfs);
        return mfs;

    }
}
