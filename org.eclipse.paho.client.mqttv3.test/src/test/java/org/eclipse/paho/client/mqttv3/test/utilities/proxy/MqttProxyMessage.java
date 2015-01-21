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

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * Proxy message with raw MQTT packet data.
 */
public final class MqttProxyMessage {
    private final byte[] data;
    private final MqttWireMessage message;

    MqttProxyMessage(byte[] data) throws MqttException {
        this.data = data.clone();
        this.message = MqttWireMessage.createWireMessage(this.data);
    }

    /**
     * Returns the raw MQTT packet data.
     * 
     * @return the raw MQTT packet data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the {@code MqttWireMessage} instead of the raw data.
     * 
     * @return the wire message
     */
    public MqttWireMessage getWireMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message.toString();
    }
}
