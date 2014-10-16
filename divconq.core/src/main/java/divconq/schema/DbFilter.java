/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.schema;

public class DbFilter {
	public String name = null;
	public String table = null;
	public String execute = null;
	
	// TODO consider storing a type here also, if type is present then "Extra" must match that time
}