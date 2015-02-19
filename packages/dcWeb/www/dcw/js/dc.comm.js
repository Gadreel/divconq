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
	 * Track replies.  Each value is 
	 * {
	 *    Func: [callback],
	 *    Timeout: [time at which it will timeout in ms],
	 *    Tag: [relpy tag id]
	 * }
	 *
	 * So replies contatins [tag] = [reply tracker]
	 */
	_replies: { },
	
	// the web socket object
	_ws: null,
	
	// the close/error handler
	_donehandler: null,

	/**
	 * Only init once.
	 */
	_initFlag: false,
	
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
		
		try {
			var url = dc.util.Ws.getConnUrl('/bus');
			
			dc.comm._ws = new WebSocket(url);
			
			dc.comm._ws.onmessage = function(e) {
				//console.log('msg: ' + e.data);
				
				try {
					var msg = JSON.parse(e.data);
						
					var ee = dc.util.Messages.findExitEntry(msg.Messages);
					
					// setup the "result" of the message based on the exit entry
					if (!ee) {
						msg.Result = 0;
					}
					else {
						msg.Result = ee.Code;
						msg.Message = ee.Message;
					}
					
					if (msg.Service == 'Replies') {
						if (msg.Feature == 'Reply') {
							if (msg.Op == 'Deliver') {
								var hand = dc.comm._replies[msg.Tag];
								
								if (hand) {
									delete dc.comm._replies[msg.Tag];
									
									hand.Func(msg);
								}
							}
						}
					}
					
					// TODO add services...
				}
				catch (x) {
					console.log('bad msg: ' + x);
					console.log('bad msg: ' + e.data);
				}
			};
			
			dc.comm._ws.onerror = function(e) {
				//console.log('error: ' + e);
				
				if (dc.comm._donehandler)
					dc.comm._donehandler();
			};
			
			dc.comm._ws.onopen = function(e) {
				//console.log('opened: ' + e);

				callback();
			}
			
			dc.comm._ws.onclose = function(e) {
				if (dc.comm._donehandler)
					dc.comm._donehandler();
			
				//console.log('closed: ' + e);
			}
			
			// periodic 
			setInterval(function() {
				// periodically keep session going
				
				dc.comm.sendForgetMessage({ 
					Service: 'Session',
					Feature: 'Control',
					Op: 'Touch'
				});
				
				// also periodically run the timeout checker for replies
				
				// TODO check for timeouts in Replies

				// TODO		callbackfunc( { Result: 1, Message: "AJAX call failed or timed out." } );
			}, 55000);
		}
		catch(x) {
			console.log('Unabled to create web socket: ' + x);
			callback();
			return;
		}
	},
	
	setDoneHandler: function(v) {
		dc.comm._donehandler = v;
	},
	
	close: function() {
		if (dc.comm._ws)
			dc.comm._ws.close();
	},
	
	sendForgetMessage : function(msg) {
		msg.RespondTag = 'SendForget';
		
		if (dc.comm._ws && (dc.comm._ws.readyState == 1))
			dc.comm._ws.send(JSON.stringify(msg));
	},
	
	sendMessage : function(msg, callbackfunc, timeout) {
		if (!dc.comm._ws || (dc.comm._ws.readyState != 1)) {
			if (callbackfunc)
				callbackfunc( { Result: 1, Message: "Unable to send data to server, connection is not ready." } );
			
			return;
		}
			
		if (msg.RespondTag != 'SendForget') {
			var tag = dc.util.Crypto.makeSimpleKey();	
			
			msg.RespondTo = 'Replies';
			msg.RespondTag = tag;
			
			timeout = (timeout ? timeout : 60) * 1000,
			
			dc.comm._replies[tag] = {
				Func: callbackfunc,
				Timeout: (new Date()).getTime() + timeout,
				Tag: tag
			};
		}
	
		dc.comm._ws.send(JSON.stringify(msg));
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