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
package divconq.script;

public enum ExecuteState {
    Ready,      // instruction has been reset and ready for fresh run
    Resume,       // been run at least once, ready to continue runs
    Done,		// instruction is complete, go on to next
    Break,      // stop this block, return to parent block
    Continue,   // restart this block, but keep the variables
    Exit        // the script is done, leave all levels of the code
}
