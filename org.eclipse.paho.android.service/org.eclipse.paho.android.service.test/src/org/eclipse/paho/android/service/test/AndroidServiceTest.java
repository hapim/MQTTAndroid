package org.eclipse.paho.android.service.test;

/*
 *   <copyright 
 *   notice="oco-source" 
 *   pids="5724-H72," 
 *   years="2010,2012" 
 *   crc="1033636418" > 
 *  IBM Confidential 
 *   
 *  OCO Source Materials 
 *   
 *  5724-H72, 
 *   
 *  (C) Copyright IBM Corp. 2010, 2012 
 *   
 *  The source code for the program is not published 
 *  or otherwise divested of its trade secrets, 
 *  irrespective of what has been deposited with the 
 *  U.S. Copyright Office. 
 *   </copyright> 
 * 
 * Version: %Z% %W% %I% %E% %U%
 */
import java.util.Date;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.test.AndroidTestCase;

import org.eclipse.paho.android.service.MqttClientAndroidService;

import android.util.Log;

/**
 *
 */
public class AndroidServiceTest extends AndroidTestCase {

  private String classCanonicalName = this.getClass().getCanonicalName();
  private String mqttServerURI = TestProperties.serverURI;
  private int waitForCompletionTime = TestProperties.waitForCompletionTime;

  /**
   * Tests that a client can be constructed and that it can connect to and
   * disconnect from the service
   * 
   * @throws Exception
   */

  public void testConnect() throws Exception {
    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testConnect");
      IMqttToken connectToken = null;
      IMqttToken disconnectToken = null;

      connectToken = mqttClient.connect(null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);

      connectToken = mqttClient.connect(null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);
    }
    catch (Exception exception) {
      Assert.fail("Failed:" + "testConnect" + " exception=" + exception);
    }
    finally {
      if (mqttClient != null) {
        mqttClient.close();
      }
    }

  }

  /**
   * Test connection using a remote host name for the local host.
   * 
   * @throws Exception
   */

  public void testRemoteConnect() throws Exception {
    String methodName = "testRemoteConnect";
    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testRemoteConnect");
      IMqttToken connectToken = null;
      IMqttToken subToken = null;
      IMqttDeliveryToken pubToken = null;
      IMqttToken disconnectToken = null;

      connectToken = mqttClient.connect(null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
          null);
      mqttClient.setCallback(mqttV3Receiver);

      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(false);

      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      byte[] payload = ("Message payload " + classCanonicalName + "." + methodName)
          .getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      pubToken.waitForCompletion(waitForCompletionTime);

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);

    }
    catch (Exception exception) {
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      if (mqttClient != null) {
        mqttClient.close();
      }
    }

  }

  /**
   * Test client pubSub using very large messages
   */

  public void testLargeMessage() {

    String methodName = "testLargeMessage";
    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI,
          "testLargeMessage");
      IMqttToken connectToken;
      IMqttToken subToken;
      IMqttToken unsubToken;
      IMqttDeliveryToken pubToken;

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null); //TODO do something about this?
      mqttClient.setCallback(mqttV3Receiver);

      connectToken = mqttClient.connect(null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      int largeSize = 1000;
      String[] topicNames = new String[]{"testLargeMessage" + "/Topic"};
      int[] topicQos = {0};
      byte[] message = new byte[largeSize];

      java.util.Arrays.fill(message, (byte) 's');

      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      unsubToken = mqttClient.unsubscribe(topicNames, null, null);
      unsubToken.waitForCompletion(waitForCompletionTime);

      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null);
      pubToken.waitForCompletion(waitForCompletionTime);

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          message);
      if (!ok) {
        Assert.fail("Receive failed");
      }

    }
    catch (Exception exception) {
      Assert.fail("Failed to instantiate:" + methodName + " exception="
                  + exception);
    }
    finally {
      try {
        IMqttToken disconnectToken;
        disconnectToken = mqttClient.disconnect(null, null);
        disconnectToken.waitForCompletion(waitForCompletionTime);

        mqttClient.close();
      }
      catch (Exception exception) {

      }
    }

  }

  /**
   * Multiple publishers and subscribers.
   */

  public void testMultipleClients() {
	
    int publishers = 2;
    int subscribers = 5;
    String methodName = "testMultipleClients";
    IMqttAsyncClient[] mqttPublisher = new IMqttAsyncClient[publishers];
    IMqttAsyncClient[] mqttSubscriber = new IMqttAsyncClient[subscribers];

    IMqttToken connectToken;
    IMqttToken subToken;
    IMqttDeliveryToken pubToken;
    IMqttToken disconnectToken;

    try {
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};

      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = new MqttClientAndroidService(mContext,
        		mqttServerURI , "MultiPub" + i);

        connectToken = mqttPublisher[i].connect(null, null);
    	Log.i(methodName, "publisher connecting url "+ mqttServerURI +"MultiPub" +i);
        connectToken.waitForCompletion();
      } // for...

      MqttV3Receiver[] mqttV3Receiver = new MqttV3Receiver[mqttSubscriber.length];
      for (int i = 0; i < mqttSubscriber.length; i++) {
        mqttSubscriber[i] = new MqttClientAndroidService(mContext,
        		mqttServerURI, "MultiSubscriber" + i);
        mqttV3Receiver[i] = new MqttV3Receiver(mqttSubscriber[i],
            null);
        mqttSubscriber[i].setCallback(mqttV3Receiver[i]);
        Log.i(methodName,"Assigning callback...");

        connectToken = mqttSubscriber[i].connect(null, null);
        Log.i(methodName, "subscriber connecting url "+mqttServerURI +"MultiSubscriber" +i);
        
        connectToken.waitForCompletion();

        subToken = mqttSubscriber[i].subscribe(topicNames, topicQos, null, null);
        Log.i(methodName,"subscribe "+topicNames[0].toString() + " QoS is "+topicQos[0]);
        subToken.waitForCompletion();
      } // for...

      for (int iMessage = 0; iMessage < 2; iMessage++) {
        byte[] payload = ("Message " + iMessage).getBytes();
        for (int i = 0; i < mqttPublisher.length; i++) {
          pubToken = mqttPublisher[i].publish(topicNames[0], payload, 0, false,
              null, null);
          Log.i(methodName,"publish to "+topicNames[0]+" payload is "+payload.toString());
          
          pubToken.waitForCompletion();
        }
        
        TimeUnit.MILLISECONDS.sleep(30000);
        
        for (int i = 0; i < mqttSubscriber.length; i++) {
          for (int ii = 0; ii < mqttPublisher.length; ii++) {
        	Log.i(methodName, "validate time = "+new Date().toString());
            boolean ok = mqttV3Receiver[i].validateReceipt(
                topicNames[0], 0, payload);
            
            if (!ok) {
              Assert.fail("Receive failed");
            }
          } // for publishers...
        } // for subscribers...
      } // for messages...

    }
    catch (Exception exception) {

      Assert.fail("Failed to instantiate:" + methodName + " exception="
                  + exception);
    }
    finally {
      try {
        for (int i = 0; i < mqttPublisher.length; i++) {
          disconnectToken = mqttPublisher[i].disconnect(null, null);
          disconnectToken.waitForCompletion();
          mqttPublisher[i].close();
        }
        for (int i = 0; i < mqttSubscriber.length; i++) {
          disconnectToken = mqttSubscriber[i].disconnect(null, null);
          disconnectToken.waitForCompletion();
          mqttSubscriber[i].close();
        }
      }
      catch (Exception exception) {

      }
    }

  }

  /**
   * Test non durable subscriptions.
   */

//  public void testNonDurableSubs() {
//    String methodName = "testNonDurableSubs";
//
//    IMqttAsyncClient mqttClient = null;
//
//    IMqttToken connectToken;
//    IMqttToken subToken;
//    IMqttToken unsubToken;
//    IMqttDeliveryToken pubToken;
//    IMqttToken disconnectToken;
//
//    try {
//      mqttClient = new MqttClientAndroidService(mContext, serverURI,
//          "testNonDurableSubs");
//      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
//          null);
//      mqttClient.setCallback(mqttV3Receiver);
//      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
//      // Clean session true is the default and implies non durable
//      // subscriptions.
//      mqttConnectOptions.setCleanSession(true);
//      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
//      connectToken.waitForCompletion(10000);
//
//      String[] topicNames = new String[]{methodName + "/Topic"};
//      int[] topicQos = {2};
//      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
//      subToken.waitForCompletion(10000);
//
//      byte[] payloadNotRetained = ("Message payload "
//                                   + classCanonicalName + "." + methodName + " not retained")
//          .getBytes();
//      pubToken = mqttClient.publish(topicNames[0], payloadNotRetained, 2, false,
//          null, null);
//      pubToken.waitForCompletion(100000);
//
//      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//          payloadNotRetained);
//      if (!ok) {
//        Assert.fail("Receive failed");
//      }
//
//      // Retained publications.
//      // ----------------------
//      byte[] payloadRetained = ("Message payload " + classCanonicalName
//                                + "." + methodName + " retained").getBytes();
//      pubToken = mqttClient.publish(topicNames[0], payloadRetained, 2, true,
//          null, null);
//      pubToken.waitForCompletion(10000);
//
//      ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//          payloadRetained);
//      if (!ok) {
//        Assert.fail("Receive failed");
//      }
//
//      // Check that unsubscribe and re subscribe resends the publication.
//      unsubToken = mqttClient.unsubscribe(topicNames, null, null);
//      unsubToken.waitForCompletion(10000);
//
//      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
//      subToken.waitForCompletion(10000);
//
//      ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//          payloadRetained);
//      if (!ok) {
//        Assert.fail("Receive failed");
//      }
//
//      // Check that subscribe without unsubscribe receives the
//      // publication.
//      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
//      subToken.waitForCompletion(10000);
//      ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//          payloadRetained);
//      if (!ok) {
//        Assert.fail("Receive failed");
//      }
//
//      // Disconnect, reconnect and check that the retained publication is
//      // still delivered.
//      disconnectToken = mqttClient.disconnect(null, null);
//      disconnectToken.waitForCompletion(10000);
//
//      mqttClient.close();
//
//      mqttClient = new MqttClientAndroidService(mContext, serverURI,
//          "testNonDurableSubs");
//
//      mqttV3Receiver = new MqttV3Receiver(mqttClient,
//          null);
//      mqttClient.setCallback(mqttV3Receiver);
//
//      mqttConnectOptions = new MqttConnectOptions();
//      mqttConnectOptions.setCleanSession(true);
//      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
//      connectToken.waitForCompletion(1000);
//
//      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
//      subToken.waitForCompletion(1000);
//
//      ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//          payloadRetained);
//      if (!ok) {
//        Assert.fail("Receive failed");
//      }
//
//    }
//    catch (Exception exception) {
//
//      Assert.fail("Failed:" + methodName + " exception=" + exception);
//    }
//    finally {
//      try {
//        disconnectToken = mqttClient.disconnect(null, null);
//        disconnectToken.waitForCompletion(1000);
//
//        mqttClient.close();
//      }
//      catch (Exception exception) {
//
//      }
//    }
//
//  }

  /**
   * Test that QOS values are preserved between MQTT publishers and
   * subscribers.
   */

  public void testQoSPreserved() {

    IMqttAsyncClient mqttClient = null;

    IMqttToken connectToken;
    IMqttToken subToken;
    IMqttDeliveryToken pubToken;
    IMqttToken disconnectToken;
    String methodName = "testQoSPreserved";

    try {
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testQoSPreserved");
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
          null);
      mqttClient.setCallback(mqttV3Receiver);

      connectToken = mqttClient.connect(null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      String[] topicNames = new String[]{methodName + "/Topic0",
          methodName + "/Topic1", methodName + "/Topic2"};
      int[] topicQos = {0, 1, 2};
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      for (int i = 0; i < topicNames.length; i++) {
        byte[] message = ("Message payload " + classCanonicalName + "."
                          + methodName + " " + topicNames[i]).getBytes();
        for (int iQos = 0; iQos < 3; iQos++) {
          pubToken = mqttClient.publish(topicNames[i], message, iQos, false,
              null, null);
          pubToken.waitForCompletion(waitForCompletionTime);

          boolean ok = mqttV3Receiver.validateReceipt(topicNames[i],
              Math.min(iQos, topicQos[i]), message);
          if (!ok) {
            Assert.fail("Receive failed sub Qos=" + topicQos[i]
                        + " PublishQos=" + iQos);
          }
        }
      }
    }
    catch (Exception exception) {

      Assert.fail("Failed:" + methodName + " exception=" + exception);

    }
    finally {
      try {
        disconnectToken = mqttClient.disconnect(null, null);
        disconnectToken.waitForCompletion(waitForCompletionTime);

        mqttClient.close();
      }
      catch (Exception exception) {

      }
    }

  }

  /**
   * Test the behaviour of the cleanStart flag, used to clean up before
   * re-connecting.
   */
  public void testCleanStart() {

    IMqttAsyncClient mqttClient = null;

    IMqttToken connectToken;
    IMqttToken subToken;
    IMqttDeliveryToken pubToken;
    IMqttToken disconnectToken;

    String methodName = "testCleanStart";
    try {
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testCleanStart");
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
          null);
      mqttClient.setCallback(mqttV3Receiver);

      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      // Clean start: true - The broker cleans up all client state,
      // including subscriptions, when the client is disconnected.
      // Clean start: false - The broker remembers all client state,
      // including subscriptions, when the client is disconnected.
      // Matching publications will get queued in the broker whilst the
      // client is disconnected.
      // For Mqtt V3 cleanSession=false, implies new subscriptions are
      // durable.
      mqttConnectOptions.setCleanSession(false);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      byte[] payload = ("Message payload " + classCanonicalName + "."
                        + methodName + " First").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      pubToken.waitForCompletion(waitForCompletionTime);

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      // Disconnect and reconnect to make sure the subscription and all
      // queued messages are cleared.
      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);

      mqttClient.close();

      // Send a message from another client, to our durable subscription.
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testCleanStart" + "Other");
      mqttV3Receiver = new MqttV3Receiver(mqttClient,
          null);
      mqttClient.setCallback(mqttV3Receiver);

      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      // Receive the publication so that we can be sure the first client
      // has also received it.
      // Otherwise the first client may reconnect with its clean session
      // before the message has arrived.
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      subToken.waitForCompletion(waitForCompletionTime);

      payload = ("Message payload " + classCanonicalName + "."
                 + methodName + " Other client").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      pubToken.waitForCompletion(waitForCompletionTime);

      ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      disconnectToken = mqttClient.disconnect(null, null);
      disconnectToken.waitForCompletion(waitForCompletionTime);

      mqttClient.close();

      // Reconnect and check we have no messages.
      mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, "testCleanStart");
      mqttV3Receiver = new MqttV3Receiver(mqttClient,
          null);
      mqttClient.setCallback(mqttV3Receiver);

      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      connectToken.waitForCompletion(waitForCompletionTime);

      MqttV3Receiver.ReceivedMessage receivedMessage = mqttV3Receiver
          .receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:"
                    + new String(receivedMessage.message.getPayload()));
      }

      // Also check that subscription is cancelled.
      payload = ("Message payload " + classCanonicalName + "."
                 + methodName + " Cancelled Subscription").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      pubToken.waitForCompletion(waitForCompletionTime);

      receivedMessage = mqttV3Receiver.receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:"
                    + new String(receivedMessage.message.getPayload()));
      }
    }
    catch (Exception exception) {

      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        disconnectToken = mqttClient.disconnect(null, null);
        disconnectToken.waitForCompletion(waitForCompletionTime);

        mqttClient.close();
      }
      catch (Exception exception) {

      }
    }

  }
  //	/** Oringally commented out from the fv test version
  //	 * Tests that invalid clientIds cannot connect.
  //	 * 
  //	 * @throws Exception
  //	 */
  //	@Test
  //	public void testBadClientId() throws Exception {
  //		final String methodName = Utility.getMethodName();
  //		Log.banner(logger, cclass, methodName);
  //		logger.entering(classCanonicalName, methodName);
  //
  //		// Client ids with length errors are now trapped by the client
  //		// implementation.
  //		// String[] clientIds = new
  //		// String[]{"","Minus-ClientId","123456789012345678901234"};
  //		String[] clientIds = new String[] { "Minus-ClientId" };
  //		IMqttAsyncClient mqttClient = null;
  //		IMqttToken connectToken ;
  //		IMqttToken disconnectToken;
  //
  //		for (String clientId : clientIds) {
  //			try {
  //				mqttClient = new MqttClientAndroidService(mContext, serverURI, "testConnect");
  //						clientId);
  //
  //				try {
  //					connectToken = mqttClient.connect(null, null);
  //					connectToken.waitForCompletion(1000);
  //					connectToken.reset();
  //
  //					disconnectToken = mqttClient.disconnect(null, null);
  //					disconnectToken.waitForCompletion(1000);
  //					disconnectToken.reset();
  //
  //					Assert.fail("We shouldn't have been able to connect!");
  //				} catch (MqttException exception) {
  //					// This is the expected exception.
  //					logger.fine("We expect an exception because we used an invalid client id");
  //					// logger.log(Level.SEVERE, "caught exception:", exception);
  //				}
  //			} catch (Exception exception) {
  //				logger.fine("Failed:" + methodName + " exception="
  //						+ exception.getClass().getName() + "."
  //						+ exception.getMessage());
  //				logger.exiting(classCanonicalName, methodName,
  //						new Object[] { exception });
  //				throw exception;
  //			}
  //		}
  //
  //		logger.exiting(classCanonicalName, methodName);
  //	}

  public void testPubSub(){
	  
	  String methodName = "testPubSub";
	  IMqttAsyncClient mqttClient = null;
	  try {
	    mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, methodName);
	    IMqttToken connectToken;
	    IMqttToken subToken;
	    IMqttDeliveryToken pubToken;

	    MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null);
	    mqttClient.setCallback(mqttV3Receiver);

	    connectToken = mqttClient.connect(null, null);
	    connectToken.waitForCompletion(waitForCompletionTime);

	    String[] topicNames = new String[]{"testPubSub" + "/Topic"};
	    int[] topicQos = {0};
	    MqttMessage mqttMessage = new MqttMessage("message for testPubSub".getBytes());
	    byte[] message = mqttMessage.getPayload();

	    subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
	    subToken.waitForCompletion(waitForCompletionTime);

	    pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null);
	    pubToken.waitForCompletion(waitForCompletionTime);

	    TimeUnit.MILLISECONDS.sleep(3000);
	    
	    boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
	    if (!ok) {
	        Assert.fail("Receive failed");
	      }

	    }
	    catch (Exception exception) {
	      Assert.fail("Failed to instantiate:" + methodName + " exception="
	                  + exception);
	    }
	    finally {
	      try {
	        IMqttToken disconnectToken;
	        disconnectToken = mqttClient.disconnect(null, null);
	        disconnectToken.waitForCompletion(waitForCompletionTime);

	        mqttClient.close();
	      }
	      catch (Exception exception) {

	      }
	    }

  }

  public void testHAConnect() throws Exception{
	
	String methodName = "testHAConnect";

	IMqttAsyncClient client = null;
	try {
	     try {
	    	 	String junk = "tcp://junk:123";
	    	 	client = new MqttClientAndroidService(mContext, junk, methodName);

	    	 	String[] urls = new String[]{"tcp://junk", mqttServerURI};

	    	 	MqttConnectOptions options = new MqttConnectOptions();
	    	 	options.setServerURIs(urls);

	    	 	Log.i(methodName,"HA connect");
	    	 	IMqttToken connectToken = client.connect(options);
	    	 	connectToken.waitForCompletion();

	    	 	Log.i(methodName,"HA diconnect");
	    	 	IMqttToken disconnectToken = client.disconnect(null, null);
	    	 	disconnectToken.waitForCompletion();
	    	 	
	    	 	Log.i(methodName,"HA success");
	      }
	      catch (Exception e) {

	    	  	e.printStackTrace();
	    	  	throw e;
	      }
	    }
	    finally {
	      if (client != null) {
	        client.close();
	      }
	    }	  
  }

  public void testRetainedMessage(){
	  
	  String methodName = "testRetainedMessage";
	  IMqttAsyncClient mqttClient = null;
	  IMqttAsyncClient mqttClientRetained = null;
	  IMqttToken disconnectToken = null;
	  
	  try {
	    mqttClient = new MqttClientAndroidService(mContext, mqttServerURI, methodName);
	    IMqttToken connectToken;
	    IMqttToken subToken;
	    IMqttDeliveryToken pubToken;

	    MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null);
	    mqttClient.setCallback(mqttV3Receiver);

	    connectToken = mqttClient.connect(null, null);
	    connectToken.waitForCompletion(waitForCompletionTime);

	    String[] topicNames = new String[]{"testRetainedMessage" + "/Topic"};
	    int[] topicQos = {0};
	    MqttMessage mqttMessage = new MqttMessage("message for testPubSub".getBytes());
	    byte[] message = mqttMessage.getPayload();

	    subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
	    subToken.waitForCompletion(waitForCompletionTime);

	    pubToken = mqttClient.publish(topicNames[0], message, 0, true, null, null);
	    pubToken.waitForCompletion(waitForCompletionTime);

	    TimeUnit.MILLISECONDS.sleep(3000);
	    
	    boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
	    if (!ok) {
	        Assert.fail("Receive failed");
	      }
	    
	    Log.i(methodName, "First client received message successfully");
	    
	    disconnectToken = mqttClient.disconnect(null, null);
	    disconnectToken.waitForCompletion();
	    mqttClient.close();
	    
	    mqttClientRetained = new MqttClientAndroidService(mContext, mqttServerURI, "Retained");
	   
	    Log.i(methodName, "New MqttClientAndroidService mqttClientRetained");
	    
	    MqttV3Receiver mqttV3ReceiverRetained = new MqttV3Receiver(mqttClientRetained, null);
	    mqttClientRetained.setCallback(mqttV3ReceiverRetained);
	    
	    Log.i(methodName, "Assigning callback...");

	    connectToken = mqttClientRetained.connect(null, null);
	    connectToken.waitForCompletion();
	    
	    Log.i(methodName, "Connect to mqtt server");
	    
	    subToken = mqttClientRetained.subscribe(topicNames, topicQos, null, null);
	    subToken.waitForCompletion();
	    
	    Log.i(methodName, "subscribe "+topicNames[0].toString() + " QoS is " + topicQos[0]);
	    
	    TimeUnit.MILLISECONDS.sleep(3000);
	    
	    ok = mqttV3ReceiverRetained.validateReceipt(topicNames[0], 0, message);
	    if (!ok) {
	        Assert.fail("Receive retained message failed");
	      }

	    Log.i(methodName, "Second client received message successfully");
	    
	    disconnectToken = mqttClientRetained.disconnect(null, null);
	    disconnectToken.waitForCompletion();
	    mqttClientRetained.close();
	    
	    }
	    catch (Exception exception) {
	      Assert.fail("Failed to instantiate:" + methodName + " exception="
	                  + exception);
	    }

  }
  
//  public void testBroadcastReceiver(){
//	  
//  }
} // AndroidServiceTest.