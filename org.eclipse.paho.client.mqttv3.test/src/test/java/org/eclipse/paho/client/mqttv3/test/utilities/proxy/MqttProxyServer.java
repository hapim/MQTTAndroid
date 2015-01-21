/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Bin Zhang - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.test.utilities.proxy;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * This MQTT proxy server acts as an intermediary between MQTT clients and an MQTT Server. MQTT clients connect to this
 * proxy server and send MQTT packets, and the proxy server passes these packets on to the MQTT Server, and also passes
 * the packets received from the MQTT Server to the MQTT clients.
 * 
 * <p>
 * A {@code IProxyInterceptor} can be set the this proxy server to handle what happens before a MQTT packet is being
 * transmitted to the real target, which may be simply {@link IMqttProxyContext#proceed()} with the original intercepted
 * packet or {@link IMqttProxyContext#proceed()} with a particular packet for some special purposes. And also the
 * connection between MQTT client and MQTT server can be closed via {@link IMqttProxyContext#terminateNow()} or can be
 * automatically closed when a condition is satisfied via {@link IMqttProxyContext#terminate(IMqttProxyPredicate)}.
 * 
 * <p>
 * Example:
 * 
 * <pre>
 * URI serverURI = new URI(&quot;tcp://localhost:1883&quot;);
 * int proxyPort = 18883;
 * 
 * MqttProxyServer proxy = MqttProxyServer.create(proxyPort, serverURI);
 * proxy.setInterceptor(new IMqttProxyInterceptor() {
 *     &#064;Override
 *     public void onReceive(IMqttProxyContext context, MqttProxyMessage message) {
 *         MqttWireMessage wireMessage = message.getWireMessage();
 *         context.proceed(wireMessage);
 *     }
 * 
 *     &#064;Override
 *     public void onSend(IMqttProxyContext context, MqttProxyMessage message) {
 *         MqttWireMessage wireMessage = message.getWireMessage();
 *         context.proceed(wireMessage);
 *     }
 * });
 * 
 * proxy.start();
 * 
 * MqttClient client1 = new MqttClient(proxy.getProxyURI(), &quot;c1&quot;);
 * client1.connect();
 * proxy.closeConnection(&quot;c1&quot;);
 * 
 * MqttClient client2 = new MqttClient(proxy.getProxyURI(), &quot;c2&quot;);
 * client2.connect();
 * proxy.closeConnection(&quot;c2&quot;);
 * 
 * proxy.shutdown();
 * 
 * </pre>
 */
public class MqttProxyServer {
    private static final Logger log = Logger.getLogger(MqttProxyServer.class.getName());
    private final List<MqttProxyConnection> connections = new CopyOnWriteArrayList<MqttProxyConnection>();

    private final int localPort;
    private final int remotePort;
    private final String remoteAddress;
    private final URI proxyURI;

    private ServerSocket serverSocket;
    private IMqttProxyInterceptor interceptor;
    private ExecutorService executor;
    private boolean running;

    /**
     * @param proxyURI - the URI is used by MQTT Client
     * @param localPort int - the port used by this proxy
     * @param remoteAddress - the address of the proxy
     * @param remotePort int - the port the proxy should use
     */
    MqttProxyServer(URI proxyURI, int localPort, String remoteAddress, int remotePort) {
        this.proxyURI = proxyURI;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    /**
     * Create a {@code ProxyServer} with the given {@code proxyPort} and MQTT {@code serverURI}.
     * 
     * @param proxyPort int - the port used by this proxy
     * @param mqttServerURI - the MQTT server URI
     */
    public static MqttProxyServer create(int proxyPort, URI serverURI) {
        String serverAddress = serverURI.getHost();
        int serverPort = serverURI.getPort();

        try {
            // Note: the proxy server is always running on localhost
            URI proxyURI = new URI(serverURI.getScheme(), serverURI.getUserInfo(), "localhost", proxyPort,
                    serverURI.getPath(), serverURI.getQuery(), serverURI.getFragment());

            log.info("MqttProxyServer is running at: " + proxyURI);

            return new MqttProxyServer(proxyURI, proxyPort, serverAddress, serverPort);
        }
        catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Start proxy server.
     */
    public synchronized MqttProxyServer start() throws Exception {
        if (running) {
            throw new IllegalStateException("Proxy is already running.");
        }
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(localPort));
        running = true;

        executor = Executors.newFixedThreadPool(1);
        executor.submit(new Acceptor());

        return this;
    }

    /**
     * Returns the proxy server URI for MQTT Client to connect.
     * 
     * @return the proxy server URI
     */
    public String getProxyURI() {
        return proxyURI.toString();
    }

    /**
     * Sets the interceptor to allow user to control the message flow.
     * 
     * @param interceptor
     */
    public synchronized void setInterceptor(IMqttProxyInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Force close the proxy connection for the given clientId.
     * 
     * @param clientId
     */
    public void closeConnection(String clientId) {
        for (MqttProxyConnection connection : connections) {
            if (connection.getClientId().equals(clientId)) {
                connection.close();
                break;
            }
        }
    }

    /**
     * Shutdown proxy server and closes all connections.
     */
    public synchronized void shutdown() {
        running = false;
        try {
            for (MqttProxyConnection connection : connections) {
                connection.close();
            }
        }
        catch (Exception e) {
            // ignore
        }
        finally {
            try {
                executor.shutdownNow();
                serverSocket.close();
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Accept connections.
     */
    class Acceptor implements Runnable {
        @Override
        public void run() {
            while (running) {
                Socket inbound = null;
                Socket outbound = null;
                try {
                    inbound = serverSocket.accept();
                    outbound = new Socket(remoteAddress, remotePort);

                    MqttProxyConnection connection = new MqttProxyConnection(inbound, outbound).open();
                    connection.setInterceptor(interceptor);
                    connections.add(connection);

                    log.info("MqttProxyServer accepted new connection from " + inbound.getInetAddress() + ":"
                            + inbound.getPort());
                }
                catch (Exception e) {
                    running = false;
                    silentClose(inbound);
                    silentClose(outbound);
                    // log.log(Level.SEVERE, "caught exception:", e);
                }
            }
        }
    }

    private void silentClose(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        }
        catch (Exception ex) {
            // ignore
        }
    }
}
