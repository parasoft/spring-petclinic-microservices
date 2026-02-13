package org.springframework.samples.petclinic.selenium;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class HeaderInjectingProxy {

    public static HttpProxyServer startProxy() {
        return DefaultHttpProxyServer.bootstrap()
                .withPort(0) // auto-assign port
                .withFiltersSource(new HttpFiltersSourceAdapter() {

                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {

                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                if (httpObject instanceof HttpRequest request) {
                                    request.headers().set(
                                            "baggage",
                                            "test-operator-id=" + ParasoftSettings.getCoverageUserId());
                                }
                                return null; // continue proxying
                            }
                        };
                    }
                })
                .start();
    }
}