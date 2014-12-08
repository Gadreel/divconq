package divconq.db;

public class DatabaseException extends Exception {
	private static final long serialVersionUID = 4051205180305111305L;

	public DatabaseException(String msg) {
		super(msg);
	}

	public DatabaseException(Exception x) {
		super(x.getMessage());
	}
}
