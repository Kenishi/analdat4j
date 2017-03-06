package ai.eve.stores;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.parse4j.Parse;
import org.parse4j.ParseBatch;
import org.parse4j.ParseException;
import org.parse4j.ParseObject;

import ai.eve.AbstractTrackerEvent;
import ai.eve.LoggerInterface;
import ai.eve.UserSession;

public class ParseStore implements LoggerInterface {
	private static Map<Properties, ParseStore> instances = new HashMap<>();
	
	private Properties props;
	private String table;
	
	private Object messageLock = new Object();
	private Vector<Message> messagesOut = new Vector<Message>();
	private Timer timer;
	
	private ParseStore(Properties props) {
		this.props = props;
		
		String endpoint = props.getProperty("store.parse.endpoint");
		String appId = props.getProperty("store.parse.appId");
		String key = props.getProperty("store.parse.key");
		
		Parse.initialize(appId, key, endpoint);
		
		this.table = props.getProperty("store.parse.table");
		
		// Setup the timer
		TimerTask task = new DispatchTask(this);
		this.timer = new Timer();
		this.timer.scheduleAtFixedRate(task, 10000, 10000);
		
		// Setup shutdown hook to kill the timer
		Thread shutdownThread = new Thread(new ShutdownHandler(this.timer, this));
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}
	
	public static ParseStore instance(Properties props) {
		if(props == null) throw new NullPointerException("A properties file is required for getting an instance.");
		if(!instances.containsKey(props) || instances.get(props) == null) {
			ParseStore store = new ParseStore(props);
			instances.put(props, store);
		}
		return instances.get(props);
	}
	
	@Override
	public void logEvent(AbstractTrackerEvent event) {
		logEvent(null, event);
	}
	
	@Override
	public void logEvent(UserSession user, AbstractTrackerEvent event) {
		Message msg = new Message(user, event);
		synchronized (this.messageLock) {
			this.messagesOut.addElement(msg);
		}
	}
	
	private static class DispatchTask extends TimerTask {
		// Because Parse lib isn't built to work with multiple parse apps we'll use
		// a lock to help avoid having multiple dispatches overwritting each other
		private static Lock dispatchLock = new ReentrantLock();
		private ParseStore storeInst;
		
		public DispatchTask(ParseStore store) {
			this.storeInst = store;
		}
		
		@Override
		public void run() {
			Message[] msgs;
			synchronized (this.storeInst.messageLock) {
				int msgCount = this.storeInst.messagesOut.size();
				msgs = this.storeInst.messagesOut.toArray(new Message[msgCount]);
				this.storeInst.messagesOut.clear();
			}
			
			int count = 0;
			ParseBatch batcher = new ParseBatch();
			for(Message m : msgs) {
				ParseObject obj = new ParseObject(this.storeInst.table);
				if(m.user != null) {
					obj.put("userId", m.user.getUserId());
				}
				
				obj.put("eventName", m.event.getEventName());
				obj.put("source", m.event.getSource());
				obj.put("data", m.event.serializeData());
				obj.put("timestamp", m.event.getTimestamp());
				batcher.createObject(obj);
				
				count++;
				
				if(count >= 49) {
					try {
						batcher.batch();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					count = 0;
				}
			}
		}
		
	}
	
	private class Message {
		public UserSession user;
		public AbstractTrackerEvent event;
		public Message(UserSession s, AbstractTrackerEvent e) {
			this.user = s;
			this.event = e;
		}
	}
	
	private class ShutdownHandler implements Runnable {
		private Timer timer;
		private ParseStore store;
		
		public ShutdownHandler(Timer timer, ParseStore store) {
			this.timer = timer;
			this.store = store;
		}
		
		@Override
		public void run() {
			// Kill any running timers
			this.timer.cancel();
			
			// Run the dispatch task one more time to clear the dispatch queue
			Thread thread = new Thread(new DispatchTask(this.store));
			thread.start();
		}
	}
}
