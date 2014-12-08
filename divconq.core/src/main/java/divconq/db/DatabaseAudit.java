package divconq.db;

public enum DatabaseAudit {
	None,		// no dates even, just 99999 - most space efficient
	Stamps,		// use stamps but no auditing in global
	Updates,	// use stamps and audit updates but not queries
	Full		// audit all, even queries
}
