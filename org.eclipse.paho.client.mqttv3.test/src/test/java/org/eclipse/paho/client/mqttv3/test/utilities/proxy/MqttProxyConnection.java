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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * Proxy connection between an MQTT client and an MQTT server.
 */
class MqttProxyConnection {
    private static final Logger log = Logger.getLogger(MqttProxyConnection.class.getName());

    private final Socket localSocket;
    private final Socket remoteSocket;
    private final MqttProxyChannel inbound;
    private final MqttProxyChannel outbound;
    private final ExecutorService executor;

    private String clientId;
    private IMqttProxyInterceptor interceptor;

    public MqttProxyConnection(Socket localSocket, Socket remoteSocket) throws Exception {
        this.localSocket = localSocket;
        this.remoteSocket = remoteSocket;

        // remote input --> local output
        inbound = MqttProxyChannel.open(new IMqttProxyHandler() {
            @Override
            public void handle(IMqttProxyContext context, MqttProxyMessage message) {
                if (null != interceptor) {
                    interceptor.onReceive(context, message);
                }
                else {
                    log.finest("inbound<<<" + message);
                    context.proceed(message.getData());
                }
            }
        }, remoteSocket.getInputStream(), localSocket.getOutputStream());

        // local input --> remote output
        outbound = MqttProxyChannel.open(new IMqttProxyHandler() {
            @Override
            public void handle(IMqttProxyContext context, MqttProxyMessage message) {
                MqttWireMessage wireMessage = message.getWireMessage();
                if (wireMessage.getType() == MqttWireMessage.MESSAGE_TYPE_CONNECT) {
                    try {
                        clientId = decodeClientId(wireMessage.getPayload());
                    }
                    catch (MqttException e) {
                        log.log(Level.SEVERE, "caught exception:", e);
                    }
                }

                if (null != interceptor) {
                    interceptor.onSend(context, message);
                }
                else {
                    log.finest("outbound>>>" + message);
                    context.proceed(message.getData());
                }
            }
        }, localSocket.getInputStream(), remoteSocket.getOutputStream());

        executor = Executors.newFixedThreadPool(2);
    }

    public String getClientId() {
        return clientId;
    }

    public MqttProxyConnection open() {
        executor.submit(inbound);
        executor.submit(outbound);
        return this;
    }

    public MqttProxyConnection setInterceptor(IMqttProxyInterceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    /**
     * Close connection
     */
    public void close() {
        inbound.terminateNow();
        outbound.terminateNow();
        try {
            localSocket.close();
            remoteSocket.close();
        }
        catch (Exception e) {
            // ignore
        }
        finally {
            executor.shutdownNow();
        }
    }

    /**
     * Decode ClientId from MQTT CONNECT packet.
     */
    private String decodeClientId(byte[] connectPayload) throws MqttException {
        return decodeUTF8(new DataInputStream(new ByteArrayInputStream(connectPayload)));
    }

    private String decodeUTF8(DataInputStream input) throws MqttException {
        try {
            int encodedLength = input.readUnsignedShort();
            byte[] encodedString = new byte[encodedLength];
            input.readFully(encodedString);
            return new String(encodedString, "UTF-8");
        }
        catch (IOException ex) {
            throw new MqttException(ex);
        }
    }
}
