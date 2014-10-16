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
  * dcScript is a high level scripting tool that is meant to be easy to develop, 
  * debug and secure.  It is not high performance but the calls it makes may do
  * a lot.  Each instruction in dcScript is run async so that thread use is 
  * minimal.  The data types are all based on {@link divconq.struct} and
  * are defined through {@link divconq.schema}.
  * </p>
  * 
  */
package divconq.script;

