package com.spring.app.mvc.remote;

import com.spring.app.mvc.codec.JacksonDecoder;
import com.spring.app.mvc.codec.JacksonEncoder;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.factory.FactoryBean;


/**
 *
 * @author beigua
 * @date 2018/8/27
 */
public class ServiceFactory<T> implements FactoryBean<T> {
    private String proxyInterfaceName;
    private Class<T> proxyInterface;
    private T t;

    public void init() throws ClassNotFoundException {
        proxyInterface = (Class<T>) ClassUtils.getClass(proxyInterfaceName.trim());

        HardCodedTarget<T> hardCodedTarget = new HardCodedTarget(proxyInterface);

        t = Feign.builder()
                .options(new Request.Options(500, 5200))
                .retryer(new Retryer.Default(200, 800, 4))
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(new RequestInterceptorImpl(getServerName()))
                .target(hardCodedTarget);
    }

    private String getServerName() {
        RpcServer rpcServer = proxyInterface.getAnnotation(RpcServer.class);

        return rpcServer.value();
    }

    @Override
    public T getObject() throws Exception {
        return this.t;
    }

    @Override
    public Class<?> getObjectType() {
        return this.proxyInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setProxyInterfaceName(String proxyInterfaceName) {
        this.proxyInterfaceName = proxyInterfaceName;
    }
}
