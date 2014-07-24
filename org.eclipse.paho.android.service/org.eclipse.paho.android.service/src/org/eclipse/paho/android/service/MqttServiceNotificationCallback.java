package org.eclipse.paho.android.service;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttServiceNotificationCallback {

	public void notify(MqttService service,String topic,MqttMessage message);
}
