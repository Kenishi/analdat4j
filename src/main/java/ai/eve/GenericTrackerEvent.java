package ai.eve;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class GenericTrackerEvent extends AbstractTrackerEvent {
	
	private String event;
	private String data;
	
	public GenericTrackerEvent() {
		super();
	}
	
	public GenericTrackerEvent(String event, String data) {
		this.event = event;
		this.data = data;
	}
	
	@Override
	public String getEventName() {
		return this.event;
	}

	@Override
	public String serializeData() {
		return this.data;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(this == obj) return true;
		if(this.getClass() != obj.getClass()) return false;
		
		GenericTrackerEvent cast = (GenericTrackerEvent) obj;
		
		return new EqualsBuilder()
				.appendSuper(super.equals(obj))
				.append(this.event, cast.event)
				.append(this.data, cast.data)
				.build();
	}
}
