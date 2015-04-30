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

dc.comm = {
	/**
	 * Only init once.
	 */
	_initFlag: false,
	_session: null,
	
	init: function(callback) {
		// only init once per page load
		if (dc.comm._initFlag) {
			callback();				
			return;
		}
		
		dc.comm._initFlag = true;	
				
		// check to see see if the user info was remembered
		// this is not so secure as we use a hard coded key for that, but at least it is
		// encrypted on disk.  'Remember' should only be used on devices with personal 
		// accounts - never shared accounts or public devices.
		dc.user.loadRememberedUser();
		
		// periodically keep session going
		setInterval(function() {
			dc.comm.sendForgetMessage({ 
				Service: 'Session',
				Feature: 'Control',
				Op: 'Touch'
			});
			
			// TODO check for messages on server
			
			// also periodically run the timeout checker for replies
			
			// TODO check for timeouts in Replies

			// TODO		callbackfunc( { Result: 1, Message: "AJAX call failed or timed out." } );
		}, 55000);

		callback();				
	},
	
	sendForgetMessage : function(msg) {
		msg.RespondTag = 'SendForget';
		
		dc.comm.sendMessage(msg);
	},
	
	sendMessage : function(msg, callbackfunc, timeout) {
		if (dc.comm._session)
			msg.Session = dc.comm._session;
			
		$.ajax( { 
			type: 'POST', 
			url: '/rpc?nocache=' + dc.util.Crypto.makeSimpleKey(), 
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify(msg), 
			processData: false,
			success: function(rmsg) {
				//console.log('after rpc: ' + JSON.stringify(rmsg));	
				
				var ee = dc.util.Messages.findExitEntry(rmsg.Messages);
				
				// setup the "result" of the message based on the exit entry
				if (!ee) {
					rmsg.Result = 0;
				}
				else {
					rmsg.Result = ee.Code;
					rmsg.Message = ee.Message;
				}
				
				if (rmsg.SessionChanged) {
					console.log('session changed');
					
					if (dc.pui && dc.pui.Loader)
						dc.pui.Loader.SessionChanged();
				}

				dc.comm._session = rmsg.Session; 
				
				if (callbackfunc) 
					callbackfunc(rmsg);
			}, 
			timeout: timeout ? timeout : 60000,
			error: function() {
				if (callbackfunc) 
					callbackfunc( { Result: 1, Message: "AJAX call failed or timed out." } );
			}
		} );
	},
	
	sendTestMessage : function(msg) {
		dc.comm.sendMessage(msg, function(res) {
			console.log('Result: ' + JSON.stringify(res));
		});
	},
	
	Tracker: {
		_list: [],
		_status: [],
		_handler: null,
		
		init: function() {
			setInterval(dc.comm.Tracker.refresh, 1000);
		},
		
		trackMessage: function(msg) {
			dc.comm.sendMessage(msg, function(e) {
				if (e.Result > 0) {
					dc.pui.Popup.alert(e.Message);
					return;
				}
				
				dc.comm.Tracker.add(e.Body);
			});
		},
		
		// to the top of the list
		add: function(task, work) {
			if (dc.util.Struct.isRecord(task))
				dc.comm.Tracker._list.unshift(task);
			else
				dc.comm.Tracker._list.unshift( { TaskId: task, WorkId: work } );
		},
		
		setWatcher: function(v) {
			dc.comm.Tracker._handler = v;
		},
		
		getStatus: function() {
			return dc.comm.Tracker._status;
		},
		
		getStatusFor: function(taskid) {
			for (var i = 0; i < dc.comm.Tracker._status.length; i++) {
				var task = dc.comm.Tracker._status[i];
				
				if (task.TaskId == taskid) 
					return task;
			}		
			
			return null;
		},
		
		clear: function(task) {
			for (var i = 0; i < dc.comm.Tracker._list.length; i++) {
				if (dc.comm.Tracker._list[i].TaskId == task) {
					dc.comm.Tracker._list[i].splice(i, 1);
					break;
				}
			}		
		},
		
		refresh: function() {
			if (dc.comm.Tracker._list.length == 0)
				return;

			var chklist = [ ];
				
			var slist = dc.comm.Tracker._status;
			
			for (var i = 0; i < dc.comm.Tracker._list.length; i++) {
				var task = dc.comm.Tracker._list[i];
			
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
						dc.comm.Tracker._status.push(status);
				}		
				
				if (dc.comm.Tracker._handler)
					dc.comm.Tracker._handler.call(dc.comm.Tracker._status);
			});
		}
	}
}