package ai.eve;

import java.util.Properties;

public abstract class AbstractTrackerClient {
	
	private Properties properties;
	
	public AbstractTrackerClient(Properties props) {
		this.properties = props;
	}
	
	public Properties getProperties() {
		return this.properties;
	}
	
	abstract public void trackEvent(UserSession session, AbstractTrackerEvent event);
	abstract public void trackEvent(AbstractTrackerEvent event);
}
