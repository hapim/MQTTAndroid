/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 *    Ian Craggs - MQTT 3.1.1 support
 */
package org.eclipse.paho.client.mqttv3.internal.wire;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * An on-the-wire representation of an MQTT CONNECT message.
 */
public class MqttConnect extends MqttWireMessage {

	public static final String KEY = "Con";
    private static final String MQTT_V31_PROTOCOL_NAME = "MQIsdp";
    private static final String MQTT_V311_PROTOCOL_NAME = "MQTT";

	private String clientId;
	private boolean cleanSession;
	private MqttMessage willMessage;
	private String userName;
	private char[] password;
	private int keepAliveInterval;
	private String willDestination;
	private int mqttVersion;
	
	/**
	 * Constructor for an on the wire MQTT connect message
	 * 
	 * @param info
	 * @param data
	 * @throws IOException
	 * @throws MqttException
	 */
	public MqttConnect(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);

        int decodedVersion = 0;
        String protocolName = decodeUTF8(dis);
        if (protocolName.equals(MQTT_V311_PROTOCOL_NAME)) {
            decodedVersion = 4;
        }

        if (protocolName.equals(MQTT_V31_PROTOCOL_NAME)) {
            decodedVersion = 3;
        }

        mqttVersion = dis.readByte();
        if (mqttVersion != decodedVersion) {
            throw new IllegalStateException("Invalid protocol name or version!");
        }

        byte connectFlags = dis.readByte();

        boolean hasUsername = ((connectFlags & 0x80) >> 7) == 1;
        boolean hasPassword = ((connectFlags & 0x40) >> 6) == 1;
        boolean willRetain = ((connectFlags & 0x20) >> 5) == 1;
        int willQoS = (connectFlags & 0x18) >> 3;
        boolean hasWill = ((connectFlags & 0x04) >> 2) == 1;
        cleanSession = ((connectFlags & 0x02) >> 1) == 1;

        keepAliveInterval = dis.readUnsignedShort();
        clientId = decodeUTF8(dis);

        // will
        if (hasWill) {
            willDestination = decodeUTF8(dis);
            int dataLen = dis.readUnsignedShort();
            byte[] willPayload = new byte[dataLen];
            dis.readFully(willPayload);
            willMessage = new MqttMessage();
            willMessage.setPayload(willPayload);
            willMessage.setQos(willQoS);
            willMessage.setRetained(willRetain);
        }

        // user & password
        if (hasUsername) {
            userName = decodeUTF8(dis);
        }

        if (hasPassword) {
            int dataLen = dis.readUnsignedShort();
            byte[] passwordBytes = new byte[dataLen];
            dis.readFully(passwordBytes);
            password = new String(passwordBytes, "UTF-8").toCharArray();
        }

		dis.close();
	}

	public MqttConnect(String clientId, int mqttVersion, boolean cleanSession, int keepAliveInterval, String userName, char[] password, MqttMessage willMessage, String willDestination) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.willMessage = willMessage;
		this.willDestination = willDestination;
		this.mqttVersion = mqttVersion;
	}

	public String toString() {
		String rc = super.toString();
		rc += " clientId " + clientId + " keepAliveInterval " + keepAliveInterval;
		return rc;
	}
	
	protected byte getMessageInfo() {
		return (byte) 0;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}
	
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			if (mqttVersion == 3) {
                encodeUTF8(dos, MQTT_V31_PROTOCOL_NAME);	
			}
			else if (mqttVersion == 4) {
                encodeUTF8(dos, MQTT_V311_PROTOCOL_NAME);	
			}
			dos.write(mqttVersion);

			byte connectFlags = 0;
			
			if (cleanSession) {
				connectFlags |= 0x02;
			}
			
			if (willMessage != null ) {
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos()<<3);
				if (willMessage.isRetained()) {
					connectFlags |= 0x20;
				}
			}
			
			if (userName != null) {
				connectFlags |= 0x80;
				if (password != null) {
					connectFlags |= 0x40;
				}
			}
			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);
			dos.flush();
			return baos.toByteArray();
		} catch(IOException ioe) {
			throw new MqttException(ioe);
		}
	}
	
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos,clientId);
			
			if (willMessage != null) {
				encodeUTF8(dos,willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}
			
			if (userName != null) {
				encodeUTF8(dos,userName);
				if (password != null) {
					encodeUTF8(dos,new String(password));
				}
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired() {
		return false;
	}
	
	public String getKey() {
		return KEY;
	}
}
