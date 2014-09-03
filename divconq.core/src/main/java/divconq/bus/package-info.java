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

 /**
  * <p>
  * Bus is used to communicate between trusted Hubs operating on the same Team.
  * A Team is two or more Hubs operating on the same project and in the same data center
  * (geographically local).  Bus has two channels - one for service calls and one for
  * file transfers (large data). 
  * </p>
  * 
  */
package divconq.bus;

