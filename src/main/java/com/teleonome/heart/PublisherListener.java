package com.teleonome.heart;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;
import com.teleonome.framework.zhinupublisher.ZhinuPublisher;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;

public class PublisherListener  extends AbstractInterceptHandler {
	Logger logger;
	//JSONObject currentPulse;
	 String computerModel = "";
	 private PostgresqlPersistenceManager aDBManager=null;
	 Charset charset = Charset.forName("ISO-8859-1");
	 int messagesReceived=0;
	 int clientsConnected=0;
	 ZhinuPublisher aZhinuPublisher;
	public PublisherListener( ) {
		logger = Logger.getLogger(getClass());
		
		aDBManager = PostgresqlPersistenceManager.instance();
		aZhinuPublisher = new ZhinuPublisher();
		logger.warn("Heart started the receptor and the aZhinuPublisher=" + aZhinuPublisher);
		
	}
	public String getComputerModel() {
		return computerModel;
	}
	public void onConnect(InterceptConnectMessage message) {
		clientsConnected++;
 		logger.info("Heart received connection from " +  message.getClientID() + " clientsConnected="+ clientsConnected);
	}
	 
	    public void onDisconnect(InterceptDisconnectMessage msg) {
	    	logger.info("Heart received onDisconnect from " +  msg.getClientID());
	    } 

	    public void onConnectionLost(InterceptConnectionLostMessage msg) {
	    	logger.info("Heart onConnectionLost from " +  msg.getClientID());
	    }
	    
	public void onPublish(InterceptPublishMessage message) {
		messagesReceived++;
		JSONObject currentPulse;
		logger.info("Heart received a message on topic: " + message.getTopicName()
		+ ", from: " + message.getClientID() + " messagesReceived=" + messagesReceived);
				if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_STATUS)) {
					
					ByteBuf m_buffer = message.getPayload();
					currentPulse = new JSONObject(m_buffer.toString(charset));
					//currentPulse = new JSONObject(message.getPayload().toString(charset));
					
					//
					// now save the file  in the heart directory
					// so that the medula can check it
					try {
						FileUtils.writeStringToFile(new File("HeartTeleonome.denome"), currentPulse.toString(), Charset.defaultCharset());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					//logger.info("currentPulse=" + currentPulse.toString(4));
					String teleonomeName = DenomeUtils.getTeleonomeName(currentPulse);
					 Identity identity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_COMPUTER_INFO, TeleonomeConstants.DENEWORD_COMPUTER_MODEL);
					 try {
						computerModel = (String) DenomeUtils.getDeneWordByIdentity(currentPulse, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 logger.info("computerModel=" + computerModel);
				} else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_ASYNC_CYCLE_UPDATE)) {
					//ByteBuf m_buffer = message.getPayload();
					ByteBuf m_buffer = message.getPayload();
					String updateMessage = m_buffer.toString(charset);
					
					//String updateMessage = charset.decode(m_buffer).toString();
					logger.info("received HEART_TOPIC_ASYNC_CYCLE_UPDATE =" + updateMessage);
					aZhinuPublisher.publish(TeleonomeConstants.HEART_TOPIC_ASYNC_CYCLE_UPDATE, updateMessage);
				} else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_BLINK)) {
					 try {
						 //
						 // get the type of computer, because the command is different
						 //
						if(computerModel.equals(TeleonomeConstants.COMPUTER_MODEL_PI_3_MODEL_B)) {
							Runtime.getRuntime().exec("sudo sh BlinkPi3.sh");
						}else {
							Runtime.getRuntime().exec("sudo sh Blink.sh");
						}
							
						} catch (IOException e) {
							// TODO Auto-gnanenerated catch block
							e.printStackTrace();
						}
					 
				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_ORGANISM_STATUS)) {
					ByteBuf m_buffer = message.getPayload();
					String telephaton = m_buffer.toString(charset);
					logger.debug("received message telephaton=" + telephaton);
				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_ORGANISM_IP)) {
					
				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO)) {
					
				}else if(message.getTopicName().equals(TeleonomeConstants.TELEPHATON_TOPIC_ANNOUNCE)) {
					//
					// 
					ByteBuf m_buffer = message.getPayload();
					String telephatonIpAddress = m_buffer.toString(charset);
					logger.debug("received message telephaton  announced IpAddress=" + telephatonIpAddress);
	        		//int commandId = aDBManager.requestCommandToExecute(command,payLoad);
					
					String command="";
					String commandCode="";
					String commandCodeType="";
					String payLoad= telephatonIpAddress;
					String clientIp="127.0.0.1"; 
					boolean restartRequired=false;
	        		JSONObject commandJSONObject =  aDBManager.requestCommandToExecute( command,  commandCode, commandCodeType,   payLoad,  clientIp,  restartRequired);
	        			
	          		logger.debug("sent command=" + command  + " clientId=" + message.getClientID());

				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_REQUEST)) {
					//
					// this messages comes from a browser
					//	
					ByteBuf m_buffer = message.getPayload();
					String string = m_buffer.toString(charset);
					logger.debug("received payload=" + string);
//					JSONObject payloadJSONObject = new JSONObject(string);
//	        		String identityPointer = payloadJSONObject.getString(TeleonomeConstants.TELEONOME_IDENTITY_LABEL);
//	        		Object value = payloadJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
//	        		String password = payloadJSONObject.getString(TeleonomeConstants.COMMAND_REQUEST_PASSWORD);
//	        		logger.warn("about to apply mutation identityPointer=" + identityPointer + " value=" + value);
//	        		
//	        		JSONObject payLoadParentJSONObject = new JSONObject();
//	        		JSONObject payLoadJSONObject = new JSONObject();
//	        		payLoadParentJSONObject.put("Mutation Name","UpdateControlParameters");
//	        		payLoadParentJSONObject.put("Payload", payLoadJSONObject);
//	        		JSONArray updatesArray = new JSONArray();
//	        		payLoadJSONObject.put("Updates"	, updatesArray);
//
//	        		JSONObject updateJSONObject =  new JSONObject();
//	        		updateJSONObject.put("Target","@On Load:Update DeneWord:Update DeneWord");
//	        		updateJSONObject.put("MutationTargetNewValue",identityPointer);
//	        		updateJSONObject.put("Value",value);
//	        		updatesArray.put(updateJSONObject);
//	        			
//	        		
//	        		
//	        		
//	        		String command="SetParameters";
//	        		String payLoad=payLoadParentJSONObject.toString();
//	        		
//	        		int commandId = aDBManager.requestCommandToExecute(command,payLoad);
//	          		logger.debug("sent command=" + command  + " clientId=" + message.getClientID() + " commandId=" + commandId);
	          		
	          		
				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_RESIGNAL)){
					
					
					ByteBuf m_buffer = message.getPayload();
					
					currentPulse = new JSONObject(m_buffer.toString(charset));
					
					
					JSONObject resignalInfo = new JSONObject(m_buffer.toString(charset));
					
					
					String enableNetworkMode = resignalInfo.getString("EnableNetworkMode");
					String enableHostMode = resignalInfo.getString("EnableHostMode");
					
					String formName = "";//req.getParameter("formName");
					String action = "";//req.getParameter("action");
					String command=null;
					String payLoad="";
					String currentIdentityMode="";
					JSONObject operationalDataDeneChain;
					try {
						operationalDataDeneChain = DenomeUtils.getDeneChainByName(currentPulse,TeleonomeConstants.NUCLEI_PURPOSE,  TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA);
						JSONObject vitalDene = DenomeUtils.getDeneByName(operationalDataDeneChain, "Vital");
						 currentIdentityMode = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(vitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_IDENTITY_MODE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					
					
					if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
						command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK;
						
						String ssid = "";//req.getParameter("AvailableNetworks");
						String password = "";//req.getParameter("password");
						
						JSONObject payLoadParentJSONObject = new JSONObject();

						JSONObject payLoadJSONObject = new JSONObject();
						try {
							payLoadParentJSONObject.put("Mutation Name","SetNetworkMode");

							payLoadParentJSONObject.put("Payload", payLoadJSONObject);
							JSONArray updatesArray = new JSONArray();
							payLoadJSONObject.put("Updates"	, updatesArray);

							JSONObject updateJSONObject =  new JSONObject();
							updateJSONObject.put("Target","@On Load:Update SSID:Update SSID");
							updateJSONObject.put("Value",ssid);
							updatesArray.put(updateJSONObject);

							JSONObject updateJSONObject2 =  new JSONObject();
							updateJSONObject2.put("Target","@On Load:Update PSK:Update PSK");
							updateJSONObject2.put("Value", password );
							updatesArray.put(updateJSONObject2);
							
							System.out.println("mononanyservlet, setting newtork info, ssid=" + ssid + " ps=" + password);
							
							updatesArray.put(updateJSONObject);
							payLoad=payLoadParentJSONObject.toString();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
					}
					
					System.out.println("the action is " + action);
					if(action.equals(TeleonomeConstants.COMMAND_REBOOT)){
						if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
							if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK;
							}else{
								command = action;
							}

						}else{
							System.out.println("the enableHostMode is " + enableHostMode);
							if(enableHostMode!=null && enableHostMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST;
							}else{
								command = action;
							}
						}
					}else if(action.equals(TeleonomeConstants.COMMAND_SHUTDOWN)){
						if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
							System.out.println("the enableNetworkMode is " + enableNetworkMode);

							if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK;
							}else{
								command = action;
							}

						}else{
							System.out.println("the enableHostMode is " + enableHostMode);
							if(enableHostMode!=null && enableHostMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST;
							}else{
								command = action;
							}
						}
					}else{
					}
					
					
					
					System.out.println("the action is " + action);
					if(action.equals(TeleonomeConstants.COMMAND_REBOOT)){
						if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
							if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK;
							}else{
								command = action;
							}

						}else{
							System.out.println("the enableHostMode is " + enableHostMode);
							if(enableHostMode!=null && enableHostMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST;
							}else{
								command = action;
							}
						}
					}else if(action.equals(TeleonomeConstants.COMMAND_SHUTDOWN)){
						if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
							System.out.println("the enableNetworkMode is " + enableNetworkMode);
							
							if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK;
							}else{
								command = action;
							}

						}else{
							System.out.println("the enableHostMode is " + enableHostMode);
							if(enableHostMode!=null && enableHostMode.equals("Yes")){
								command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST;
							}else{
								command = action;
							}
						}
					}else{
					}


					//
					// delete the startpulse.info and endpulse.info
					File file = new File(Utils.getLocalDirectory() + "StartPulse.info");
					if(file.isFile())file.delete();
					file = new File(Utils.getLocalDirectory() + "EndPulse.info");
					if(file.isFile())file.delete();
					//
					// write the kill file so as to mark it dead
					//
//					FileUtils.writeByteArrayToFile(new File("KillPulse.info"), (""+System.currentTimeMillis()).getBytes());

				

				}else if(message.getTopicName().equals(TeleonomeConstants.HEART_TOPIC_EXECUTE_MANUAL_ACTION)){

					//
					// this form has another parameter that will indicate whether we are starting or stopping 
					// manually
					//line 543  - timerModeSwitch =TimerOn
					//		line 543  - TimerMinutes =10
					//		line 543  - formName =ManualStart
					//		line 543  - StartButtonAction =Start
					//
					// is either Start or Stop	
					
					String action = "";//req.getParameter("StartButtonAction");
					//
					// if its on the value is TimerOn otherwise null
					String timerModeSwitch = "";//req.getParameter("timerModeSwitch");
					String command="";
					String payLoad="";
					//
					// is alwyas an integer
					String timerMinutesAmount = "";//req.getParameter("TimerMinutes");
					int timerMinutes = 0;
					if(timerMinutesAmount!=null && !timerMinutesAmount.equals("")){
						try{
							timerMinutes = Integer.parseInt(timerMinutesAmount);
						}catch(NumberFormatException e){
							
						}
					}
					String timerTypeSwitch =  "";//req.getParameter("timerTypeSwitch");
					String timerType="Volume";
					if(timerTypeSwitch!=null && timerTypeSwitch.equals("Minutes")){
						timerType="Time";
					}
					System.out.println("MonoNanny Do Post timerTypeSwitch=" + timerTypeSwitch + " action=" + action + " timerModeSwitch=" + timerModeSwitch + " timerMinutes=" + timerMinutes + " timerType=" + timerType);

					if(timerModeSwitch!=null && timerModeSwitch.equals("TimerOn")){
						if(action.equals("Start")){
							command = "TurnOnWithTimer";
							
							
							JSONObject payLoadParentJSONObject = new JSONObject();

							JSONObject payLoadJSONObject = new JSONObject();
							try {
								payLoadParentJSONObject.put("Mutation Name","TurnOnWithTimer");

								payLoadParentJSONObject.put("Payload", payLoadJSONObject);
								JSONArray updatesArray = new JSONArray();
								payLoadJSONObject.put("Updates"	, updatesArray);
								
								JSONObject updateJSONObject =  new JSONObject();
								updateJSONObject.put("Target","@On Load:Update Actuator Command Code True Expression:Update Actuator Command Code True Expression");
								updateJSONObject.put("Value","TurnOnWithTimer#" + timerMinutes + "#" + timerType);
								updatesArray.put(updateJSONObject);

							
								JSONObject updateJSONObject2 =  new JSONObject();
								updateJSONObject2.put("Target","@On Load:Update Set Timer Type:Update Set Timer Type");
								updateJSONObject2.put("Value", timerType );
								updatesArray.put(updateJSONObject2);

								JSONObject updateJSONObject3 =  new JSONObject();
								updateJSONObject3.put("Target","@On Load:Update Set Timer Minutes to New Value:Update Set Timer Minutes Maximum Body Temperature to New Value");
								updateJSONObject3.put("Value",timerMinutes);
								updatesArray.put(updateJSONObject3);
								payLoad=payLoadParentJSONObject.toString();

							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							
							
						}else if(action.equals("Stop")){
							command = "Turn Pump Off";
						}	
					}else{
						if(action.equals("Start")){
							command = "Turn Pump On";
						}else if(action.equals("Stop")){
							command = "Turn Pump Off";
						}	
					}
					
					//sendCommand(command, payLoad);


				}
				currentPulse=null;
				System.gc();
	}
	
	  
	
	
	public String getID() {
		// TODO Auto-generated method stub
		return "Heart Publisher Listener";
	}
}
