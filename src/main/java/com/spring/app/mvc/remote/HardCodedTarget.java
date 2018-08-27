package com.spring.app.mvc.remote;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Optional;

public class HardCodedTarget<T> extends Target.HardCodedTarget<T> {

    private static Log log = LogFactory.getLog(HardCodedTarget.class);

    public static String REQUEST_HOST = "REQUEST-HOST";

    public HardCodedTarget(Class type) {
        super(type, "http://null/");
    }

    @Override
    public Request apply(RequestTemplate input) {
        if (input.url().indexOf("http") != 0) {
            Optional<String> first = input.headers().get(REQUEST_HOST).stream().findFirst();
            input.insert(0, (first.isPresent() ? first.get() : this.url()));
        }
        Request request = input.request();
        log.info("rpc-url = " + request.method() + " " + request.url());
        return request;
    }


}
