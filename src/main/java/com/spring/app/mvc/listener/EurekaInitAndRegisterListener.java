package com.spring.app.mvc.listener;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.spring.app.mvc.Utils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EurekaInitAndRegisterListener implements ApplicationListener<ApplicationEvent> {

    private static volatile boolean isInit;

    private final Lock lock = new ReentrantLock();

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            eurekaClientRegister();
        } else if (applicationEvent instanceof ContextClosedEvent || applicationEvent instanceof ContextStoppedEvent) {
            eurekaClientCancel();
        }
    }

    private void eurekaClientCancel() {
        if (isInit) {
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
            isInit = false;
        }
    }

    private void eurekaClientRegister() {
        if (isInit) {
            return;
        }

        System.out.println("Eureka Starting...");
        Integer port = getPort();

        if (port != null) {
            lock.lock();
            init(port);
            up();
            lock.unlock();
            System.out.println("Eureka Success...");
        } else {
            System.out.println("Eureka Error...");
        }
    }

    private Integer getPort() {
        String port = null;

        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
            port = objectNames.iterator().next().getKeyProperty("port");
        } catch (MalformedObjectNameException e) {
            //ignore
        }

        return port == null ? null : Integer.valueOf(port);
    }

    private void init(final Integer serverPort) {
        if (!isInit) {
            //Load Eureka Config
            if (serverPort != null) {
                Properties properties = new Properties();
                properties.put("eureka.port", serverPort);
                ConfigurationManager.loadProperties(properties);
            }

            //Load Eureka Ribbon Config
            try {
                ConfigurationManager.loadPropertiesFromResources("rpc.properties");
            } catch (IOException e) {
            }

            //Init Eureka Client
            DiscoveryManager.getInstance().initComponent(
                    new MyDataCenterInstanceConfig() {
                        @Override
                        public String getHostName(boolean refresh) {
                            String vipAddress = ConfigurationManager.getConfigInstance().getString("eureka.vipAddress");

                            return Utils.getLocalIP() + "- " + vipAddress + "-" + serverPort;
                        }
                    },
                    new DefaultEurekaClientConfig());

            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);
            isInit = true;
        }
    }

    private void up() {
        if (isInit) {
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        }
    }
}
