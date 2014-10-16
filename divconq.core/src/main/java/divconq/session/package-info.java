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
  * Sessions are used to track state both on the client end and server (service) end.
  * HTTP, SFTP and other protocols make sessions.  All communication channels through
  * session.  Sessions can talk to each other.  Sessions have built in feature for
  * transmission of large data (files), such that file transfer between Hubs is always
  * done from Session to Session even if the session is for a background process and
  * not a connected client.
  * </p>
  * 
  */
package divconq.session;

