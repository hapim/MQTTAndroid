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

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * The proxy context that user can control the inbound messages received and outbound messages send to the server.
 */
public interface IMqttProxyContext {

    /**
     * Proceed with the proxy message flow with the given {@code message} as an input.
     * 
     * @param message
     */
    void proceed(MqttWireMessage message);

    /**
     * Proceed with the proxy message flow with the given {@code message} as an input.
     * 
     * @param message
     */
    void proceed(byte[] message);

    /**
     * Terminate the message flow when the condition satisfies.
     * 
     * @param when the condition to terminate the message flow
     */
    void terminate(IMqttProxyPredicate<MqttProxyMessage> when);

    /**
     * Terminate the message flow now.
     */
    void terminateNow();
}
