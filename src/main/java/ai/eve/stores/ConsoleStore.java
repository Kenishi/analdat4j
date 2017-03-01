package ai.eve.stores;

import ai.eve.AbstractTrackerEvent;
import ai.eve.LoggerInterface;
import ai.eve.UserSession;

public class ConsoleStore implements LoggerInterface {
	
	@Override
	public void logEvent(AbstractTrackerEvent event) {
		String name = event.getEventName();
		String data = event.serializeData();
		
		System.out.println("Tracker Event: " + name + " Data: " + data);
	}

	@Override
	public void logEvent(UserSession user, AbstractTrackerEvent event) {
		String userId = user.getUserId();
		String name = event.getEventName();
		String data = event.serializeData();
		
		System.out.println("User: " + userId + " Tracker Event: " + name + " Data: " + data);
	}
}
