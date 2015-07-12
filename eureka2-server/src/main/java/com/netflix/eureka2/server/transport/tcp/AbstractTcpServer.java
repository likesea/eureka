/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.eureka2.server.transport.tcp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.netflix.eureka2.server.config.EurekaServerTransportConfig;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.RxServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tomasz Bak
 */
public abstract class AbstractTcpServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTcpServer.class);

    protected final EurekaServerTransportConfig config;
    protected final MetricEventsListenerFactory servoEventsListenerFactory;
    private final int serverPort;
    private final PipelineConfigurator<Object, Object> pipelineConfigurator;
    private final ConnectionHandler<Object, Object> tcpHandlerProvider;
    protected RxServer<Object, Object> server;

    protected AbstractTcpServer(MetricEventsListenerFactory servoEventsListenerFactory,
                                EurekaServerTransportConfig config,
                                int serverPort,
                                PipelineConfigurator<Object, Object> pipelineConfigurator,
                                ConnectionHandler<Object, Object> tcpHandlerProvider) {
        this.servoEventsListenerFactory = servoEventsListenerFactory;
        this.config = config;
        this.serverPort = serverPort;
        this.pipelineConfigurator = pipelineConfigurator;
        this.tcpHandlerProvider = tcpHandlerProvider;
    }

    /**
     * override for additional start up needs
     */
    public void start() {
    }

    /**
     * override for additional shutdown needs
     */
    public void stop() {
    }

    @PostConstruct
    protected void _start() {
        server = RxNetty.newTcpServerBuilder(serverPort, tcpHandlerProvider)
                .pipelineConfigurator(pipelineConfigurator)
                .withMetricEventsListenerFactory(servoEventsListenerFactory)
                .build()
//                .withErrorHandler()  TODO use a custom handler (?) as the default emits extraneous error logs
                .start();

        start();

        logger.info("Starting {} on port {} with {} encoding...", getClass().getSimpleName(), server.getServerPort(), config.getCodec());
    }

    @PreDestroy
    protected void _stop() {
        stop();

        if (server != null) {
            try {
                server.shutdown();
                logger.info("Stopped TCP server {}", this);
            } catch (InterruptedException e) {
                logger.info("Shutdown of TCP server " + this + " interrupted", e);
            } finally {
                server = null;
            }
        }
    }

    public int serverPort() {
        return server.getServerPort();
    }

    @Override
    public String toString() {
        String port = server == null ? "N/A" : Integer.toString(serverPort());
        return "{server=" + this.getClass().getSimpleName() + ", port=" + port + '}';
    }
}