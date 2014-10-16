package divconq.struct;

public class StructUtil {
	static public RecordStruct record(FieldStruct... fields) {
		return new RecordStruct(fields);
	}
	
	static public FieldStruct field(String name, Object value) {
		return new FieldStruct(name, value);
	}
	
	static public ListStruct list(Object... values) {
		return new ListStruct(values);
	}	
}
