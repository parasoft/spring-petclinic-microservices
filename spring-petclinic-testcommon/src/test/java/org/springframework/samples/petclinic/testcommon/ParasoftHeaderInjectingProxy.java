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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class ParasoftHeaderInjectingProxy {
    private static final Logger LOGGER = Logger.getLogger(ParasoftHeaderInjectingProxy.class.getName());

    public static HttpProxyServer startProxy() {
        return startProxy(null);
    }

    public static HttpProxyServer startProxy(AtomicReference<String> coverageUserIdRef) {
        InetSocketAddress bindAddress = null;
        if (ParasoftSettings.PROXY_BIND_HOST != null && !ParasoftSettings.PROXY_BIND_HOST.isBlank()) {
            try {
                // Use an IPv4 address for containerized Grid server scenarios.
                bindAddress = new InetSocketAddress(InetAddress.getByName(ParasoftSettings.PROXY_BIND_HOST), 0);
            } catch (Exception e) {
                if (ParasoftSettings.CTP_DEBUG) {
                    LOGGER.info("[ParasoftHeaderInjectingProxy] Failed to resolve bind host '" + ParasoftSettings.PROXY_BIND_HOST + "': " + e.getMessage());
                }
                bindAddress = null;
            }
        }
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap();
        if (bindAddress != null) {
            bootstrap = bootstrap.withAddress(bindAddress);
        } else {
            bootstrap = bootstrap.withPort(0); // auto-assign port
        }
        return bootstrap
                .withFiltersSource(new HttpFiltersSourceAdapter() {

                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {

                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                if (httpObject instanceof HttpRequest request) {
                                    String coverageUserId = resolveCoverageUserId(coverageUserIdRef);
                                    request.headers().set(
                                            "baggage",
                                            "test-operator-id=" + coverageUserId);
                                }
                                return null; // continue proxying
                            }
                        };
                    }
                })
                .start();
    }

    private static String resolveCoverageUserId(AtomicReference<String> coverageUserIdRef) {
        if (coverageUserIdRef == null) {
            return "NoCoverageUserIdProvided";
        }
        String coverageUserId = coverageUserIdRef.get();
        if (coverageUserId == null || coverageUserId.isBlank()) {
            return "NoCoverageUserIdProvided";
        }
        return coverageUserId;
    }
}
