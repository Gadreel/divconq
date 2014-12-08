package divconq.db.update;

public class ConditionalValue {
	protected boolean set = false;
	protected Object value = null;
	
	public void setValue(Object value) {
		this.value = value;
		this.set = true;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public boolean isSet() {
		return this.set;
	}

	public void clear() {
		this.value = null;
		this.set = false;
	}
}
