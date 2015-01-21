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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * Proxy channel handles both inbound and outbound message.
 */
class MqttProxyChannel implements Runnable, IMqttProxyContext {
    private static final Logger log = Logger.getLogger(MqttProxyChannel.class.getName());
    private final InputStream in;
    private final OutputStream out;
    private final IMqttProxyHandler handler;

    private boolean running = true;
    private IMqttProxyPredicate<MqttProxyMessage> terminateWhen;

    public MqttProxyChannel(IMqttProxyHandler handler, InputStream in, OutputStream out) {
        this.handler = handler;
        this.in = in;
        this.out = out;
    }

    /**
     * Open a channel.
     */
    public static MqttProxyChannel open(IMqttProxyHandler handler, InputStream in, OutputStream out) {
        return new MqttProxyChannel(handler, in, out);
    }

    @Override
    public void run() {
        while (running) {
            try {
                byte[] packet = readPacket();
                MqttProxyMessage message = new MqttProxyMessage(packet);
                if (terminateWhen != null && terminateWhen.test(message)) {
                    terminateNow();
                }
                else {
                    handler.handle(this, message);
                }

                log.finest("Raw MQTT packet: " + bytesToHexString(packet));
            }
            catch (SocketException e) {
                running = false;
            }
            catch (Exception e) {
                log.log(Level.SEVERE, "caught exception:", e);
            }
        }
    }

    @Override
    public synchronized void proceed(MqttWireMessage message) {
        try {
            out.write(message.getHeader());
            out.write(message.getPayload());
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "caught exception:", e);
        }
    }

    @Override
    public synchronized void proceed(byte[] message) {
        try {
            out.write(message);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "caught exception:", e);
        }
    }

    @Override
    public synchronized void terminate(IMqttProxyPredicate<MqttProxyMessage> when) {
        terminateWhen = when;
    }

    @Override
    public synchronized void terminateNow() {
        running = false;
        try {
            in.close();
            out.close();
        }
        catch (Exception e) {
            // ignore
        }
    }

    /**
     * Read MQTT packet from inputStream
     */
    private byte[] readPacket() throws IOException {
        byte header = (byte) in.read();
        MBI mbi = readMBI();

        int packetLength = 1 + mbi.count + mbi.value;
        byte[] buf = new byte[packetLength];
        buf[0] = header;
        System.arraycopy(mbi.data, 0, buf, 1, mbi.count);
        in.read(buf, 1 + mbi.count, mbi.value);
        return buf;
    }

    /**
     * Decodes an MQTT Multi-Byte Integer from the input stream.
     */
    private MBI readMBI() throws IOException {
        List<Byte> data = new ArrayList<Byte>();
        byte digit;
        int msgLength = 0;
        int multiplier = 1;
        int count = 0;
        do {
            digit = (byte) in.read();
            count++;
            data.add(digit);
            msgLength += ((digit & 0x7F) * multiplier);
            multiplier *= 128;
        }
        while ((digit & 0x80) != 0);

        return new MBI(toArray(data), msgLength, count);
    }

    private byte[] toArray(Collection<? extends Number> collection) {
        Object[] boxedArray = collection.toArray();
        int len = boxedArray.length;
        byte[] array = new byte[len];
        for (int i = 0; i < len; i++) {
            array[i] = ((Number) boxedArray[i]).byteValue();
        }
        return array;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        formatter.close();
        return sb.toString();
    }

    private static class MBI {
        // remaining length
        private final int value;
        // the number of bytes read when decoding this MBI.
        private final int count;
        private final byte[] data;

        public MBI(byte[] data, int value, int count) {
            this.data = data;
            this.value = value;
            this.count = count;
        }
    }
}
