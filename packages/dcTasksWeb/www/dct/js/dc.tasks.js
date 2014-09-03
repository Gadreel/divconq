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

dc.tasks = {
	Tracker: {
		_list: [],
		_status: [],
		_handler: null,
		
		init: function() {
			setInterval(dc.tasks.Tracker.refresh, 1000);
		},
		
		trackMessage: function(msg) {
			dc.comm.sendMessage(msg, function(e) {
				if (e.Result > 0) {
					dc.pui.Popup.alert(e.Message);
					return;
				}
				
				dc.tasks.Tracker.add(e.Body);
			});
		},
		
		// to the top of the list
		add: function(task, work) {
			if (dc.util.Struct.isRecord(task))
				dc.tasks.Tracker._list.unshift(task);
			else
				dc.tasks.Tracker._list.unshift( { TaskId: task, WorkId: work } );
		},
		
		setWatcher: function(v) {
			dc.tasks.Tracker._handler = v;
		},
		
		getStatus: function() {
			return dc.tasks.Tracker._status;
		},
		
		getStatusFor: function(taskid) {
			for (var i = 0; i < dc.tasks.Tracker._status.length; i++) {
				var task = dc.tasks.Tracker._status[i];
				
				if (task.TaskId == taskid) 
					return task;
			}		
			
			return null;
		},
		
		clear: function(task) {
			for (var i = 0; i < dc.tasks.Tracker._list.length; i++) {
				if (dc.tasks.Tracker._list[i].TaskId == task) {
					dc.tasks.Tracker._list[i].splice(i, 1);
					break;
				}
			}		
		},
		
		refresh: function() {
			if (dc.tasks.Tracker._list.length == 0)
				return;

			var chklist = [ ];
				
			var slist = dc.tasks.Tracker._status;
			
			for (var i = 0; i < dc.tasks.Tracker._list.length; i++) {
				var task = dc.tasks.Tracker._list[i];
			
				var skiptask = false;
			
				for (var i = 0; i < slist.length; i++) {
					if (task.TaskId != slist[i].TaskId) 
						continue;
					
					skiptask = (slist[i].Status == 'Completed');
					break;
				}
				
				if (!skiptask)
					chklist.push(task);
			}		
			
			// no RPC if nothing to check
			if (chklist.length == 0)
				return;
			
			dc.comm.sendMessage({ 
				Service: 'Status',
				Feature: 'Info',
				Op: 'TaskStatus', 
				Body: chklist 
			}, function(e) {
				for (var i = 0; i < e.Body.length; i++) {
					var status = e.Body[i];
				
					var fnd = false;
				
					for (var i = 0; i < slist.length; i++) {
						if (status.TaskId != slist[i].TaskId) 
							continue;
						
						fnd = true;
						slist[i] = status;
						break;
					}
					
					if (!fnd)
						dc.tasks.Tracker._status.push(status);
				}		
				
				if (dc.tasks.Tracker._handler)
					dc.tasks.Tracker._handler.call(dc.tasks.Tracker._status);
			});
		}
	}
}
