package ai.eve.stores;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.json.JSONArray;
import org.parse4j.Parse;
import org.parse4j.ParseBatch;
import org.parse4j.ParseException;
import org.parse4j.ParseObject;
import org.parse4j.ParseUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.eve.AbstractTrackerEvent;
import ai.eve.LoggerInterface;
import ai.eve.UserSession;

public class ParseStore implements LoggerInterface {
	final static Logger logger = LoggerFactory.getLogger(ParseStore.class);
	final static Integer BATCH_SIZE = Integer.valueOf(20);
	final static Integer BATCH_TIME = Integer.valueOf(10000); // Milliseconds
	
	private static Map<Properties, ParseStore> instances = new HashMap<>();
	
	private Properties props;
	private String table;
	
	private Object messageLock = new Object();
	private Vector<Message> messagesOut = new Vector<Message>();
	private Timer timer;
	
	public ParseStore(Properties props) {
		logger.trace("Instantiating ParseStore");
		this.props = props;
		
		String endpoint = props.getProperty("store.parse.endpoint");
		String appId = props.getProperty("store.parse.appId");
		String masterKey = props.getProperty("store.parse.master");
		
		Parse.initializeAsRoot(appId, masterKey, endpoint);
				
		this.table = props.getProperty("store.parse.table");
		
		// Setup the timer
		TimerTask task = new DispatchTask(this);
		this.timer = new Timer("parseStoreDispatcher", true); // Is a Daemon thread
		this.timer.scheduleAtFixedRate(task, BATCH_TIME, BATCH_TIME);
		
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
		// TODO: This doesn't work, need to add a call to make parse switch its initialization when we want to  use this instance
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

				try {
					obj.save();
				} catch(Exception e) {
					logger.error("Error creating object during ParseStore dispatch.", e);
				}
				
//				batcher.createObject(obj);
//				count++;
//				
//				if(count >= BATCH_SIZE) {
//					dispatch(batcher);
//					batcher = new ParseBatch();
//					count = 0;
//				}
			}
			
//			 Dispatch any remaining messages
//			if(count > 0) {
//				dispatch(batcher);
//			}
		}
		
		private void dispatch(final ParseBatch batcher) {
			try {
				JSONArray array = batcher.batch();
				logger.trace("Batch request finished. Result: {}", array.toString());
			} catch (ParseException e) {
				e.printStackTrace();
				logger.error("Error batching request: {} {}", e.getMessage(), e.getStackTrace().toString());
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
			new DispatchTask(this.store).run();
		}
	}
}
