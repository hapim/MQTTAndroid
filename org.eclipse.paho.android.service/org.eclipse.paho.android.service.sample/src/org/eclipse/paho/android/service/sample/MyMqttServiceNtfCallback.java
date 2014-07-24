package org.eclipse.paho.android.service.sample;

import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.android.service.MqttServiceNotificationCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

//ZHANGYANG
public class MyMqttServiceNtfCallback implements MqttServiceNotificationCallback {

	@Override
	public void notify(MqttService service,String topic,MqttMessage message) {
		
		NotificationCompat.Builder b=new NotificationCompat.Builder(service)
		.setContentTitle("message["+topic+"]")
		.setContentText(new String(message.getPayload()))
		.setSmallIcon(R.drawable.ic_launcher)
		.setAutoCancel(true);
		
		Notification n=b.build();
		
		NotificationManager nm = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(0, n);
	}


}
