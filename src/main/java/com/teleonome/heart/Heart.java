package com.teleonome.heart;


import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import io.moquette.broker.Server;
//import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.broker.config.IConfig;
//import io.moquette.server.config.IResourceLoader;
import io.moquette.broker.config.MemoryConfig;
//import io.moquette.server.config.ResourceLoaderConfig;
//import com.teleonome.heart.PublisherListener;
/**
 * Hello world!
 * 
 */
public class Heart 
{
	public final static String BUILD_NUMBER="05/08/2026 07:12";

	Logger logger;
	int heartPid=0;
	 MqttAsyncClient client=null;;
	 PublisherListener aPublisherListener;
	 private PostgresqlPersistenceManager aDBManager=null;
	 private Server mqttBroker;
	 
	public Heart() {

		String processName = ManagementFactory.getRuntimeMXBean().getName();

		String fileName =  "/home/pi/Teleonome/lib/Log4J.properties";
		System.out.println("reading log4j file at " + fileName);
		PropertyConfigurator.configure(fileName);
		logger = Logger.getLogger(getClass());

		try {
			logger.warn("line 78");
			FileUtils.writeStringToFile(new File("/home/pi/Teleonome/heart/HeartProcess.info"), processName);
			logger.warn("line 80");
			heartPid = Integer.parseInt(processName.split("@")[0]);
			logger.warn("line 82, heartPid=" + heartPid);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
		}
		logger.warn("line 85");
		try {
			FileUtils.writeStringToFile(new File("/home/pi/Teleonome/heart/HeartBuild.info"), BUILD_NUMBER);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		logger.warn("Heart build " + BUILD_NUMBER + " ProcessNumber:" + heartPid);

		//int qos             = 2;
		//String broker       = "tcp://localhost:1883";
		//        MemoryPersistence persistence = new MemoryPersistence();

		try {
			// Creating a MQTT Broker using Moquette
			//    		final IConfig classPathConfig = new ClasspathConfig();
			//    		final Server mqttBroker = new Server();
			//    		final List<? extends InterceptHandler> userHandlers = Arrays.asList(new PublisherListener());
			//    		mqttBroker.startServer(classPathConfig, userHandlers);
			//    		

			Properties configProps = new Properties();

			configProps.put("port", Integer.toString(1883));
			configProps.put("websocket_port", Integer.toString(9999));

			configProps.put("host", "0.0.0.0");
			configProps.put("allow_anonymous", Boolean.valueOf(true));
			 String dbName = System.getProperty("user.dir")+  File.separator  + "heart.mapdb";
		     configProps.setProperty(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME,String.valueOf(1536*1000));
			//configProps.put(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME,dbName);
			configProps.put(BrokerConstants.PERSISTENCE_ENABLED_PROPERTY_NAME, "false");
			IConfig config = new MemoryConfig(configProps);
			//IResourceLoader classpathLoader = new ClasspathResourceLoader();
			//final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
			logger.warn("line 124 before dbamanager");
			aDBManager = PostgresqlPersistenceManager.instance();
			logger.warn("line 126 after dbamanager");
//			// 	        
//			//
//			// start a client that will receive updates fr the updates from the heart
//			//
//			String broker = "tcp://0.0.0.0:1883";
//			
//			
//			try {
//				client = new MqttAsyncClient(broker, MqttAsyncClient.generateClientId(), new MqttDefaultFilePersistence());
//				MyMQTTListener aMyMQTTListener = new MyMQTTListener();
//	            client.connect(null,aMyMQTTListener );
//	            MyCallBack aMyCallBack = new MyCallBack();
//	            client.setCallback(aMyCallBack);
//			} catch (MqttException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
			logger.warn("line 145 about to publisher");
			
			aPublisherListener = new PublisherListener();
			logger.warn("line 147 about to publisher");
			List<? extends InterceptHandler> userHandlers = Collections.singletonList(aPublisherListener);
			logger.warn("line 149 about to publisher");
			mqttBroker = new Server();
			mqttBroker.startServer(config, userHandlers);
			logger.warn("line 152 started server");
			PingThread aPingThread = new PingThread();
			aPingThread.start();
			
			


			logger.warn("moquette mqtt broker started without client1, press ctrl-c to shutdown..");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					ArrayList results;
					logger.warn("stopping moquette mqtt broker without restarting..");
					mqttBroker.stopServer();
					logger.warn("moquette mqtt broker stopped");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
//					try {
//						logger.warn("about to restart heart");
//						results = Utils.executeCommand("/home/pi/Teleonome/heart/StartHeartBG.sh");
//						//Runtime.getRuntime().exec("sudo sh /home/pi/Teleonome/heart/StartHeartBG.sh");
//						String data = "Restart hear command response="  +String.join(", ", results);
//						logger.warn( data);
//						
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						logger.warn(Utils.getStringException(e));
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					logger.warn( "About to ezit the bad instance of the heart");
					
				}
			});

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			//
			// if we are here it probably means that the mapdb got corrupted so delete them
			File f = new File("/home/pi/Teleonome/heart/heart.mapdb");
			if(f.isFile())f.delete();
			f = new File("/home/pi/Teleonome/heart/heart.mapdb.p");
			if(f.isFile())f.delete();
			f = new File("/home/pi/Teleonome/heart/heart.mapdb.t");
			if(f.isFile())f.delete();
//			try {
//				Utils.executeCommand("sudo reboot");
//			} catch (IOException | InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			
		}

	}

	class PingThread extends Thread{

	    public PingThread(){
	        setDaemon(true);
	    }
	    //
	    // Self-reported leading indicator of the Metaspace/GC pressure that has
	    // been the trigger behind Heart's publish-freeze incidents (chronic
	    // classloading churn from Moquette's connection handling, proportional
	    // to connection-churn volume -- see conversation 2026-08-05). Read
	    // in-process via JMX rather than shelling out to jstat, so no sudo is
	    // needed and Medula (a separate process) can see it just by reading
	    // HeartPing.info like everything else here.
	    //
	    private double getMetaspacePercentUsed() {
	    	for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
	    		if (pool.getName().equals("Metaspace")) {
	    			MemoryUsage usage = pool.getUsage();
	    			long max = usage.getMax();
	    			if (max <= 0) max = usage.getCommitted();
	    			if (max <= 0) return -1;
	    			return (usage.getUsed() * 100.0) / max;
	    		}
	    	}
	    	return -1;
	    }
	    private long getFullGcCount() {
	    	long count = 0;
	    	boolean found = false;
	    	for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
	    		// old-generation / major collectors correspond to what jstat's FGC
	    		// column reports, across the common collectors (G1 Old Generation,
	    		// PS MarkSweep, ConcurrentMarkSweep)
	    		String name = gc.getName().toLowerCase();
	    		if (name.contains("old") || name.contains("marksweep")) {
	    			found = true;
	    			long c = gc.getCollectionCount();
	    			if (c > 0) count += c;
	    		}
	    	}
	    	return found ? count : -1;
	    }
	    //
	    // Broadcast the same ping content Medula reads from HeartPing.info onto
	    // the broker itself, so the webapp's live Heart modal can show it too --
	    // not just an external process polling a file on disk. Published via
	    // Server.internalPublish(), Moquette's own API for a broker-embedding
	    // application to inject a message without a separate client connection
	    // (so this can never hit the Paho publish-thread issue fixed elsewhere
	    // today -- there's no client involved at all here).
	    //
	    private void broadcastHealthStatus(String topic, JSONObject payload) {
	    	try {
	    		byte[] payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
	    		MqttPublishMessage msg = MqttMessageBuilders.publish()
	    				.topicName(topic)
	    				.qos(MqttQoS.AT_MOST_ONCE)
	    				.retained(false)
	    				.payload(Unpooled.wrappedBuffer(payloadBytes))
	    				.build();
	    		mqttBroker.internalPublish(msg, "Heart");
	    	} catch (Exception e) {
	    		logger.warn("Failed to broadcast " + topic + ": " + Utils.getStringException(e));
	    	}
	    }
	    public void run(){
	        while(true) {
	        	logger.warn("Heart Ping ");
	        	try {
	        		double heartAvailableMemory = Runtime.getRuntime().freeMemory()/1024000;
					double heartMaxMemory = Runtime.getRuntime().maxMemory()/1024000;
					JSONObject pingInfo = aPublisherListener.getStatsJSON();
					pingInfo.put(TeleonomeConstants.HEART_PROCESS_AVAILABLE_MEMORY, heartAvailableMemory);
					pingInfo.put(TeleonomeConstants.HEART_PROCESS_MAXIMUM_MEMORY, heartMaxMemory);
					pingInfo.put(TeleonomeConstants.DATATYPE_TIMESTAMP_MILLISECONDS, System.currentTimeMillis());
					//
					// direct thread-liveness check on Moquette's session event loops --
					// a dead loop here is permanent (Moquette never restarts one) and
					// means every client hashed to that shard is silently unresponsive,
					// even though the broker process itself (and this ping) looks fine
					//
					pingInfo.put("sessionEventLoopCount", mqttBroker.getSessionEventLoopCount());
					pingInfo.put("deadSessionEventLoopCount", mqttBroker.getDeadSessionEventLoopCount());
					pingInfo.put(TeleonomeConstants.HEART_METASPACE_PERCENT_USED, getMetaspacePercentUsed());
					pingInfo.put(TeleonomeConstants.HEART_FULL_GC_COUNT, getFullGcCount());

	    			FileUtils.writeStringToFile(new File("HeartPing.info"), pingInfo.toString());
	    			broadcastHealthStatus(TeleonomeConstants.HEART_TOPIC_HEART_HEALTH_STATUS, pingInfo);
	    			
	    			
	    		//	String webPid = Integer.parseInt(processName.split("@")[0]);
	    			try {
						Thread.sleep(1000*60);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		} catch (IOException e1) {
	    			// TODO Auto-generated catch block
	    			e1.printStackTrace();
	    		}
	        }
	    }
	}  
	    
	    
//	class MyCallBack implements  MqttCallback {
//        @Override
//        public void connectionLost(Throwable cause) {
//
//        }
//
//        @Override
//        public void messageArrived(String topic, MqttMessage message) throws Exception {
//        	logger.warn("Received message on topic: " + topic + " -> " + new String(message.getPayload()));
//        	if(topic.equals(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_REQUEST)) {
//        		
//    				
//        	}
//        }

//        
//      
//    	
//        
//        
//        public void blink() {
//        	 try {
//				 //
//				 // get the type of computer, because the command is different
//				 //
//        		 String computerModel = aPublisherListener.getComputerModel();
//				if(computerModel.equals(TeleonomeConstants.COMPUTER_MODEL_PI_3_MODEL_B)) {
//					Runtime.getRuntime().exec("sudo sh BlinkPi3.sh");
//				}else {
//					Runtime.getRuntime().exec("sudo sh Blink.sh");
//				}
//					
//				} catch (IOException e) {
//					// TODO Auto-gnanenerated catch block
//					e.printStackTrace();
//				}
//			 
//        }
//        
//        @Override
//        public void deliveryComplete(IMqttDeliveryToken token) {
//
//        }
//
//    }

	
//	class MyMQTTListener implements  IMqttActionListener {
//    @Override
//    public void onSuccess(IMqttToken asyncActionToken) {
//        try {
//            client.subscribe(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_REQUEST, 0, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//
//                }
//            });
//
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//    	logger.warn("On Failure, exception=" + Utils.getStringException(exception));
//    }
//}
	public static String getLocalDirectory(){
		String hh;
		Properties pp;
		pp = System.getProperties();
		hh = pp.getProperty("user.dir");
		File currentDir = new File(hh);
		String sep = currentDir.separator;
		return hh + sep ;
	}

	public static void main(String[] args) {

		new Heart();
	}
}
