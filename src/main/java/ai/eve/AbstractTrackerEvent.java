package ai.eve;

import org.apache.commons.lang3.builder.EqualsBuilder;

public abstract class AbstractTrackerEvent {
	
	private long timestamp;
	
	public AbstractTrackerEvent() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	protected void setTimestamp(long time) { this.timestamp = time; }
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(obj == this) return true;
		if(this.getClass() != obj.getClass()) return false;
		
		AbstractTrackerEvent cast = (AbstractTrackerEvent) obj;
		
		return new EqualsBuilder()
				.append(this.timestamp, cast.timestamp)
				.build();
	}
	
	abstract public String getEventName();
	abstract public String serializeData();
}
