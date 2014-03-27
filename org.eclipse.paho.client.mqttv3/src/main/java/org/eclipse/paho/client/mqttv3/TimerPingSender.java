package org.eclipse.paho.client.mqttv3;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * Default PingSender, uses java.util.Timer to schedule the ping.
 */
public class TimerPingSender implements MqttPingSender {
	// TODO: Add log.
	private ClientComms comms;
	private Timer timer;

	public void init(ClientComms comms) {
		if (comms == null) {
			throw new IllegalArgumentException("ClientComms cannot be null.");
		}
		this.comms = comms;
		timer = new Timer("MQTT Ping: " + comms.getClient().getClientId());
	}

	public void start() {
		//Check ping after first keep alive interval.
		timer.schedule(new PingTask(), comms.getKeepAlive());
	}

	public void stop() {
		timer.cancel();
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		timer.schedule(new PingTask(), delayInMilliseconds);		
	}
	
	class PingTask extends TimerTask {
		public void run() {
			System.out.println("Check time :" + System.currentTimeMillis());
			comms.checkForActivity();			
		}
	}
}
