package org.springframework.samples.petclinic.testcommon;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

/**
 * HTTP proxy server that injects a {@code baggage} header containing the test operator ID
 * into all proxied HTTP requests for Parasoft coverage tracking.
 * <p>
 * Uses LittleProxy and Netty with dynamic port assignment. Integrates with
 * {@link ParasoftSettings} for user identification.
 */
public class ParasoftHeaderInjectingProxy {
    private final Logger LOGGER = Logger.getLogger(ParasoftHeaderInjectingProxy.class.getName());
    private final AtomicReference<String> coverageUserIdRef;
    private final HttpProxyServer proxy;

    public ParasoftHeaderInjectingProxy() {
        this.coverageUserIdRef = new AtomicReference<String>("__UNINITIALIZED__");
        this.proxy = startProxy();
    }

    public HttpProxyServer startProxy() {
        return startProxy(coverageUserIdRef);
    }

    public AtomicReference<String> getCoverageUserIdRef() {
        return coverageUserIdRef;
    }

    public HttpProxyServer getProxy() {
        return proxy;
    }

    /** Sets the coverage user ID by copying the value from the given reference. */
    public void setCoverageUserId(AtomicReference<String> externalRef) {
        Objects.requireNonNull(externalRef);
        coverageUserIdRef.set(externalRef.get());
    }

    public HttpProxyServer startProxy(AtomicReference<String> coverageUserIdRef) {
        InetSocketAddress bindAddress = null;
        if (ParasoftSettings.PROXY_BIND_HOST != null && !ParasoftSettings.PROXY_BIND_HOST.isBlank()) {
            try {
                // Use an IPv4 address for containerized Grid server scenarios.
                bindAddress = new InetSocketAddress(InetAddress.getByName(ParasoftSettings.PROXY_BIND_HOST), 0);
                if (ParasoftSettings.isLogLevelEnabled("TRACE")) {
                    LOGGER.info("[ParasoftHeaderInjectingProxy] Proxy binding to address: " + bindAddress);
                }
            } catch (Exception e) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftHeaderInjectingProxy] Failed to resolve bind host '" + ParasoftSettings.PROXY_BIND_HOST + "': " + e.getMessage());
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
                                String coverageUserId = coverageUserIdRef.get();
                                if (ParasoftSettings.isLogLevelEnabled("TRACE")) {
                                    // very noisy, enable TRACE if you need to see the actual baggage header being injected into the requests
                                    LOGGER.info("[ParasoftHeaderInjectingProxy] Proxy baggage header: baggage: test-operator-id=" + coverageUserId);
                                }
                                if (httpObject instanceof HttpRequest request) {
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
}
