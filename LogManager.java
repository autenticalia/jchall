package de.maultaschenfabrikle.kurts.warehouse;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Level;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;


public class LogManager extends Level {

	private static final long serialVersionUID = -2605763352847032132L;
	private static LogManager singleton = null;	
	private String configLocation = "/usr/local/tomcat/webapps/warehouse/WEB-INF/classes/log4j2.xml";
	
	protected LogManager(String name, int value) {
		super(name, value);
	}
	
	public static LogManager getInstance() {
		
		if (null == singleton) {
			singleton = new LogManager("GAU", 5);
		}
		
		return singleton;
		
	}
	
	public Logger getLogger(String name) {
									
		LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
		String currentConfigLocation = context.getConfiguration().getConfigurationSource().getLocation();
				
		//logger.error(currentConfigLocation);
		if (currentConfigLocation == null || !currentConfigLocation.equals(configLocation)) {
			
			//logger.error("going to reconfigure");
			
			System.out.println("reconfiguring log4j");					
						
            // dirty log4j2 bugfix
			// required as the library sleeps indefinitely if the logger is re-configured online
			// see LoggerContext->setConfiguration (Line 364)
			try {
				Field configField;
				configField = LoggerContext.class.getDeclaredField("config");
	            configField.setAccessible(true);
	            configField.set(context, null);
			} catch (Exception e) {
				System.out.println("ERROR while fixing the log4j lib");
				e.printStackTrace();
			}

			context.setConfigLocation(new File(configLocation).toURI());			
		}
		
		Logger logger  = context.getLogger(name);
		logger  = context.getLogger(name);		
		return logger;
		
	}
			
}
