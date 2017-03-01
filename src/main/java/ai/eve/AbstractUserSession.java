package ai.eve;

import org.apache.commons.lang3.builder.EqualsBuilder;

public abstract class AbstractUserSession {
	
	private String uuid;
	
	public AbstractUserSession(String userId) {
		this.uuid = userId;
	}
	
	public String getUserId() {
		return this.uuid;
	}
	
	public void setUserId(String userId) {
		this.uuid = userId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(this.getClass() != obj.getClass()) return false;
		
		AbstractUserSession cast = (AbstractUserSession) obj;
		
		return new EqualsBuilder()
				.append(this.uuid, cast.uuid)
				.build();
	}
}
