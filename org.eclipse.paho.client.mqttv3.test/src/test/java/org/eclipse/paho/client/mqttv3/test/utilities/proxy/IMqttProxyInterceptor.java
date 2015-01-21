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

/**
 * Proxy interceptor to intercept the messages between MQTT client and server.
 */
public interface IMqttProxyInterceptor {

    /**
     * Invoked when a message is received from an MQTT server by the proxy server, but not yet passed to an MQTT client
     * that connected to this proxy server. Calls {@link IMqttProxyContext#proceed(message)} to receive the message by
     * the MQTT client or {@link IMqttProxyContext#terminateNow()} to stop receiving.
     * 
     * @param context the context associated with this interceptor
     * @param message the proxy message received
     */
    void onReceive(IMqttProxyContext context, MqttProxyMessage message);

    /**
     * Invoked when sending a message to an MQTT server from an MQTT client, but not yet passed to an MQTT server by the
     * proxy server. Calls {@link IMqttProxyContext#proceed(message)} to send the message to the MQTT server or
     * {@link IMqttProxyContext#terminateNow()} to stop sending.
     * 
     * @param context the context associated with this interceptor
     * @param message the proxy message is being sent
     */
    void onSend(IMqttProxyContext context, MqttProxyMessage message);

}
