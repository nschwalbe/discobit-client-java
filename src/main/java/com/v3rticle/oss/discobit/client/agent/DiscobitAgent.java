package com.v3rticle.oss.discobit.client.agent;

import com.v3rticle.oss.discobit.client.DiscobitClientFactory;
import com.v3rticle.oss.discobit.client.DiscobitOperationException;
import com.v3rticle.oss.discobit.client.bootstrap.DiscobitSettings;

import java.lang.instrument.Instrumentation;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

public class DiscobitAgent {
private static Logger log = Logger.getLogger(DiscobitAgent.class.getName());
	
	public static void main(String[] args){
		execute();
	}
	
	public static void premain(String agentArgs, Instrumentation inst){
		execute();
	}
	
	private static void execute(){
		DiscobitSettings settings = new DiscobitSettings();
		try {
			log.info("Reading discobit configuration from " + settings.getServerURL() + "::" + settings.getDefaultConfigurationUUID());
			Properties props = DiscobitClientFactory.getClient().getConfig(UUID.fromString(settings.getDefaultConfigurationUUID()));
			Enumeration e = props.propertyNames();
		    while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      String value = (String) props.getProperty(key);
		      
		      if (key != null && value != null){
		    	  System.setProperty(key, value);
		      } else if (key != null && value == null){
		    	  System.clearProperty(key);
		      }
		    }

			log.info("injected " + props.size() + " properties to system context");

		} catch (DiscobitOperationException e) {
			log.warning(e.getMessage());
		} finally {
//			try {
//				Unirest.shutdown();
//			} catch (IOException e) {
//				log.warning(e.getMessage());
//			}
		}
	}
}
