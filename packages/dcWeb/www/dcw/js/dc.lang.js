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

var is_chrome = navigator.userAgent.indexOf('Chrome') > -1;
var is_explorer = navigator.userAgent.indexOf('MSIE') > -1;
var is_firefox = navigator.userAgent.indexOf('Firefox') > -1;
var is_safari = navigator.userAgent.indexOf("Safari") > -1;
var is_Opera = navigator.userAgent.indexOf("Presto") > -1;
if ((is_chrome)&&(is_safari)) {is_safari=false;}

var isWindows = navigator.userAgent.toLowerCase().indexOf("windows") > -1;

//trimming space from both side of the string
String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g,"");
}
 
//trimming space from left side of the string
String.prototype.ltrim = function() {
	return this.replace(/^\s+/,"");
}
 
//trimming space from right side of the string
String.prototype.rtrim = function() {
	return this.replace(/\s+$/,"");
}

//pads left
String.prototype.lpad = function(padString, length) {
	var str = this;
    while (str.length < length)
        str = padString + str;
    return str;
}

//pads right
String.prototype.rpad = function(padString, length) {
	var str = this;
    while (str.length < length)
        str = str + padString;
    return str;
}
  
if (!String.prototype.endsWith) {
	Object.defineProperty(String.prototype, 'endsWith', {
		value: function(searchString, position) {
			var subjectString = this.toString();
			if (position === undefined || position > subjectString.length) {
				position = subjectString.length;
			}

			position -= searchString.length;
			var lastIndex = subjectString.indexOf(searchString, position);
			return lastIndex !== -1 && lastIndex === position;
		}
	});
}  

if (!String.prototype.startsWith) {
	Object.defineProperty(String.prototype, 'startsWith', {
		enumerable: false,
		configurable: false,
		writable: false,
		value: function(searchString, position) {
			position = position || 0;
			return this.lastIndexOf(searchString, position) === position;
		}
	});
}

String.prototype.escapeQuotes = function() {
	return this.replace(/'/g,"\\\'").replace(/"/gi,"\\\"");
}

String.prototype.escapeSingleQuotes = function() {
	return this.replace(/'/g,"\\\'");
}

String.prototype.escapeDoubleQuotes = function() {
	return this.replace(/"/g,"\\\"");
}

String.prototype.escapeHtml = function() {
	return this.replace(/&/g, '&amp;').replace(/>/g, '&gt;').replace(/</g, '&lt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
}

String.prototype.urldecode = function() {
	return decodeURIComponent(this.replace(/\+/g, '%20'));
}

String.escapeHtml = function(val) {
	if (dc.util.Struct.isEmpty(val))
		return '';

	if (!dc.util.String.isString(val))
		return val + '';
		
	return val.escapeHtml();
}

String.formatMoney = function(total) {
	//if (!dc.util.Number.isNumber(total))
	//	total = 0;
	
	return dc.util.Number.toNumberStrict(total).toFixed(2);

	/*		
	var ttotal = total + '';		// to string
	
	var dp = ttotal.indexOf('.');
	
	if (dp != -1) {
		if (dp + 3 < ttotal.length) {
			// round up 1 penny if 3rd decimal point is > 4
			if ((ttotal.charAt(dp + 3) - 0) > 4) {
				total = ttotal.substr(0, dp + 3) - 0 + 0.01;
				ttotal = total + '';		// to string
			}
		}
		
		dp = ttotal.indexOf('.');
		
		if (dp != -1) {		
			while (dp + 2 >= ttotal.length) 
				ttotal += '0';
		
			ttotal = ttotal.substr(0, dp + 3);
		}
		else
			ttotal += '.00';
	}
	else
		ttotal += '.00';
	
	return ttotal;
	*/
}

window.requestAnimationFrame = (function(){
  return  window.requestAnimationFrame       ||
          window.webkitRequestAnimationFrame ||
          window.mozRequestAnimationFrame    ||
          function( callback ){
            window.setTimeout(callback, 50);		// try 20 fps
          };
})();

if (!window.location.origin) 
	window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');

var dc = {
	util: {
		Struct: {
			isScalar: function(v) {
				if (dc.util.String.isString(v))
					return true;
				
				if (dc.util.Number.isNumber(v))
					return true;
				
				if (dc.util.Binary.isBinary(v))
					return true;
				
				if (dc.util.Boolean.isBoolean(v))
					return true;
					
				return false;
			},
			isRecord: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if (v instanceof Array)
					return false;
					
				if (v instanceof Object) 
					return true;
					
				return false;
			},
			isList: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if (v instanceof Array)
					return true;
					
				return false;
			},
			isComposite: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if ((v instanceof Object) || (v instanceof Array)) 
					return true;
					
				return false;
			},
			isNotEmpty: function(v) {
				return !dc.util.Struct.isEmpty(v);
			},
			isEmpty: function(v) {
				if ((typeof v == 'undefined') || (v == null))
					return true;
					
				if (dc.util.String.isString(v))
					return (dc.util.String.toString(v).length == 0);
				
				if (dc.util.Number.isNumber(v))
					return false;		// a number is not empty
				
				if (dc.util.Binary.isBinary(v))
					return false;		// TODO support and check length
				
				if (dc.util.Boolean.isBoolean(v))
					return false;		// a bool is not empty
				
				if (v instanceof Array)
					return (v.length == 0);
					
				if (v instanceof Object) {
				    for (var p in v) 
				        if (v.hasOwnProperty(p)) 
				        	return false;
			    }
			    
			    return true;
			}
		},
		List: {
			sortObjects: function(sfield) {
				return function(a, b) { 
					var av = a[sfield];
					var bv = b[sfield];
					
					if ((av === null) && (bv === null))
						return 0;
						
					if (av === null) 
						return -1;
						
					if (bv === null) 
						return -1;
						
					if (av < bv)
						return -1;
						
					if (av > bv)
						return 1;
						
					return 0;
				}
			},
			sortDescObjects: function(sfield) {
				return function(a, b) { 
					var av = a[sfield];
					var bv = b[sfield];
					
					if ((av === null) && (bv === null))
						return 0;
						
					if (av === null) 
						return 1;
						
					if (bv === null) 
						return 1;
						
					if (av < bv)
						return 1;
						
					if (av > bv)
						return -1;
						
					return 0;
				}
			}
		},
		Number: {
			isNumber: function(n) {
				return !isNaN(parseFloat(n)) && isFinite(n);
			},
			toNumber: function(n) {
				if (isNaN(parseFloat(n)) || !isFinite(n))
					return null;
			
				return parseFloat(n);
			},
			toNumberStrict: function(n) {
				if (isNaN(parseFloat(n)) || !isFinite(n))
					return 0;
			
				return parseFloat(n);
			}			
		},
		String: {
			// in dc dates are strings because dates are strings during validation and during transport.
			isString: function(v) {
				return ((v instanceof Date) || (v instanceof String) || (typeof v == 'string'));
			},
			toString: function(v) {
				if (v instanceof Date) 
					v = v.formatStdDateTime();
				else if (dc.util.String.isString(v))
					v = v.toString();
				else if (dc.util.Number.isNumber(v))
					v = dc.util.Number.toNumber(v) + '';
				else if (dc.util.Boolean.isBoolean(v))
					v = dc.util.Boolean.toBoolean(v) ? 'True' : 'False';
					
				if (typeof v != 'string')
					return null;
				
				return v;
			},
			isEmpty: function(v) {
				v = dc.util.toString(v);
					
				if (v && (v.length == 0))
					return true;
			
				return (v == null);
			}
		},
		// TODO add support
		Binary: {
			isBinary: function(v) {
				return false;
			},
			toBinary: function(v) {
				return null;
			}
		},
		Boolean: {
			isBoolean: function(v) {
				return ((v instanceof Boolean) || (typeof v == 'boolean'));
			},
			toBoolean: function(v) {
				if (v instanceof Boolean)
					return v.valueOf();
				else if (dc.util.String.isString(v))
					v = (v.toLowerCase().toString() == 'true');
					
				if (typeof v == 'boolean')
					return v;

				if (v)
					return true;
										
				return false;
			}
		},		
		Dialog: {
			show : function(msg) {
				alert(msg);
			}
		},
		Cookies: {
			getCookie: function(name) {
				var i,x,y,ARRcookies = document.cookie.split(";");
				
				for (var i=0;i<ARRcookies.length;i++) {
				  var x=ARRcookies[i].substr(0,ARRcookies[i].indexOf("="));
				  var y=ARRcookies[i].substr(ARRcookies[i].indexOf("=")+1);
				  x=x.replace(/^\s+|\s+$/g,"");
				  if (x==name)
					return unescape(y);					
				}
			},
			setCookie: function(name,value,exdays,path,domain,secure) {
				var exdate = new Date();
				
				if (exdays)
					exdate.setDate(exdate.getDate() + exdays);
				
				document.cookie=name + "=" + escape(value) 
					+ ( exdays ? "; expires="+exdate.toUTCString() : "")
					+ ( path ? ";path=" + path : "" ) 
					+ ( domain ? ";domain=" + domain : "" ) 
					+ ( secure ? ";secure" : "" );					
			},
			deleteCookie: function(name, path, domain) {
				document.cookie = name + "=" 
					+ ( path  ? ";path=" + path : "") 
					+ ( domain ? ";domain=" + domain : "" ) 
					+ ";expires=Thu, 01-Jan-1970 00:00:01 GMT";
			}
		},
		Date: {
			zToMoment: function(z) {
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true);
			},
		
			formatZMomentLocal: function(m) {
				return m.local().format('MMM Do YYYY, h:mm:ss a Z');
			},
		
			formatZLocal: function(z) {
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true).local().format('MMM Do YYYY, h:mm:ss a Z');
			},
		
			formatZLocalMedium: function(z) {
				if (z.indexOf('T') == -1)
					return z;
					 
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true).local().format('MM-DD-YYYY h:mm:ss a');
			},
		
			toUtc : function(date) {
				var year = date.getUTCFullYear();
				var month = date.getUTCMonth();
				var day = date.getUTCDate();
				var hours = date.getUTCHours();
				var minutes = date.getUTCMinutes();
				var seconds = date.getUTCSeconds();
				var ms = date.getUTCMilliseconds();
				
				return new Date(year, month, day, hours, minutes, seconds, ms);
			},
			
			stamp: function() {
				return moment.utc().format('YYYYMMDDTHHmmssSSS') + 'Z';
			},
						
			toMonth : function(date) {
				var month = date.getMonth();
				
				if (month == 0)
					return 'January';
				
				if (month == 1)
					return 'February';
				
				if (month == 2)
					return 'March';
				
				if (month == 3)
					return 'April';
				
				if (month == 4)
					return 'May';
				
				if (month == 5)
					return 'June';
				
				if (month == 6)
					return 'July';
				
				if (month == 7)
					return 'August';
				
				if (month == 8)
					return 'September';
				
				if (month == 9)
					return 'October';
				
				if (month == 10)
					return 'November';
				
				if (month == 11)
					return 'December';
					
				return null;
			},
			toMonthAbbr : function(date) {
				var month = date.getMonth();
				
				if (month == 0)
					return 'Jan';
				
				if (month == 1)
					return 'Feb';
				
				if (month == 2)
					return 'Mar';
				
				if (month == 3)
					return 'Apr';
				
				if (month == 4)
					return 'May';
				
				if (month == 5)
					return 'Jun';
				
				if (month == 6)
					return 'Jul';
				
				if (month == 7)
					return 'Aug';
				
				if (month == 8)
					return 'Sep';
				
				if (month == 9)
					return 'Oct';
				
				if (month == 10)
					return 'Nov';
				
				if (month == 11)
					return 'Dec';
					
				return null;
			},
			toShortTime: function(date) {
				var hrs = date.getHours();
				var mins = date.getMinutes();
				
				if (mins < 10)
					mins = '0' + mins;
				
				if (hrs > 12)
					return (hrs - 12) + ':' + mins + ' pm';
					
				return hrs + ':' + mins + ' am';
			},
			formatZone: function(z) {
				if (z == 'America/Anchorage')
					return 'Alaska';
				if (z == 'America/Phoenix')
					return 'Arizona';
				if (z == 'America/Chicago')
					return 'Central';
				if (z == 'America/New_York')
					return 'Eastern';
				if (z == 'Pacific/Honolulu')
					return 'Hawaii';
				if (z == 'America/Indiana/Indianapolis')
					return 'Indiana-East';
				if (z == 'America/Indiana/Knox')
					return 'Indiana-Starke';
				if (z == 'America/Detroit')
					return 'Michigan';
				if (z == 'America/Denver')
					return 'Mountain';
				if (z == 'America/Los_Angeles')
					return 'Pacific';
					
				return z;
			}
		},
		Http: {
			getParam : function(name) {
			    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			},
			getHashParam : function(name) {
			    var match = RegExp('[?&#]' + name + '=([^&]*)').exec(window.location.hash);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			},
			getFromParams : function(params,name) {
			    var match = RegExp('[?&#]' + name + '=([^&]*)').exec(params);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			}
		},
		Ws: {
			getConnUrl: function(path) {
				var url = (window.location.protocol == 'https:') ? 'wss': 'ws';
				
				return url + "://" + window.location.hostname + (window.location.port ? ':' + window.location.port: '') + path;
			}
		},
		Form: {
			matchTextInput : function(input, org, ignore) {
				if (input == ignore)
					input = null;
					
				// circumvent null != "" issue
				if (!input && !org)
					return true;
	
				return (input == org);
			}
		},
		Params: {
			// retrive 
			get : function(offset) {
				var path = location.pathname;
				offset++;  // for initial /

				while ((offset > 0) && (path.length > 0)) {
					if (path[0] == '/')
						offset--;

					path = (path.length > 1) ? path.substr(1) : '';
				}

				return path.split('/')[0];
			},
			getFrom : function(offset) {
				var path = location.pathname;
				offset++;  // for initial /

				while ((offset > 0) && (path.length > 0)) {
					if (path[0] == '/')
						offset--;

					path = (path.length > 1) ? path.substr(1) : '';
				}

				return path.split('/');
			},
			is_touch_device: function() {
				 return (('ontouchstart' in window)
				      || (navigator.MaxTouchPoints > 0)
				      || (navigator.msMaxTouchPoints > 0));
			}
		},
		Uuid: {
			EMPTY : '00000000-0000-0000-0000-000000000000',
			create : function() {
				var _padLeft = function (paddingString, width, replacementChar) {
					return paddingString.length >= width ? paddingString : _padLeft(replacementChar + paddingString, width, replacementChar || ' ');
				};

				var _s4 = function (number) {
					var hexadecimalResult = number.toString(16);
					return _padLeft(hexadecimalResult, 4, '0');
				};

				var _cryptoGuid = function () {
					var buffer = new window.Uint16Array(8);
					window.crypto.getRandomValues(buffer);
					return [_s4(buffer[0]) + _s4(buffer[1]), _s4(buffer[2]), _s4(buffer[3]), _s4(buffer[4]), _s4(buffer[5]) + _s4(buffer[6]) + _s4(buffer[7])].join('-');
				};

				var _guid = function () {
					var currentDateMilliseconds = new Date().getTime();
					return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (currentChar) {
						var randomChar = (currentDateMilliseconds + Math.random() * 16) % 16 | 0;
						currentDateMilliseconds = Math.floor(currentDateMilliseconds / 16);
						return (currentChar === 'x' ? randomChar : (randomChar & 0x7 | 0x8)).toString(16);
					});
				};

				return (window.crypto && window.crypto.getRandomValues) ? _cryptoGuid() : _guid();
			}
		},
		SocialMedia: {
			withFacebook: function(cb) {
				if (!window.FB) {
					console.log('loading facebook api');
					
					$.getScript('https://connect.facebook.net/en_US/all.js', function(){
						FB.init({ appId: dc.handler.settings.fbAppId, version: 'v2.2' });     
			
						cb();			
					});
				}
				else {
					cb();			
				}
			}  
		},
		Messages: {
			findExitEntry : function(list) {
				if (!dc.util.Struct.isList(list)) 
					return null;
			
				var firsterror = null;
				
				for (var i = list.length - 1; i >= 0; i--) {
					var msg = list[i];
					
					if ("Error" == msg.Level)
						firsterror = msg;
				
					if (dc.util.Struct.isList(msg.Tags)) {
						for (var t = 0; t < msg.Tags.length; t++) {
							if (msg.Tags[t] == 'Exit')
								return (firsterror != null) ? firsterror : msg;
						}
					}
				}

				return firsterror;
			}
		},
		Misc: {
			sleep: function(millis) {
				var start = new Date().getTime();

				for (var i = 0; i < 1e7; i++) {
					if ((new Date().getTime() - start) > millis)
						break;
				}
  			}		
		},
		Crypto: {
			makeSimpleKey : function() {
				var text = "";
				var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

				for( var i=0; i < 16; i++ )
					text += possible.charAt(Math.floor(Math.random() * possible.length));

				return text;
			},
			
			/**
			 * Formatting for AES functions
			 */
			_cryptoFormatter: null,
			
			/**
			 * Initialize Crypto (no need to call in code)
			 */
			init: function() {
				if (!dc.util.Crypto._cryptoFormatter)
					dc.util.Crypto._cryptoFormatter = {
						stringify: function (cipherParams) {
							// create json object with ciphertext
							var jsonObj = {
								ct: cipherParams.ciphertext.toString(CryptoJS.enc.Base64)
							};
		
							// optionally add iv and salt
							if (cipherParams.iv) {
								jsonObj.iv = cipherParams.iv.toString();
							}
							if (cipherParams.salt) {
								jsonObj.s = cipherParams.salt.toString();
							}
		
							// stringify json object
							return JSON.stringify(jsonObj);
						},
		
						parse: function (jsonStr) {
							// parse json string
							var jsonObj = JSON.parse(jsonStr);
		
							// extract ciphertext from json object, and create cipher params object
							var cipherParams = CryptoJS.lib.CipherParams.create({
								ciphertext: CryptoJS.enc.Base64.parse(jsonObj.ct)
							});
		
							// optionally extract iv and salt
							if (jsonObj.iv) {
								cipherParams.iv = CryptoJS.enc.Hex.parse(jsonObj.iv)
							}
							if (jsonObj.s) {
								cipherParams.salt = CryptoJS.enc.Hex.parse(jsonObj.s)
							}
		
							return cipherParams;
						}
					};
			},
			
			encrypt : function(value, phrase) {
				dc.util.Crypto.init();
				
				return CryptoJS.AES.encrypt(value, phrase, { format: dc.util.Crypto._cryptoFormatter }).toString();
			},
			
			decrypt : function(value, phrase) {
				dc.util.Crypto.init();
				
				var decrypted = CryptoJS.AES.decrypt(value, phrase, { format: dc.util.Crypto._cryptoFormatter });
				return decrypted.toString(CryptoJS.enc.Utf8);
			}
		}
	},
	lang: {
		Xml: {
			// consider  https://www.fyneworks.com/jquery/xml-to-json/
			xmlToObj: function(xml) {
				var res = { };
				
				$.each(this.attributes, function(i, attrib){
					res[attrib.name] = attrib.value;					
				});
				
				return res;
			}
		},
		
		// TODO put DebugLevel enum in and use it (below)
		
		Dict: {
			_tokens: { },
			
			get: function(token) {
				return dc.lang.Dict._tokens[token];
			},
			load: function(tokens) {
				if (!tokens)
					return;
					
				for (var i in tokens) {
					var dt = tokens[i];
					dc.lang.Dict._tokens[dt.Token] = dt.Value;
				} 
			},
			tr: function(token, params) {
				var val = dc.lang.Dict._tokens[token];
				
				if (!dc.util.Struct.isList(params))
					params = []; 
				
				var msg = '';
		        var lpos = 0;
		        var bpos = val.indexOf('{$');
		
		        while (bpos != -1) {
		            var epos = val.indexOf("}", bpos);
		            if (epos == -1) 
		            	break;
		
		            msg += val.substring(lpos, bpos);
		
		            lpos = epos + 1;
		
		            var varname = val.substring(bpos + 2, epos).trim();
		
		            // TODO add some formatting features for numbers/datetimes
		            
		            var parampos = varname - 1;
		            
		            if (parampos <= params.length) 
		            	msg += params[parampos];
		            else 
		                msg += val.substring(bpos, epos + 1);
		
		            bpos = val.indexOf("{$", epos);
		        }
		
		        msg += val.substring(lpos);
				
				return msg;
			}
		},
		
		// TODO put in Result and Callback features too
		OperationResult: function(loglevel) {
			this.Code = 0;		// error code, non-zero means error, only first error code is tracked 
			this.Message = null;		// error code, non-zero means error, first code is tracked 
			this.Messages = [];
			this.LogLevel = loglevel ? loglevel : 0;
    
			this.trace = function(code, msg) {
				this.log(4, code, msg);
			};
			
			this.info = function(code, msg) {		
				this.log(3, code, msg);
			};
			
			this.warn = function(code, msg) {
				this.log(2, code, msg);
			};
			
			this.error = function(code, msg) {
				this.log(1, code, msg);
			};
			
			this.exit = function(code, msg) {
				this.Code = code;
				this.Message = msg;
				
				this.log(3, code, msg);
			};
			
			this.log = function(lvl, code, msg) {
				this.Messages.push({
					Occurred: moment().format('MMM Do YYYY, h:mm:ss a Z'),
					Level: lvl,
					Code: code,
					Message: msg
				});
				
				if ((lvl == 1) && (this.Code == 0)) {
					this.Code = code;
					this.Message = msg;
				}		
			};
			
			this.traceTr = function(code, params) {
				this.logTr(4, code, params);
			};
			
			this.infoTr = function(code, params) {		
				this.logTr(3, code, params);
			};
			
			this.warnTr = function(code, params) {
				this.logTr(2, code, params);
			};
			
			this.errorTr = function(code, params) {
				this.logTr(1, code, params);
			};
			
			this.exitTr = function(code, params) {
				var msg = dc.lang.Dict.tr("_code_" + code, params)
				
				this.Code = code;
				this.Message = msg;
				
				this.logTr(3, code, params);
			};
			
			// params should be an array
			this.logTr = function(lvl, code, params) {
				var msg = dc.lang.Dict.tr("_code_" + code, params)
				
				this.Messages.push({
					Occurred: moment().format('MMM Do YYYY, h:mm:ss a Z'),
					Level: lvl,
					Code: code,
					Message: msg
				});
				
				if ((lvl == 1) && (this.Code == 0)) {
					this.Code = code;
					this.Message = msg;
				}		
			};
		
			// from another result
			this.copyMessages = function(res) {		
				for (var i = 0; i < res.Messages.length; i++) {
					var msg = res.Messages[i];
					this.Messages.push(msg);
					
					if ((msg.Level == 1) && (this.Code == 0)) {
						this.Code = msg.Code;
						this.Message = msg.Message;
					}
				}		
				
				// if not in list, still copy top error message
				if (this.Code == 0) {
					this.Code = res.Code;
					this.Message = res.Message;
				}
			};
		
			this.hasErrors = function() {
				return (this.Code != 0);
			};		
		},
		CountDownCallback: function(cnt, callback) {
			this.cnt = cnt;
			this.callback = callback;
			
			this.dec = function() {
				this.cnt--;
				
				if (this.cnt == 0)
					this.callback();
			};
			
			this.inc = function() {
				this.cnt++;
			};
		}
	}
}

$.fn.dcVal = function() {
    var v = this.attr('Value');
    
    if (!v)
    	v = this.attr('value');
    
    if (!v)
    	v = this.text();
    
    return v;
};

$.fn.dcMD = function(txt) {
	marked.setOptions({
	  renderer: new marked.Renderer(),
	  gfm: true,
	  tables: true,
	  breaks: true,
	  pedantic: false,
	  sanitize: false,
	  smartLists: true,
	  smartypants: false
	});

	this.html(marked(txt));
}