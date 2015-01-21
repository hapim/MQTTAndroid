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
 * Internal proxy handler.
 */
interface IMqttProxyHandler {

    /**
     * Handle the proxy message.
     * 
     * @param context
     * @param message
     */
    void handle(IMqttProxyContext context, MqttProxyMessage message);

}
