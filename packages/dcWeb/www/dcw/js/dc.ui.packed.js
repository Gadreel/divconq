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
/*
	PAGE:
	
	{
		Name: '',
		Title: '',
		RequireLibs: [ path, path, path ],
			- standard JS libs (ignore in Loader.load these were already loaded before first page load)
			
		RequireModules: [ path, path, path ],
			- other modules
			
		Icons: [
			{
				Module: 'name',
				Icons: [ name, name, name ]
			}
		],
			- list module and name for each icon used
			
		Fonts: [
			{
				Module: 'name',
				Fonts: [ name, name, name ]
			}
		],
			- list module and name for each font used
			
		Layout: [
		],
			- structure for layout
		
		Functions: {
			'name': func...
		}
			- functions for use with layout
			
		-- reserved functions
			-- page level
				Load	
				
				Save	

			-- form level
					
				LoadRecord
				 
				AfterLoadRecord
				 
				AfterLoad
				
				SaveRecord
				
				AfterSaveRecord
				
				AfterSave
			
	}
	
	entry:
	
	{
		Name: [name],
		
		Hash: [hash],
		
		Loaded: t/f - once t then does a thaw next visit to this entry
		
		Params: { 
			name: [param]
		},
			- parameters for this page
		
		Store: {
			'name': obj
		}
			- general data for this page 
		
		Forms: {
			'fname': {
				Name: n,
				SaveButton: n,
				AlwaysNew: t/f,
				Inputs: { },
				ValidationRules: { },
				ValidationMessages: { },
				RecordOrder: [ "Default" ],
				AsNew: { },
				InternalValues: { }
				
			}
		}
			- forms data for this page 
	}
*/

dc.pui = {
	Loader: {
		__content: null,
		__loadPageHash: null,
		__current: null,
		__devmode: false,
		__hashes: { },
		__pages: { },
		__mainPage: '/dct/pages/Main',
		
		init: function() {
			dc.pui.Loader.__content = document.getElementById('pageContent');

			$(window).on('hashchange', function() {
				var hash = location.hash.substring(1); 
				
				dc.pui.Loader.manifestPage(dc.pui.Loader.__hashes[hash]);
			});
			
			/* no swipe!!
			$(document).on("swipeleft swiperight", "#pgMain", function( e ) {
				// We check if there is no open panel on the page because otherwise
				// a swipe to close the left panel would also open the right panel (and v.v.).
				// We do this by checking the data that the framework stores on the page element (panel: open).
				if ( $(".ui-page-active").jqmData("panel") !== "open") {
					if (e.type === "swipeleft") {
						$( "#notifyPanel" ).panel( "open" );
					} 
				}
			});
			*/
			
			// setup some validators
			
    		$.validator.addMethod('dcDataType', dc.pui.Page.validateInput, 'Invalid format.');
    		
    		$.validator.addMethod('dcJson', function(value, element, param) {
				if (dc.util.Struct.isEmpty(value))
					return this.optional(element);
					
    			try { 
    				JSON.parse(value); 
    				return true; 
    			} 
    			catch (x) { } 
    			
    			return false;
			}, 'Invalid JSON.');
		},
		
		setMainPage: function(v) {
			dc.pui.Loader.__mainPage = v;
		},
		
		loadMainPage: function() {
			/*
				TODO lookup user's landing page
				
			var msg = {
				Op: "SignIn",
				Credentials: creds
			};
			
			dc.comm.sendMessage(msg, function(rmsg) { 
				if (rmsg.Result == 0) {
					var resdata = '/dct/pages/SignIn';  // rmsg.Body.;
					
					dc.pui.Loader.loadPage('/dct/pages/SignIn', { PTag: 'super' });
				}
			});
			*/

			dc.pui.Loader.loadPage(dc.pui.Loader.__mainPage);
		},
		
		loadPage: function(page, params) {
			var entry = {
				Name: page, 
				Params: params ? params : { },
				Store: { },
				Forms: { },
				call: function(method) {
					var page = dc.pui.Loader.__pages[this.Name];
					
					if (page && page.Functions[method]) 
						page.Functions[method].apply(this, Array.prototype.slice.call(arguments, 1));
				},
				form: function(name) {
					if (name)
						return this.Forms[name];
				
					// if no name then return the first we find
					for (var name2 in this.Forms) 
						if (this.Forms.hasOwnProperty(name2)) 
							return this.Forms[name2];
					
					return null;
				}
			};
			
			dc.pui.Loader.__loadPageHash = dc.pui.Loader.addPageEntry(entry);
		
			// if page is already loaded then show it
			if (!dc.pui.Loader.__devmode && dc.pui.Loader.__pages[page]) {
				dc.pui.Loader.resumePageLoad();
				return;
			}
			
			var script = document.createElement('script');
			script.src = page + '?nocache=' + dc.util.Crypto.makeSimpleKey();
			script.id = 'page' + page.replace(/\//g,'.');
			document.head.appendChild(script);
		},
		
		resumePageLoad: function() {
			window.location.hash = dc.pui.Loader.__loadPageHash;				
		},
		
		addPageEntry: function(entry) {
			var hash = dc.util.Uuid.create().replace(/-/g,'');
			
			entry.Hash = hash;
			
			dc.pui.Loader.__hashes[hash] = entry;
			
			return hash;
		},
		
		addPageDefinition: function(name, def) {
			dc.pui.Loader.__pages[name] = def;
			
			// get rid of the script tag so we can reload the page again later
			$('#page' + name.replace(/\//g,'.')).remove();
		},
		
		manifestPage: function(entry) {
			dc.pui.Loader.__current = entry;
			
			var page = dc.pui.Loader.__pages[entry.Name];
			
			if (dc.util.String.isString(page.Title))
				$('#lblPageTitle h1').text(page.Title);
			
			$(dc.pui.Loader.__content).empty().promise().then(function() {
				// layout using 'pageContent' as the top of the chain of parents
				dc.pui.Page.layout(page, entry, page.Layout, {
					Element: $(dc.pui.Loader.__content),
					Parent: null,
					Definition: null
				});
				
				$(dc.pui.Loader.__content).enhanceWithin().promise().then(function() {			
					$("html, body").animate({ scrollTop: 0 }, "slow");
					
					// loadcntdwn can be used by the Load function to delay loading the form
					// if Load needs to do an async operation
					var loadcntdwn = new dc.lang.CountDownCallback(1, function() { 
						// now load forms, if any
						for (var name in entry.Forms) {
							if (entry.Forms.hasOwnProperty(name)) {
								if (entry.Loaded)
									dc.pui.Page.thawForm(page, entry, name, function() {
										//console.log('form thawed cb');
									});
								else
									dc.pui.Page.loadForm(page, entry, name, function() {
										//console.log('form loaded cb');
									});
							}
						}
							
						entry.Loaded = true;
						
						$.mobile.loading('hide'); 
					});
					
					//if (entry.Loaded) {
					//	if (page.Functions.Thaw) 
					//		page.Functions.Thaw.call(entry, dc.pui.Loader.__content, loadcntdwn);
					//}
					//else if (page.Functions.Load) 
					if (page.Functions.Load) 
						page.Functions.Load.call(entry, dc.pui.Loader.__content, loadcntdwn);
						
					loadcntdwn.dec();						
				});
			});
		},
		
		currentPageEntry: function() {
			return dc.pui.Loader.__current;
		}
	},
	Page: {		
		layout: function(page, entry, children, parentchain) {
			// TODO more layout
			
			if (!children)
				return;
				
			for (var i = 0; i < children.length; i++) {
				var child = children[i];
				
				// text nodes are added
				if (dc.util.String.isString(child)) {
					parentchain.Element.append(child);
					continue;
				}
				
				if (dc.util.String.isString(child.Element)) {
					var node = null;

					// TODO move some of these down into dc.pui.Controls
					if (child.Element == 'Link') {
						node = $('<a href="#" data-role="button" data-theme="a" data-mini="true" data-inline="true"></a>');
						
						if (dc.util.String.isString(child.Label))
							node.text(child.Label);
						
						if (dc.util.String.isString(child.Icon))
							node.attr('data-icon', child.Icon);
						
						if (dc.util.String.isString(child.Click)) 
							node.click(page.Functions[child.Click], function(e) {
								if (e.data)
									e.data.call(entry, e, this);
								
								e.preventDefault();
								return false;
							});
					}
					else if (child.Element == 'WideLink') {
						node = $('<a href="#" data-role="button" data-theme="a" data-iconpos="right" data-mini="true"></a>');
						
						if (dc.util.String.isString(child.Label))
							node.text(child.Label);
						
						if (dc.util.String.isString(child.Icon))
							node.attr('data-icon', child.Icon);
						
						if (dc.util.String.isString(child.Click)) 
							node.click(page.Functions[child.Click], function(e) {
								if (e.data)
									e.data.call(entry, e, this);
								
								e.preventDefault();
								return false;
							});
					}
					else if (child.Element == 'SubmitButton') {
						node = $('<input type="submit" data-theme="a" data-mini="true" data-inline="true"></a>');
						
						if (dc.util.String.isString(child.Label))
							node.attr('value', child.Label);
						
						if (dc.util.String.isString(child.Icon))
							node.attr('data-icon', child.Icon);
						
						if (dc.util.String.isString(child.Click)) 
							node.click(page.Functions[child.Click], function(e) {
								if (e.data)
									e.data.call(entry, e);
								
								e.preventDefault();
								return false;
							});
							
						var id = child.Id;
						
						if (!id) 
							id = child.id;
						
						if (!id) 
							id = dc.util.Uuid.create();

						node.attr('id', id);
							
						var form = dc.pui.Page.findFormForLayout(page, entry, parentchain);
						
						if (form) 
							form.SaveButton = id;							
					}
					else if (child.Element == 'Form') {
						node = $('<form />');
						
						if (dc.util.String.isString(child.Name)) {
							node.attr('id', 'frm' + child.Name);

							// don't add form info on a thaw, reuse but rebuild the inputs
							if (entry.Forms[child.Name]) {
								entry.Forms[child.Name].Inputs = { };
							}
							else {
								// clone and use form
								var form = $.extend(true, { 
									Id: 'frm' + child.Name,
									Inputs: { },
									ValidationRules: { },
									ValidationMessages: { },
									RecordOrder: [ "Default" ],
									AsNew: { },
									InternalValues: { }
								}, child);
								
								form.AlwaysNew = (form.AlwaysNew == 'true');
								
								form.input = function(name) {
									return this.Inputs[name];
								};
								
								form.query = function(name) {
									var inp = this.Inputs[name];
									
									return $('#' + inp.Id);
								};
								
								entry.Forms[form.Name] = form;
							}
						}
					}
					else if (child.Element == 'FieldContainer') {
						node = $('<div data-role="fieldcontain" />');
					}
					else if (child.Element == 'Label') {
						var id = child.Id;
						
						if (!id) 
							id = dc.util.Uuid.create();
							
						node = $('<div />');
						node.attr('id', id);
					}
					else if (child.Element == 'HorizRadioGroup') {
						node = $('<div data-role="fieldcontain" />');
						
						fsnode = $('<fieldset data-role="controlgroup" data-type="horizontal" data-mini="true" />');
						
						if (child.Required && child.Label)
							fsnode.append('<legend>' + child.Label + ' <span class="fldreq">*</span></legend>');
						else if (child.Label)
							fsnode.append('<legend>' + child.Label + '</legend>');
						
						if (child.Name)
							fsnode.append('<label style="display: none;" for="' + child.Name + '" class="error">Please choose one.</label>');
						
						node.append(fsnode);
					}
					else if (child.Element == 'RadioButton') {
						var form = dc.pui.Page.findFormForLayout(page, entry, parentchain);
						
						if (!form || !parentchain.Definition.Name) 
							continue;
							
						var fname = parentchain.Definition.Name;
							
						var input = form.Inputs[fname];
						var rule = form.ValidationRules[fname];
						
						if (!input) {
							var rec = parentchain.Definition.Record;
							
							if (!rec) 
								rec = 'Default';

							var id = dc.util.Uuid.create();
							
							input = {
								Field: fname,
								Record: rec,
								Type: 'RadioSelect',
								Id: id
							};
							
							form.Inputs[fname] = input;
							
							rule = { };
							
							if (parentchain.Definition.Required == 'true')
								rule.required = true;
							
							form.ValidationRules[fname] = rule;
						}
						
						var val = dc.util.Struct.isEmpty(child.Value) ? child.Label : child.Value;
						var id = input.Id + '-' + val;
						
						node = $('<input type="radio" />');
						node.attr('id', id);
						node.attr('name', fname);
						node.attr('value', val);
						node.attr('data-dc-required', rule.required ? 'true' : 'false')
						
						parentchain.Element.find('fieldset label').last()
							.before(node)
							.before('<label for="' + id + '">' + child.Label + '</label>');
						
						// skip further processing on this control
						node = null;
					}
					else if ((child.Element == 'TextInput') || (child.Element == 'PasswordInput') || (child.Element == 'RadioSelect') || (child.Element == 'Range') || (child.Element == 'Select') || (child.Element == 'YesNo') || (child.Element == 'TextArea') || (child.Element == 'HiddenInput')) {
						var form = dc.pui.Page.findFormForLayout(page, entry, parentchain);
						
						if (!form) 
							continue;
							
						//console.log('found form: ' + form.Name);
							
						var fname = child.Name;
						
						if (!fname)
							return;
						
						var dtype = child.DataType;
						
						if (!dtype) 
							dtype = 'String';
							
						var defrule = { };
						
						var rec = child.Record;
						
						if (!rec) 
							rec = 'Default';
						
						var id = child.Id;
						
						if (!id) 
							id = dc.util.Uuid.create();
						
						var itype = child.Element;
						
						if (itype == 'RadioSelect') {
							node = $('<input type="radio" data-mini="true" />');
							
							// ??? id = id.substr(0, id.indexOf('-'));
						}
						else if (itype == 'Range') {
							node = $('<input type="range" data-mini="true" />');
						}
						else if (itype == 'Select') {
							node = $('<select data-mini="true" />');
						}
						else if (itype == 'YesNo') {
							dtype = 'Boolean';
							defrule.required = true;
							
							node = $('<select data-role="flipswitch" data-mini="true"> \
									<option Value="false">No</option> \
									<option Value="true">Yes</option> \
								</select>');
							
							//<label style="display: none;" for="cbRememberMe" class="error">Please choose one.</label> 
						}
						else if (itype == 'TextArea') {
							node = $('<textarea data-mini="true" />');
						}
						else if (itype == 'PasswordInput') {
							node = $('<input type="password" data-mini="true" />');
						}
						else if (itype == 'HiddenInput') {
							node = $('<input type="hidden" data-mini="true" />');
						}
						else {
							node = $('<input type="text" data-mini="true" />');
						}
						
						var input = {
							Field: fname,
							DataType: dtype,
							Record: rec,
							Type: itype,
							Id: id
						};
					
						form.Inputs[fname] = input;
							
						node.attr('id', id);
						node.attr('name', fname);
						
						// clone the rule or start blank
						var rule = child.Rule ? $.extend(true, defrule, child.Rule) : defrule;
						
						rule.dcDataType = dtype;
						
						// don't overwrite required in rule unless Required prop exists
						if (child.Required)
							rule.required = (child.Required == 'true');
						
						if (child.Pattern)
							rule.pattern = child.Pattern;
						
						form.ValidationRules[fname] = rule;
						
						var vmsg = $(this).attr('data-dc-message');
						
						if (child.ErrorMessage)
							form.ValidationMessages[fname] = child.ErrorMessage;
					}
					else {
						node = $('<' + child.Element + '>');
					}
					
					if (node) {
						if (child.Attributes) {
							for (var key in child.Attributes) 
								if (child.Attributes.hasOwnProperty(key)) 
									node.attr(key, child.Attributes[key]);
						}
						
						dc.pui.Page.layout(page, entry, child.Children, {
							Element: node,
							Parent: parentchain,
							Definition: child
						});
						
						// post processing
						if (child.Element == 'FieldContainer') {
							var fndLabelTarget = false;
							var lblText = child.Label ? child.Label : '';
						
							if (child.Children && child.Children.length) {
								var fldname = child.Children[0].Name;
								
								var form = dc.pui.Page.findFormForLayout(page, entry, parentchain);
								
								if (fldname && form && form.Inputs[fldname]) {
									var id = form.Inputs[fldname].Id;
									
									if (child.Children[0].Required)
										node.prepend('<label for="' + id + '">' + lblText + ' <span class="fldreq">*</span></label>');
									else
										node.prepend('<label for="' + id + '">' + lblText + ' </label>');
										
									fndLabelTarget = true;
								}
							}
							
							if (!fndLabelTarget) {
								var id = node.children().first().attr('id');
								
								if (id)
									node.prepend('<label for="' + id + '">' + lblText + ' </label>');
							}
						}
						
						parentchain.Element.append(node);
					}
					
					continue;
				}
			}
		},
		
		loadForm: function(page, entry, formname, callback) {
			var form = entry.Forms[formname];
			var fnode = $('#' + form.Id);
			
			//console.log('loading form: ' + formname + ' - ' + form);
			
			if(!form.RecordOrder) {
				callback();
				return;
			}
			
			$(fnode).validate({
				rules: form.ValidationRules || { },
				messages: form.ValidationMessages || { },
				invalidHandler: function() {
					dc.pui.Popup.alert('Missing or invalid inputs, please correct.');
				},
				submitHandler: function(form) {
					dc.pui.Page.saveForm(page, entry, formname, function() {
						if (page.Functions.Save) 
							page.Functions.Save.call(entry, dc.pui.Loader.__content);
						
						$.mobile.loading('hide'); 
					});
				
					return false;
				}
			});
			
			if (form.SaveButton)
				$('#' + form.SaveButton).click(function() { 
					$(fnode).validate().form(); 
				});			
			
			// build a queue of record names (copy array) to load 
			var rnames = form.RecordOrder.concat(); 
			
			var qAfter = function(event) {
				var funcname = form.Prefix ? form.Prefix +  'AfterLoadRecord' : 'AfterLoadRecord';
				
				// handler will change Data if necessary
				if (page.Functions[funcname]) 
					page.Functions[funcname].call(entry, event);
			
				dc.pui.Page.loadRecord.call(entry, formname, event.Record, event.Data, event.AsNew);
					
				// process next record in queue
				qProcess();
			};
			
			// define how to process the queue
			var qProcess = function() {
				// all done with loading
				if (rnames.length == 0) {
					var funcname = form.Prefix ? form.Prefix + 'AfterLoad' : 'AfterLoad';
				
					if (page.Functions[funcname]) 
						page.Functions[funcname].call(entry, event);
					
					callback();					
					return;
				}
					
				var rname = rnames.shift();
				
				var event = { 
					Record: rname
				};

				dc.pui.Page.initChanges.call(entry, formname, event.Record);
				
				// handler will set Message if we need to load data from bus and Data if 
				// we need to load a record they provide
				// also they should set AsNew = true if this is a new record
				var funcname = form.Prefix ? form.Prefix + 'LoadRecord' : 'LoadRecord';
			
				if (page.Functions[funcname]) 
					page.Functions[funcname].call(entry, event);

				if (event.Stop) {
					callback();
					return;
				}
				
				if (event.Message) {					
					dc.comm.sendMessage(event.Message, function (e) {
						if (e.Result != 0) { 
							var ignore = false;
							
							if (event.IgnoreResults) {
								for (var i = 0; i < event.IgnoreResults.length; i++) 
									if (event.IgnoreResults[i] == e.Result) {
										ignore = true;
										break;
									}
							}
							
							if (!ignore) {
								dc.pui.Popup.alert(e.Message);
								callback();
								return;
							}
						}
		
						event.Result = e;
						event.Data = e.Body;
						
						qAfter(event);
					}, null, true);
				}
				else {
					event.Result = 0;
					qAfter(event);
				}
			};
			
			// start the queue processing
			qProcess();
		},
		
		thawForm: function(page, entry, formname, callback) {
			var form = entry.Forms[formname];
			var fnode = $('#' + form.Id);
			
			//console.log('resuming form: ' + formname + ' - ' + form);
			
			if(!form.RecordOrder) {
				callback();
				return;
			}
			
			$(fnode).validate({
				rules: form.ValidationRules || { },
				messages: form.ValidationMessages || { },
				invalidHandler: function() {
					dc.pui.Popup.alert('Missing or invalid inputs, please correct.');
				},
				submitHandler: function(form) {
					dc.pui.Page.saveForm(page, entry, formname, function() {
						if (page.Functions.Save) 
							page.Functions.Save.call(entry, dc.pui.Loader.__content);
						
						$.mobile.loading('hide'); 
					});
				
					return false;
				}
			});
			
			if (form.SaveButton)
				$('#' + form.SaveButton).click(function() { 
					$(fnode).validate().form(); 
				});			
			
			// build a queue of record names (copy array) to load 
			var rnames = form.RecordOrder.concat(); 
			
			// define how to process the queue
			var qProcess = function() {
				// all done with thaw
				if (rnames.length == 0) {
					callback();					
					return;
				}
					
				var rname = rnames.shift();
				
				var event = { 
					Record: rname,
					Result: 0,
					Data: form.InternalValues,
					AsNew: form.AsNew[rname]
				};

				dc.pui.Page.loadRecord.call(entry, formname, event.Record, event.Data, event.AsNew);
					
				// process next record in queue
				qProcess();
			};
			
			// start the queue processing
			qProcess();
		},
		
		validate: function() {
			var entry = dc.pui.Loader.currentPageEntry();
			
			for (var name in entry.Forms) 
				if (entry.Forms.hasOwnProperty(name)) 
					$('#' + entry.Forms[name].Id).validate().form(); 
		},
		
		saveForm: function(page, entry, formname, callback) {
			var form = entry.Forms[formname];
			
			if (!form || !form.RecordOrder) {
				callback();
				return;
			}
			
			var fnode = $('#' + form.Id);
			
			//console.log('saving form: ' + formname + ' - ' + form);
						
			var anychanged = false;
			var event = { };
			
			var funcname = form.Prefix ? form.Prefix + 'BeforeSave' : 'BeforeSave';
		
			// do before save record event
			if (page.Functions[funcname]) 
				page.Functions[funcname].call(entry, event);

			if (event.Stop) {
				callback();
				return;
			}
					
			// build a queue of record names to load 
			var rnames = form.RecordOrder.concat(); 
				
			// define how to process the queue
			var qProcess = function() {
				// all done with loading
				if (rnames.length == 0) {
					// do after save record event
					var event = {
						NoChange: !anychanged
					}
			
					var funcname = form.Prefix ? form.Prefix + 'AfterSave' : 'AfterSave';
				
					if (page.Functions[funcname]) 
						page.Functions[funcname].call(entry, event);
				
					if (!event.Loading)
						$.mobile.loading('hide');
					
					if (event.Alert)
						dc.pui.Popup.alert(event.Alert);
					else if (event.DefaultSaved)
						dc.pui.Popup.alert(anychanged ? 'Saved' : 'No changes, nothing to save.');

					callback();
					return;
				}
				
				var rname = rnames.shift();

				if (!dc.pui.Page.isChanged.call(entry, formname, rname)) {
					// process next record in queue
					qProcess();
					return;
				}
				
				anychanged = true;
				
				var event = {
					Record: rname,
					Data: dc.pui.Page.getChanges.call(entry, formname, rname)
				}
				
				var savecntdwn = new dc.lang.CountDownCallback(1, function() { 
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert);
						callback();
						return;
					}
					else if (event.Stop) {
						$.mobile.loading('hide');
						callback();
						return;
					}
					
					if (event.Message) {					
						dc.comm.sendMessage(event.Message, function (e) {
							if (e.Result != 0) { 
								dc.pui.Popup.alert(e.Message);
								callback();
								return;
							}
						
							dc.pui.Page.clearChanges.call(entry, formname, rname);
					
							event.Result = e;
							event.Data = e.Body;
			
							var funcname = form.Prefix ? form.Prefix + 'AfterSaveRecord' : 'AfterSaveRecord';
						
							if (page.Functions[funcname]) 
								page.Functions[funcname].call(entry, event);
									
							if (event.Alert) {
								dc.pui.Popup.alert(event.Alert);
								callback();
								return;
							}
							else if (event.Stop) {
								$.mobile.loading('hide');
								callback();
								return;
							}
							
							// process next record in queue
							qProcess();
						});
					}
					else {
						dc.pui.Page.clearChanges.call(entry, formname, rname);
						
						// process next record in queue
						qProcess();
					}
				});
		
				event.CountDown = savecntdwn;
		
				// handler will set Message if we need to save data to bus  
				var funcname = form.Prefix ? form.Prefix + 'SaveRecord' : 'SaveRecord';
			
				if (page.Functions[funcname]) 
					page.Functions[funcname].call(entry, event);
					
				savecntdwn.dec();						
			};
			
			// start the queue processing
			qProcess();
		},
		
		findFormForLayout: function(page, entry, parentchain) {
			if (!parentchain)
				return null;
				
			if (parentchain.Definition.Element != 'Form') 
				return dc.pui.Page.findFormForLayout(page, entry, parentchain.Parent);
				
			return entry.Forms[parentchain.Definition.Name];
		},		
		
		formForInput: function(el) {
			var entry = dc.pui.Loader.currentPageEntry();
			var fel = $(el).closest('form');
			var id = $(fel).attr('id');
			
			for (var name in entry.Forms) 
				if (entry.Forms.hasOwnProperty(name)) {
					var f = entry.Forms[name];
					
					if (f.Id == id)
						return f;
				}
				
			return null;
		},		
		
		loadDefault: function(formname, data, asNew) {
			var entry = dc.pui.Loader.currentPageEntry();
			
			return dc.pui.Page.loadRecord.call(entry, formname, 'Default', data, asNew);
		},
		
		loadRecord: function(formname, recname, data, asNew) {
			// this = entry
			var form = this.Forms[formname];
			
			if (!form || !form.Inputs)
				return;
			
			if (asNew)
				form.AsNew[recname] = true;

			if (!data) 
				return;
			
			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];
					
					if ((iinfo.Record != recname) || !data.hasOwnProperty(iinfo.Field))
						continue;
					
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].Set)
						dc.pui.Controls[iinfo.Type].Set.call(this, form, iinfo, data[iinfo.Field]);
				}
			}
		},
		
		initChanges: function(formname, recname) { 
			// this = entry
			var form = this.Forms[formname];
			
			if (!form || !form.Inputs)
				return;
			
			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];

					if (iinfo.Record != recname)
						continue;
					
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].Set)
						dc.pui.Controls[iinfo.Type].Set.call(this, form, iinfo, null);
				}
			}
		},
		
		isDefaultChanged: function(formname) { 
			var entry = dc.pui.Loader.currentPageEntry();
			
			return dc.pui.Page.isChanged.call(entry, formname, 'Default');
		},
		
		isChanged: function(formname, recname) { 
			// this = entry
			var form = this.Forms[formname];
			
			if (!form || !form.Inputs)
				return false;

			if (form.AsNew[recname] || form.AlwaysNew)
				return true;

			var changed = false;
			
			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];

					if (iinfo.Record != recname)
						continue;
					
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].IsChanged)
						if (dc.pui.Controls[iinfo.Type].IsChanged.call(this, form, iinfo))
							changed = true;
				}
			}
			
			return changed;
		},
		
		getDefaultChanges: function(formname) { 
			var entry = dc.pui.Loader.currentPageEntry();
			
			return dc.pui.Page.getChanges.call(entry, formname, 'Default');
		},
		
		getChanges: function(formname, recname) { 
			// this = entry
			var form = this.Forms[formname];
			var changes = { };				
			
			if (!form || !form.Inputs)
				return changes;

			var asNew = (form.AsNew[recname] || form.AlwaysNew);

			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];

					if (iinfo.Record != recname)
						continue;
						
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].IsChanged && dc.pui.Controls[iinfo.Type].Get)
						if (asNew || dc.pui.Controls[iinfo.Type].IsChanged.call(this, form, iinfo)) 
							changes[name] = dc.pui.Controls[iinfo.Type].Get.call(this, form, iinfo);
				}
			}
			
			return changes;
		},
		
		clearDefaultChanges: function(formname) { 
			var entry = dc.pui.Loader.currentPageEntry();
			
			return dc.pui.Page.clearChanges.call(entry, formname, 'Default');
		},
		
		clearChanges: function(formname, recname) { 
			// this = entry
			var form = this.Forms[formname];
			
			if (!form || !form.Inputs)
				return;
			
			form.AsNew[recname] = false;							

			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];

					if (iinfo.Record != recname)
						continue;
					
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].ClearChanges)
						dc.pui.Controls[iinfo.Type].ClearChanges.call(this, form, iinfo);
				}
			}
		},
		
		validateInput: function(value, element, datatype) {
			if (dc.util.Struct.isEmpty(value))
				return this.optional(element);
				
			var entry = dc.pui.Loader.currentPageEntry();
			var iid = $(element).attr('id');
			var form = dc.pui.Page.formForInput(element);
			
			if (!form || !form.Inputs)
				return false;

			for (var name in form.Inputs) {
				if (form.Inputs.hasOwnProperty(name)) {
					var iinfo = form.Inputs[name];

					if (iinfo.Id != iid)
						continue;
					
					if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].Validate)
						return dc.pui.Controls[iinfo.Type].Validate.call(entry, form, iinfo, value);
				}
			}
					
			return false;
		},
		
		getInput: function(formname, field) { 
			var entry = dc.pui.Loader.currentPageEntry();
			var form = entry.Forms[formname];
			
			if (!form || !form.Inputs || !form.Inputs[field])
				return null;

			var iinfo = form.Inputs[field];
				
			if (dc.pui.Controls[iinfo.Type] && dc.pui.Controls[iinfo.Type].Get)
				return dc.pui.Controls[iinfo.Type].Get.call(this, form, iinfo);
			
			return null;
		},
		
		getFormValues: function(formname) {
			var entry = dc.pui.Loader.currentPageEntry();
			var form = entry.Forms[formname];
			
			if (!form || !form.InternalValues)
				return null;
			
			return form.InternalValues;
		}		
	},
	Controls: {	
		PasswordInput:{
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value)
					value = '';
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value);
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val();
			}
		},
		TextInput: {
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value)
					value = '';
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value);
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val();
			}
		},
		HiddenInput:{
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value)
					value = '';
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value);
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val();
			}
		},
		RadioSelect: { 
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value) {
					value = 'NULL';
					form.InternalValues[field.Field] = null;
				}
				else
					form.InternalValues[field.Field] = value;
				
				$('#' + field.Id + '-' + value).prop('checked',true);
				$('#frm' + form.Name + ' input[name=' + field.Field + ']').checkboxradio("refresh");
			},
			Get: function(form, field) {
				var val = $('#frm' + form.Name + ' input[name=' + field.Field + ']:checked').val();
				
				if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				if (value == 'NULL')
					value = null;
					
				if (value == null) 
					return !form.ValidationRules[field.Field].required;
					
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#frm' + form.Name + ' input[name=' + field.Field + ']:checked').val();
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				var val = $('#frm' + form.Name + ' input[name=' + field.Field + ']:checked').val();
				
				if (val == 'NULL')
					form.InternalValues[field.Field] = null;
				else
					form.InternalValues[field.Field] = val;
			}
		},
		YesNo: {
			Set: function(form, field, value) {
				value = dc.util.Boolean.toBoolean(value);

				if (!value)
					value = false;
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value + '').flipswitch("refresh");
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				return (val == 'true');				
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value == true, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != (val == 'true'));
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val() == 'true';
			}
		},
		Range: { 
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value)
					value = '';
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value);
				$('#' + field.Id).slider('refresh');
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val();
			}
		},
		Select: { 
			Set: function(form, field, value) {
				value = dc.util.String.toString(value);

				if (!value) {
					value = 'NULL';
					form.InternalValues[field.Field] = null;
				}
				else
					form.InternalValues[field.Field] = value;
				
				$('#' + field.Id).val(value).selectmenu("refresh");
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
					val = null;
					
				return val;
			},
			Validate: function(form, field, value) {
				if (value == 'NULL')
					value = null;
					
				if (value == null) 
					return !form.ValidationRules[field.Field].required;
					
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (val == 'NULL')
					form.InternalValues[field.Field] = null;
				else
					form.InternalValues[field.Field] = val;
			},
			Add: function(form, field, values) {
				for (var i = 0; i < values.length; i++) {
					var opt = values[i];			
					var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
					
					if (form.InternalValues[field.Field] == val)
						$('#' + field.Id).append($('<option value="' + val + '" selected="selected">' + opt.Label + '</option>'));
					else
						$('#' + field.Id).append($('<option value="' + val + '">' + opt.Label + '</option>'));
				}
				
				$('#' + field.Id).selectmenu("refresh");
			}
		},
		TextArea: { 
			Set: function(form, field, value) {
				if (dc.util.Struct.isComposite(value))
					value = JSON.stringify(value, undefined, 3);
				else
					value = dc.util.String.toString(value);

				if (!value)
					value = '';
				
				form.InternalValues[field.Field] = value;
				$('#' + field.Id).val(value);
			},
			Get: function(form, field) {
				var val = $('#' + field.Id).val();
				
				if (dc.util.Struct.isEmpty(val))
					val = null;
				else if (field.DataType == 'Json')
					try { 
						val = JSON.parse(val);
					}
					catch (x) {
						val = null;
					}
					
				return val;
			},
			Validate: function(form, field, value) {
				return (dc.schema.Manager.validate(value, field.DataType).Code == 0);
			},
			IsChanged: function(form, field) {
				var val = $('#' + field.Id).val();				
				return (form.InternalValues[field.Field] != val);				
			},
			ClearChanges: function(form, field) {
				form.InternalValues[field.Field] = $('#' + field.Id).val();
			}
		}
	},
	Popup: {
		init: function() {
			$('#puInfo').enhanceWithin().popup();
			$('#puConfirm').enhanceWithin().popup();
			
			$('#btnConfirmPopup').click(function(e) {
				if (dc.pui.Popup.__cb)
					dc.pui.Popup.__cb();
					
				e.preventDefault();
				return false;
			});
		},
		alert: function(msg) {
			$.mobile.loading('hide');
			
			console.log('alert called: ' + msg);

			$('#puInfoHtml').html(msg);
			$('#puInfo').popup('open', { positionTo: 'window', transition: 'pop' });
		},
		confirm: function(msg,callback) {
			$.mobile.loading('hide');
			
			console.log('confirm called: ' + msg);

			dc.pui.Popup.__cb = callback;
			$('#puConfirmHtml').html(msg);
			$('#puConfirm').popup('open', { positionTo: 'window', transition: 'pop' });
		},
		loading: function() {
	 		$.mobile.loading('show', { theme: 'b' });
		}
	}
};

$(document).on('mobileinit', function () { 
	$.mobile.ajaxEnabled = false; 
});

$(document).ready(function() { 
	dc.pui.Popup.init();
	dc.pui.Loader.init();
	
	dc.comm.setDoneHandler(function() {
		window.location = '/';
	});
	
	dc.comm.init(function() {
		/*
			TODO be sure the alternative is working
			
			dc.schema.Manager.load(dcSchema);
			dc.schema.Manager.load(dcCustomSchema);
		*/
		
		var info = dc.user.getUserInfo();

		if (info.Credentials) {
			dc.user.signin(info.Credentials.UserName, info.Credentials.Password, info.RememberMe, function(msg) { 
				if (dc.user.isVerified())
					dc.pui.Loader.loadMainPage();
				else 
					dc.pui.Loader.loadPage('/dct/pages/SignIn');		// TODO configure
			});
		}
		else 
			dc.pui.Loader.loadPage('/dct/pages/SignIn');		// TODO configure
	});
});
