package ai.eve.stores;

import java.util.Properties;
import java.util.logging.Logger;

import org.parse4j.Parse;
import org.parse4j.ParseException;
import org.parse4j.ParseObject;

import ai.eve.AbstractTrackerEvent;
import ai.eve.LoggerInterface;
import ai.eve.UserSession;

public class ParseStore implements LoggerInterface {
	private String table;
	
	public ParseStore(Properties props) {
		String endpoint = props.getProperty("store.parse.endpoint");
		String appId = props.getProperty("store.parse.appId");
		String key = props.getProperty("store.parse.key");
		
		Parse.initialize(appId, key, endpoint);
		
		this.table = props.getProperty("store.parse.table");
	}
	
	@Override
	public void logEvent(AbstractTrackerEvent event) {
		ParseObject obj = new ParseObject(this.table);
		obj.add("eventName", event.getEventName());
		obj.add("data", event.serializeData());
		obj.add("timestamp", event.getTimestamp());
		obj.saveInBackground();
	}
	
	@Override
	public void logEvent(UserSession user, AbstractTrackerEvent event) {
		ParseObject obj = new ParseObject(this.table);
		obj.add("userId", user.getUserId());
		obj.add("eventName", event.getEventName());
		obj.add("data", event.serializeData());
		obj.add("timestamp", event.getTimestamp());
		obj.saveInBackground();
	}
}
