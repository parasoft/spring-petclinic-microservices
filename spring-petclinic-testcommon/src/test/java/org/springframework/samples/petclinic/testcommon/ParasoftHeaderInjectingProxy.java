package org.springframework.samples.petclinic.testcommon;

import java.net.InetAddress;
import java.net.InetSocketAddress;
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
 * HTTP proxy server that injects a {@code baggage} header into all proxied HTTP requests for
 * Parasoft coverage tracking. The baggage value (e.g. {@code "test-operator-id=admin+uuid"})
 * is sourced from the {@code /test/start} API response and updated per-test via
 * {@link org.springframework.samples.petclinic.testcommon.ParasoftSessionManager#updateBaggage}.
 * <p>
 * Uses LittleProxy and Netty with dynamic port assignment.
 */
public class ParasoftHeaderInjectingProxy {
    private final Logger LOGGER = Logger.getLogger(ParasoftHeaderInjectingProxy.class.getName());
    private final AtomicReference<String> baggageRef;
    private final HttpProxyServer proxy;

    public ParasoftHeaderInjectingProxy() {
        this.baggageRef = new AtomicReference<String>(ParasoftSessionManager.BAGGAGE_UNINITIALIZED);
        this.proxy = startProxy();
    }

    public HttpProxyServer startProxy() {
        return startProxy(baggageRef);
    }

    /** Returns the {@link AtomicReference} holding the current baggage value for this proxy instance. */
    public AtomicReference<String> getBaggageRef() {
        return baggageRef;
    }

    public HttpProxyServer getProxy() {
        return proxy;
    }

    public HttpProxyServer startProxy(AtomicReference<String> baggageRef) {
        // Resolve the configured log level ONCE at proxy construction.
        final boolean traceEnabled = ParasoftSettings.isLogLevelEnabled("TRACE");
        
        InetSocketAddress bindAddress = null;
        if (ParasoftSettings.PROXY_BIND_HOST != null && !ParasoftSettings.PROXY_BIND_HOST.isBlank()) {
            try {
                // Use an IPv4 address for containerized Grid server scenarios.
                bindAddress = new InetSocketAddress(InetAddress.getByName(ParasoftSettings.PROXY_BIND_HOST), 0);
                if (traceEnabled) {
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
                                // Guard on HttpRequest BEFORE any other work so chunked request
                                // bodies (e.g. POST form data) skip the hot path entirely.
                                if (httpObject instanceof HttpRequest request) {
                                    String value = baggageRef.get();
                                    if (traceEnabled) {
                                        // very noisy, enable TRACE if you need to see the baggage header being injected into the requests
                                        LOGGER.info("[ParasoftHeaderInjectingProxy] Proxy baggage header: baggage: " + value);
                                    }
                                    if (value != null && !ParasoftSessionManager.isBaggageSentinel(value)) {
                                        request.headers().set("baggage", value);
                                    }
                                }
                                return null; // continue proxying
                            }
                        };
                    }
                })
                .start();
    }
}
