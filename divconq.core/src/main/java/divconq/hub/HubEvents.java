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
package divconq.hub;

public class HubEvents {
	final static public int Booted = 0;			// no work, no interfaces even if a gateway
	final static public int Connected = 1;		// full business as usual
	final static public int Running = 2;		// full business as usual
	final static public int Idling = 3;			// don't take on new work, uploads/schedule - but stay alive, admin web/rpc available?  
	final static public int Stopping = 4;
	
	final static public int BusConnected = 100;
	final static public int BusDisconnected = 101;
	
	final static public int DomainConfigChanged = 200;
	final static public int DomainAdded = 201;
	final static public int DomainUpdated = 202;
}
