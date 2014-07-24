package org.eclipse.paho.android.service;

public interface ServiceNotificationCallbackStore {

	public String getAppServiceNTFCallbackClass(String appPackageName);
	public boolean setAppServiceNTFCallbackClass(String appPackageName,String NTFCallback);
	public void close();
}
