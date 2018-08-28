package com.spring.app.mvc.remote;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.client.ClientFactory;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class RequestInterceptorImpl implements RequestInterceptor {

    private static Log log = LogFactory.getLog(RequestInterceptorImpl.class);

    public static String RPC_SERVER = "RPC-SERVER";

    private static String LAST_HOST = "LAST-HOST";

    private String realName;
    private String rpcName;
    private String url = "http://rpc-client-no-init/";
    private RoundRobinRule chooseRule = new RoundRobinRule();

    public RequestInterceptorImpl(String rpcName) {
        this.rpcName = rpcName;
    }

    @Override
    public void apply(RequestTemplate template) {
        String host = this.url(template);
        Transaction t = Cat.newTransaction("Call", StringUtils.defaultIfBlank(realName, rpcName) + template.url());
        try {
            Cat.logEvent("Call.Method", template.method());
            RpcContext ctx = new RpcContext();
            Cat.logRemoteCallClient(ctx);
            for (Map.Entry<String, String> entry : ctx.getProperties().entrySet()) {
                template.header(entry.getKey(), entry.getValue());
            }
            template.header("X-CAT-TRACE-MODE", "true");
            template.header("X-CAT-ROOT-ID", ctx.getProperty(Cat.Context.ROOT));
            template.header("X-CAT-PARENT-ID", ctx.getProperty(Cat.Context.PARENT));
            template.header("X-CAT-ID", ctx.getProperty(Cat.Context.CHILD));
            Cat.logEvent("PigeonCall.app", realName);
        } catch (Exception e) {
            log.error("doGet error,url:" + this.url + ",errorMessage:" + e.getMessage(), e);
        } finally {
//            CatRpcAop.RESOURCE.set(t);
        }
        //设置请求的host
        template.header(HardCodedTarget.REQUEST_HOST, host);
        template.header(RPC_SERVER, rpcName);
    }

    public String url(RequestTemplate input) {
        //获取地址
        String addr = selectOne(input, rpcName);
        if (!StringUtils.isEmpty(addr)) {
            this.url = addr;
            return addr;
        } else {
            log.error("Get Service Error!"+ rpcName + " get addr is null");
            return this.url;
        }
    }

    /**
     * 根据轮询策略选择一个地址
     *
     * @param clientName 提供服务的服务名 例如：ECHO-SERVICE
     */
    private String selectOne(RequestTemplate input, String clientName) {
        try {
            //会缓存结果, 所以不用担心它每次都会向eureka发起查询
            DynamicServerListLoadBalancer lb = (DynamicServerListLoadBalancer) ClientFactory.getNamedLoadBalancer(clientName);
            if (StringUtils.isEmpty(realName)) {
                try {
                    if (lb.getServerListImpl().getInitialListOfServers().size() > 0) {
                        realName = ((DiscoveryEnabledServer) lb.getServerListImpl().getInitialListOfServers().get(0)).getInstanceInfo().getVIPAddress().toLowerCase();
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
            Server selected = null;
            //判断如果是非第一次请求（第一次请求失败的情况：超时、系统异常）
            final Collection<String> lastHosts = input.headers().get(LAST_HOST);
            if (!CollectionUtils.isEmpty(lastHosts)) {
                Optional<Server> first = lb.getServerList(true).stream().filter(server -> !lastHosts.contains(server.getHost())).findFirst();
                selected = first.isPresent() ? first.get() : null;
            }
            if (selected == null) {
                selected = chooseRule.choose(lb, null);
            }
            if (selected != null) {
                Collection<String> hosts = CollectionUtils.isEmpty(lastHosts) ? new ArrayList() : lastHosts;
                hosts.add(selected.getHost());
                input.header(LAST_HOST, hosts);
                return new StringBuilder("http://").append(selected.getHost()).append(":").append(selected.getPort()).toString();
            }
        } catch (Exception e) {
            log.error("Select Springcloud Server Error : {}", e);
        }
        return null;
    }

    class RpcContext implements Cat.Context {
        private Map<String, String> properties = new HashMap<String, String>();
        @Override
        public void addProperty(String key, String value) {
            properties.put(key, value);
        }
        @Override
        public String getProperty(String key) {
            return properties.get(key);
        }
        public final Map<String, String> getProperties() {
            return properties;
        }
    }

}
