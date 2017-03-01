package ai.eve;

public interface LoggerInterface {
	public void logEvent(AbstractTrackerEvent event);
	public void logEvent(UserSession user, AbstractTrackerEvent event);
}
