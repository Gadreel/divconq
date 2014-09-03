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

dc.user = {
	/**
	 * Tracks user info for the current logged in user.
	 * See "Info" property for collected user data.  
	 * Here is the structure for that data.
	 *
	 *	{
	 *		Credentials: object,		// only if RememberMe - possible security hole use with care
	 *		UserId: string,
	 *		UserName: string,
	 *		FirstName: string,
	 *		LastName: string,
	 *		Email: string,
	 *		RememberMe: boolean,			
	 *		DomainId: string,
	 *		Locale: string,
	 *		Chronology: string,
	 *		Verified: boolean,			// logged in
	 *		AuthTags: array of string
	 *	}
	 *
	 * @type object
	 */
	_info: { },
	_signinhandler: null,
	
	getRememberedPhrase : function() {
		return "152c8abccbbf880db5cd5c5a9487029d40c43c10265f1248bf170ee181bef52f";
	},
	
	// fairly unsafe, use RemberMe only on trusted computer
	loadRememberedUser : function() {
		dc.user._info = { };
			
		try {
			var encrypted = localStorage.getItem("adinfo.remeber");
			
			if (encrypted) {
				var plain = dc.util.Crypto.decrypt(encrypted, dc.user.getRememberedPhrase());
				dc.user._info = JSON.parse(plain);
				
				delete dc.user._info.Verified;
		
				return true;
			}
		}
		catch (x) {
		}
		
		return false;
	},

	/**
	 *  If RememberMe is true then store the current user info.  If not, make sure it is not present on disk and that Credentials are not in memory (much safer approach).
	 */
	saveRememberedUser : function() {
		try {
			if (!dc.user._info || !dc.user._info.RememberMe) {
				if (dc.user._info)
					delete dc.user._info.Credentials;
			
				localStorage.removeItem("adinfo.remeber");
				return;
			}
		
			var plain = JSON.stringify( dc.user._info );
			var crypted = dc.util.Crypto.encrypt(plain, dc.user.getRememberedPhrase());
			localStorage.setItem("adinfo.remeber", crypted);
		}
		catch (x) {
		}
	},
	
	/**
	 *  User is signed in
	 */
	isVerified : function() {
		return (dc.user._info.Verified === true);
	},
	
	isAuthorized: function(tags) {
		if (!tags)
			return true;
			
		if (!dc.user._info.AuthTags)
			return false;
			
		var ret = false;

		$.each(tags, function(i1, itag) {
			$.each(dc.user._info.AuthTags, function(i2, htag) {
				if (itag === htag)
					ret = true;
			});
		});
		
		return ret;
	},
	
	getUserInfo : function() {
		return dc.user._info;
	},
	
	setSignInHandler : function(v) {
		dc.user._signinhandler = v;
	},
	
	signin : function(uname, pass, remember, callback) {
		dc.user.signin2(
			{
				UserName: uname,
				Password: pass
			}, 
			remember, 
			callback
		);
	},
	
	/**
	 * Given the current user info, try to sign in.  Trigger the callback whether sign in works or fails.
	 */
	signin2 : function(creds, remember, callback) {		
		dc.user._info = { };

		// we take what ever Credentials are supplied, so custom Credentials may be used		
		var msg = {
			Service: 'Session',
			Feature: 'Control',
			Op: 'Start',
			Credentials: creds
		};
		
		dc.comm.sendMessage(msg, function(rmsg) { 
			if (rmsg.Result == 0) {
				var resdata = rmsg.Body;
				
				if (resdata.Verified) {
					// copy only select fields for security reasons
					var uinfo = {
						Verified: true,
						UserId: resdata.UserId,
						UserName: resdata.UserName,
						FullName: resdata.FullName,
						Email: resdata.Email,
						AuthTags: resdata.AuthTags,
						DomainId: resdata.DomainId,
						Locale: resdata.Locale,
						Chronology: resdata.Chronology
					}
					
					if (remember) {
						uinfo.Credentials = creds;		
						uinfo.RememberMe = remember;
					}
					
					dc.user._info = uinfo;
 
					// failed login will not wipe out remembered user (could be a server issue or timeout),
					// only set on success - successful logins will save or wipe out depending on Remember
					dc.user.saveRememberedUser();
					
					if (dc.user._signinhandler)
						dc.user._signinhandler.call(dc.user._info);
				}
			}
			
			if (callback)
				callback();
		});
	},
	
	/**
	 *  Sign out the current user, kill session on server
	 */
	signout : function() {
		dc.user._info = { };

		// TODO really should remove the remembered user too
		//localStorage.removeItem("adinfo.remeber");
		
		dc.comm.sendMessage({ 
			Service: 'Session',
			Feature: 'Control',
			Op: 'Stop'
		}, function() {
			dc.comm.close();
		},
		1000);			
	}
	
}