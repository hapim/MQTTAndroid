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
 * Represents a predicate (boolean-valued function) of one argument.
 * 
 * @param <T> the type of the input to the predicate
 */
public interface IMqttProxyPredicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param input the input argument
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    boolean test(T input);
}
