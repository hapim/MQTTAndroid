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
 *******************************************************************************/
package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.client.mqttv3.test.utilities.proxy.IMqttProxyContext;
import org.eclipse.paho.client.mqttv3.test.utilities.proxy.IMqttProxyInterceptor;
import org.eclipse.paho.client.mqttv3.test.utilities.proxy.MqttProxyMessage;
import org.eclipse.paho.client.mqttv3.test.utilities.proxy.MqttProxyServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for MQTT callbacks.
 */
public class MqttCallbackTest {
    private static final Logger log = Logger.getLogger(MqttCallbackTest.class.getName());
    private static final MqttClientPersistence DATA_STORE = new MemoryPersistence();
    private static final int PROXY_PORT = 18883;
    private static URI serverURI;
    private static MqttProxyServer proxy;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            String methodName = Utility.getMethodName();
            LoggingUtilities.banner(log, MqttCallbackTest.class, methodName);
            serverURI = TestProperties.getServerURI();
            proxy = MqttProxyServer.create(PROXY_PORT, serverURI).start();
            proxy.setInterceptor(new IMqttProxyInterceptor() {
                @Override
                public void onReceive(IMqttProxyContext context, MqttProxyMessage message) {
                    context.proceed(message.getWireMessage());
                }

                @Override
                public void onSend(IMqttProxyContext context, MqttProxyMessage message) {
                    context.proceed(message.getWireMessage());
                }
            });
        }
        catch (Exception exception) {
            log.log(Level.SEVERE, "caught exception:", exception);
            throw exception;
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        String methodName = Utility.getMethodName();
        LoggingUtilities.banner(log, MqttCallbackTest.class, methodName);
        proxy.shutdown();
    }

    /**
     * Test case for bug 434761: Connect onSuccess getting executed for the second when connection closed by server
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=434761
     */
    @Test
    public void testConnectCallback() throws Exception {
        // connect to proxy
        String clientId = "testConnectCallback";
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        MqttAsyncClient client = new MqttAsyncClient(proxy.getProxyURI(), clientId, DATA_STORE);

        final AtomicInteger callbackCalled = new AtomicInteger(0);
        client.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                callbackCalled.incrementAndGet();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                callbackCalled.incrementAndGet();
            }
        }).waitForCompletion();

        // force client connection close
        proxy.closeConnection(clientId);

        // wait some time for callback to be invoked
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(callbackCalled.get() == 1);
    }

    /**
     * Test case for bug 455911: Subscription Callback invoked a second time when connection gets dropped
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=455911
     */
    @Test
    public void testSubscribeCallback() throws Exception {
        // connect to proxy
        String clientId = "testSubscribeCallback";
        String topic = "testTopic";
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        MqttAsyncClient client = new MqttAsyncClient(proxy.getProxyURI(), clientId, DATA_STORE);
        client.connect(options).waitForCompletion();

        final AtomicInteger callbackCalled = new AtomicInteger(0);

        // 1) test subscribe
        client.subscribe(topic, 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                callbackCalled.incrementAndGet();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                callbackCalled.incrementAndGet();
            }
        }).waitForCompletion();

        // force client connection close
        proxy.closeConnection(clientId);

        // wait some time for callback to be invoked
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(callbackCalled.get() == 1);

        // 2) test unsubscribe
        client.connect(options).waitForCompletion();
        callbackCalled.set(0);
        client.unsubscribe(topic, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                callbackCalled.incrementAndGet();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                callbackCalled.incrementAndGet();
            }
        }).waitForCompletion();

        // force client connection close
        proxy.closeConnection(clientId);

        // wait some time for callback to be invoked
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(callbackCalled.get() == 1);
    }

}
