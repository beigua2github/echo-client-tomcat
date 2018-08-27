package com.spring.app.mvc.service;

import com.spring.app.mvc.remote.RpcServer;
import feign.Param;
import feign.RequestLine;

/**
 * Created by beigua on 2018/8/28.
 */
@RpcServer("echo-service")
public interface EchoService {
    @RequestLine("GET /world?msg={msg}")
    String hello(@Param("msg") String msg);
}
