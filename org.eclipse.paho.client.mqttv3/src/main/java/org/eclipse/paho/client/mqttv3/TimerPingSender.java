package org.eclipse.paho.client.mqttv3;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * Default PingSender, uses java.util.Timer to schedule the ping.
 */
public class TimerPingSender implements MqttPingSender {
	private ClientComms comms;
	private Timer timer;

	private final static String className = TimerPingSender.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,className); 


	public void init(ClientComms comms) {
		if (comms == null) {
			throw new IllegalArgumentException("ClientComms cannot be null.");
		}
		this.comms = comms;
	}

	public void start() {
		final String methodName = "start";		
		String clientid = comms.getClient().getClientId();
		
		//@Trace 659=start timer for client:{0}
		log.fine(className, methodName, "659", new Object[]{clientid});
				
		timer = new Timer("MQTT Ping: " + clientid);
		//Check ping after first keep alive interval.
		timer.schedule(new PingTask(), comms.getKeepAlive());
	}

	public void stop() {
		final String methodName = "stop";
		//@Trace 661=stop
		log.fine(className, methodName, "661", null);
		timer.cancel();
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		timer.schedule(new PingTask(), delayInMilliseconds);		
	}
	
	class PingTask extends TimerTask {
		private static final String methodName = "PingTask.run";
		
		public void run() {
			//@Trace 660=Check schedule at {0}
			log.fine(className, methodName, "660", new Object[]{new Long(System.currentTimeMillis())});
			comms.checkForActivity();			
		}
	}
}
