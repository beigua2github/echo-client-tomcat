package com.spring.app.mvc.listener;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

/**
 * Created by beigua on 2018/8/27.
 */
public class EurekaInitAndRegisterListener implements ApplicationListener<ApplicationEvent> {
    private static final String NETWORK_CARD = "eth0|en0|";

    static volatile boolean isInit;

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (isInit) {
            return;
        }
        System.out.println("Eureka Starting begin...");

        Integer port = null;
        try {
            port = getPort();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        if (port != null) {
            init(port);
            up();
        }

        System.out.println("Eureka Starting end...");
    }

    private Integer getPort() throws MalformedObjectNameException {
        Integer serverPort = null;

        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

        Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
        String port = objectNames.iterator().next().getKeyProperty("port");
        return Integer.valueOf(port);
    }

    public static void init(final Integer serverPort) {
        if (!isInit) {
            //Load Eureka Config
            if (serverPort != null) {
                Properties properties = new Properties();
                properties.put("eureka.port", serverPort);
                ConfigurationManager.loadProperties(properties);
            }
            //Load Eureka Config
            try {
                ConfigurationManager.loadPropertiesFromResources("rpc.properties");
            } catch (IOException e) {
            }
            //Load Eureka Config

            //Init Eureka Client
            DiscoveryManager.getInstance().initComponent(
                    new MyDataCenterInstanceConfig() {
                        @Override
                        public String getHostName(boolean refresh) {
                            String vipAddress = "localhost";
                            vipAddress = ConfigurationManager.getConfigInstance().getString("eureka.vipAddress");

                            return getLocalIP() + "- " + vipAddress + "-" + serverPort;
                        }
                    },
                    new DefaultEurekaClientConfig());

            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);
            isInit = true;
        }
    }

    public static void up() {
        if (isInit) {
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        }
    }


    private static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces();
            while (e1.hasMoreElements()) {
                NetworkInterface ni = e1.nextElement();
                if (NETWORK_CARD.indexOf(ni.getName() + "|") >= 0) {
                    Enumeration<InetAddress> e2 = ni.getInetAddresses();
                    while (e2.hasMoreElements()) {
                        InetAddress ia = e2.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;
                        }
                        return ia.getHostAddress();
                    }
                    break;
                } else {
                    continue;
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
