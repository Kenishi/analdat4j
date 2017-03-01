package ai.eve;

import java.lang.reflect.Constructor;
import java.util.Properties;

public class TrackerClient extends AbstractTrackerClient {
	
	private LoggerInterface loggerInterface;
	
	@SuppressWarnings("unchecked")
	public TrackerClient(Properties props) {
		super(props);
		
		String storePath = props.getProperty("store.class", "ai.eve.stores.ConsoleStore");
		try {
			Constructor<LoggerInterface> storeConstructor = 
					(Constructor<LoggerInterface>) TrackerClient.class.getClassLoader().loadClass(storePath).getConstructor();
			this.loggerInterface = storeConstructor.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TrackerClient(Properties props, LoggerInterface logger) {
		super(props);
		this.loggerInterface = logger;
	}

	@Override
	public void trackEvent(UserSession session, AbstractTrackerEvent event) {
		if(loggerInterface != null) {
			loggerInterface.logEvent(session, event);
		}
		
	}

	@Override
	public void trackEvent(AbstractTrackerEvent event) {
		if(loggerInterface != null) {
			loggerInterface.logEvent(event);
		}
	}
}
