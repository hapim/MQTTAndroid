package org.eclipse.paho.android.service;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Ping Sender based on AlarmManager
 */
public class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	static final String TAG = "AlarmPingSender";

	// TODO: Add log.
	private ClientComms comms;
	private MqttService service;
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.service = service;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		String action = MqttServiceConstants.PING_SENDER
				+ comms.getClient().getClientId();
		Log.d(TAG, "Register alarmreceiver to MqttService");
		service.registerReceiver(alarmReceiver, new IntentFilter(action));

		pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(
				action), PendingIntent.FLAG_UPDATE_CURRENT);

		schedule(comms.getKeepAlive());
	}

	@Override
	public void stop() {
		// Cancel Alarm.
		AlarmManager alarmManager = (AlarmManager) service
				.getSystemService(Service.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);

		Log.d(TAG, "Unregister alarmreceiver to MqttService");
		service.unregisterReceiver(alarmReceiver);
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		AlarmManager alarmManager = (AlarmManager) service
				.getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
				pendingIntent);
	}

	/*
	 * This class sends PingReq package to MQTT broker
	 */
	class AlarmReceiver extends BroadcastReceiver {
		private WakeLock wakelock;
		private String wakeLockTag = MqttServiceConstants.PING_WAKELOCK
				+ that.comms.getClient().getClientId();

		@Override
		public void onReceive(Context context, Intent intent) {
			// According to the docs, "Alarm Manager holds a CPU wake lock as
			// long as the alarm receiver's onReceive() method is executing.
			// This guarantees that the phone will not sleep until you have
			// finished handling the broadcast.", but this class still get
			// a wake lock to wait for ping finished.
			int count = intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, -1);
			Log.d(TAG, "Ping " + count + " times.");

			Log.d(TAG, "Check time :" + System.currentTimeMillis());
			IMqttToken token = comms.checkForActivity();

			// No ping has been sent.
			if (token == null) {
				return;
			}

			// Assign new callback to token to execute code after PingResq
			// arrives. Get another wakelock even receiver already has one,
			// release it until ping response returns.
			if (wakelock == null) {
				PowerManager pm = (PowerManager) service
						.getSystemService(Service.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
						wakeLockTag);
			}
			wakelock.acquire();
			token.setActionCallback(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					wakelock.release();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken,
						Throwable exception) {
					Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					wakelock.release();
				}
			});
		}
	}
}