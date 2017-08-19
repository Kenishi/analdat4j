package ai.eve;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ai.eve.stores.ConsoleStore;

public class TrackerClientTest {
	private List<Object[]> events;
	
	@Before
	public void setUp() throws Exception {
		events = new ArrayList<>();
	}

	@After
	public void tearDown() throws Exception {
		events = null;
	}

	@Test
	public void testLogging() {
		Properties props = new Properties();
		TestLogger logger = new TestLogger();
		TrackerClient client = new TrackerClient(props, logger);
		GenericTrackerEvent event = new GenericTrackerEvent("testEvent", "testData", "server");
		client.trackEvent(event);
		
		// Test that non-user track logged
		GenericTrackerEvent expected = new GenericTrackerEvent("testEvent", "testData", "server");
		expected.setTimestamp(event.getTimestamp());
		
		boolean result = events.stream()
				.anyMatch(ev -> ev[0] == null && ev[1].equals(expected));
		assertTrue(result);
		
		UserSession session = new UserSession("myUserId");
		event = new GenericTrackerEvent("testEvent2", "testData2", "server");
		client.trackEvent(session, event);
		
		GenericTrackerEvent expected2 = new GenericTrackerEvent("testEvent2", "testData2", "server");
		expected2.setTimestamp(event.getTimestamp());
		result = events.stream()
				.anyMatch(ev -> (ev[0] != null && ev[0].equals(new UserSession("myUserId"))) && ev[1].equals(expected2));
		assertTrue(result);
	}
	
	@Test
	public void testConsoleLoggerFallback() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		TrackerClient client = new TrackerClient(new Properties());
		Field field = client.getClass().getDeclaredField("loggerInterface");
		field.setAccessible(true);
		LoggerInterface logger = (LoggerInterface) field.get(client);
		assertEquals(ConsoleStore.class, logger.getClass());
	}
	
	public class TestLogger implements LoggerInterface {

		@Override
		public void logEvent(AbstractTrackerEvent event) {
			events.add(new Object[]{null, event});
		}

		@Override
		public void logEvent(UserSession user, AbstractTrackerEvent event) {
			events.add(new Object[]{user, event});
		}
	}
}
