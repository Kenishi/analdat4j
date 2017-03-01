package ai.eve.stores;

import java.sql.Connection;

import ai.eve.AbstractTrackerEvent;
import ai.eve.LoggerInterface;
import ai.eve.UserSession;

public class JDBCStore implements LoggerInterface {
	
	private Connection conn;
	
	public JDBCStore(Connection connection) {
		this.conn = connection;
	}
	
	@Override
	public void logEvent(AbstractTrackerEvent event) {
		

	}

	@Override
	public void logEvent(UserSession user, AbstractTrackerEvent event) {
		// TODO Auto-generated method stub
		
	}

}
