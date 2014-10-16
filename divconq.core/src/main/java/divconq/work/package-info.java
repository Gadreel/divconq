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
  * DivConq uses Tasks for many things.  Generally any processing that you don't want to
  * do in your current thread should be placed in a Task.  A task may then be run on
  * the local hub's work pool directly or the task may be submitted to a shared
  * bucket of work that can be processed by any hub in the Squad.  
  * </p>
  * 
  * <p>
  * You start by making a Task which describes what you want to do:
  * </p>
  * 
  * <pre><code>Task task = new Task()
  *	 .withTitle("Greeting Carl " + i)
  *	 .withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
  *	 .withTimeout(5)		// give 5 seconds to complete
  *	 .withWork(SlowGreetWork.class);    // the class that does the work
  * </code></pre>
  * 
  * <p>
  * Now either submit the task to the local WorkPool to run immediately:
  * </p>
  * 
  * <pre><code>Hub.instance.submitToWorkPool(task);</code></pre>
  * 
  * <p>
  * or submit the task to the shared work queue to run on the next available 
  * DivConq server:
  * </p>
  * 
  * <pre><code>Hub.instance.submitToWorkQueue(task);</code></pre>
  * 
  * <p>
  * Submits to the work queue also have an audit trail of their run in the
  * dcwork* tables.  The local work pool has no audit beyond what might
  * show up in the log file.  Note that at present work queue requires
  * that you have a properly configured SQL database, work pool does not.
  * </p>
  * 
  * The key classes for this package are {@link divconq.work.Task}, {@link divconq.work.IWork},
  * {@link divconq.work.TaskRun}, {@link divconq.work.WorkPool}, {@link divconq.work.WorkQueue}.
  * 
  */
package divconq.work;

