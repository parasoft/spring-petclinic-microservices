/**
 * ParasoftHeaderInjectingProxy creates an HTTP proxy server that injects custom headers into outgoing requests.
 * <p>
 * This class uses LittleProxy and Netty to start a proxy server that automatically adds
 * a "baggage" header containing the test operator ID to all proxied HTTP requests.
 * <ul>
 *   <li>Used for coverage tracking in Parasoft environments</li>
 *   <li>Supports dynamic port assignment</li>
 *   <li>Integrates with ParasoftSettings for user identification</li>
 * </ul>
 */
package org.springframework.samples.petclinic.testcommon;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class ParasoftHeaderInjectingProxy {

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
