package security;

public class TimeValue {
	private int timestamp;
	private String value;
	
	public TimeValue(int timestamp, String value) {
		this.timestamp = timestamp;
		this.value = value;
	}
	
	public void setTimeValue(int timestamp, String value) {
		this.timestamp = timestamp;
		this.value = value;
	}
	
	public TimeValue getTimeValue() {
		return  this;
	}
}
