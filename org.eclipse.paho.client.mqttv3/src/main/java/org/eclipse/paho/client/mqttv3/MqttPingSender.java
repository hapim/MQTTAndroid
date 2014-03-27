package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;

public interface MqttPingSender {

	public void init(ClientComms comms);

	public void start();
	
	public void stop();
	
	public void schedule(long delayInMilliseconds);
	
}
