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

dc.schema = {
	Manager: {
		_knownTypes: { },
		
		/* TODO preload known types with basics so they are always present
			<StringType Id="String">
			<StringType Id="BigString" />
			<NumberType Id="Integer" Class="divconq.struct.scalar.IntegerStruct">
			<NumberType Id="BigInteger" Class="divconq.struct.scalar.BigIntegerStruct">
			<NumberType Id="Decimal" Class="divconq.struct.scalar.DecimalStruct">
			<NumberType Id="BigDecimal" Class="divconq.struct.scalar.DecimalStruct">
			<NumberType Id="Number" Class="divconq.struct.scalar.DecimalStruct">
			<BooleanType Id="Boolean" />
			<BinaryType Id="Binary" />
			<StringType Id="Time">
			<StringType Id="TimeRange">
			<StringType Id="Date">
			<StringType Id="DateTime" Class="divconq.struct.scalar.DateTimeStruct">
			<StringType Id="BigDateTime" Class="divconq.struct.scalar.BigDateTimeStruct">
			<StringType Id="Id">
			<StringType Id="Json" />
			<StringType Id="Xml" />
			<NullType Id="Null" />
			<AnyType Id="Any" />
			<StringType Id="ResultLevel">
		*/
		
		load: function(defs) {
			if (!defs)
				return;
				
			for (var i in defs) {
				var dt = new dc.schema.DataType(defs[i]);
				dc.schema.Manager._knownTypes[dt.Id] = dt;
			} 
		},
					
		resolveType: function(type) {
			if (type.indexOf(':') != -1) {
				var tparts = type.split(':');
				var pt = dc.schema.Manager._knownTypes[tparts[0]];
				
				if (!pt || (pt.Kind != dc.schema.DataKind.Record))
					return null;
					
				var fld = pt.getField(tparts[1]);
				
				if (!fld)
					return null;

				return fld.getPrimaryType();
			}
			
			return dc.schema.Manager._knownTypes[type];
		},
		validate: function(data, type) {
			var or = new dc.lang.OperationResult();
			
			// do not error here, it is not a fault of this code that
			// no definitions are loaded - track that elsewhere
			if (dc.util.Struct.isEmpty(dc.schema.Manager._knownTypes))
				return or;
			
			var dt = dc.schema.Manager.resolveType(type); 
			
			if (!dt)
				or.errorTr(436);		
			else
				dt.validate(data, or);
			
			return or;
		},
		
		// for display
		format: function(data, type) {
		},
		// for data entry field
		// also good way to do compares - make sure both are in 'edit' format and then compare
		// internal format - JSON - is not always easy to compare? but edit should be 
		edit: function(data, type) {
		},
		// from data entry format to internal
		internal: function(data, type) {
		}
	},
	DataKind: {
		Scalar: 1,
		List: 2,
		Record: 3
	},
	DataType: function(settings) {
		this.Id = null;
		this.Kind = null;
		
		// for record
		this.Fields = null; 
		this.AnyRec = false;
		
		// for list
		this.Items = null;
		this.MinItems = 0;
		this.MaxItems = 0;
		
		// for scalar
		this.CoreType = null;
		
		if (settings) {
			for (var skey in settings) 
				if (settings.hasOwnProperty(skey)) 
					this[skey] = settings[skey];
				
			if (settings.Fields) {
				this.Fields = { };
				
				for (var i in settings.Fields) {
					var fld = new dc.schema.FieldOption(settings.Fields[i]);
					this.Fields[fld.Name] = fld;
				}
			}
				
			if (settings.Items) 
				this.Items = new dc.schema.ListOption(settings.Items);
			
			if (settings.CoreType) 
				this.CoreType = new dc.schema.CoreType(settings.CoreType);
		}
		
		this.getField = function(name) {
			if (!this.Fields)
				return null;
			
			return this.Fields[name];
		};
	
		// don't call this with data == null from a field if field required - required means "not null" so put the error in
		this._match = function(data, mr) {
			if (this.Kind == dc.schema.DataKind.Record) {
				if (!data || dc.util.Struct.isScalar(data))
					return false;
					 
				if (data instanceof Object) {
					if (this.Fields != null) {							
						// match only if all required fields are present 
						for (var fname in this.Fields) {
							if (this.Fields.hasOwnProperty(fname)) {
								var fld = this.Fields[fname];
								
								if ((fld.Required == dc.schema.ReqTypes.True) && dc.util.String.isEmpty(data[fname]))
									return false;
								
								if ((fld.Required == dc.schema.ReqTypes.IfPresent) && data.hasOwnProperty(fname) && dc.util.String.isEmpty(data[fname]))
									return false;
							}
						}
						
						return true;
					}
					
					// this is an exception to the rule, there is no "non-null" state to return from this method
					return this.AnyRec;
				}
				
				return false;
			}
			
			if (this.Kind == dc.schema.DataKind.List) 
				return (data instanceof Array);
	
			if (this.CoreType == null) 
				return false;
			
			return this.CoreType._match(data, mr);
		};
		
		// don't call this with data == null from a field if field required - required means "not null" so put the error in
		// returns true only if there was a non-null value present that conforms to the expected structure (record, list or scalar) 
		// null values that do not conform should not cause an false
		this.validate = function(data, mr) {
			if (typeof data == 'undefined')
				return false;
			
			if (this.Kind == dc.schema.DataKind.Record) 
				return this._validateRecord(data, mr);
			
			if (this.Kind == dc.schema.DataKind.List) {
				if (data instanceof Array)
					return this._validateList(data, mr);
	
				mr.errorTr(415, [ data ]);		
				return false;
			}
	
			return this._validateScalar(data, mr);
		};
	
		this._validateRecord = function(data, mr) {
			if (!data || dc.util.Struct.isScalar(data)) {
				mr.errorTr(414, [ data ]);
				return false;
			}
				
			if (! (data instanceof Object)) {
				mr.errorTr(414, [ data ]);
				return false;
			}
			
			if (this.Fields != null) {
				// handles all but the case where data holds a field not allowed 
				for (var fname in this.Fields) {
					if (this.Fields.hasOwnProperty(fname)) 
						this.Fields[fname].validate(data.hasOwnProperty(fname), data[fname], mr);
				}
				
				if (!this.AnyRec) {
					for (var fname in data) {
						if (! this.Fields.hasOwnProperty(fname))
							mr.errorTr(419, [ fname, data ]);	
					}
				}
			}
			
			// this is an exception to the rule, there is no "non-null" state to return from this method
			return true;
		};
	
		this._validateList = function(data, mr) {
			if (!(data instanceof Array)) {
				mr.errorTr(415, [ data ]);   
				return false;
			}
			
			if (this.Items == null) 
				mr.errorTr(416, [ data ]);   
			else
				for (var i = 0; i < data.length; i++)
					this.Items.validate(data[i], mr);		
			
			if ((this.MinItems > 0) && (data.length < this.MinItems))
				mr.errorTr(417, [data]);   
			
			if ((this.MaxItems > 0) && (data.length > this.MaxItems))
				mr.errorTr(418, [data]);   
			
			return true;		
		};
	
		this._validateScalar = function(data, mr) {
			if (this.CoreType == null) {
				mr.errorTr(420, [data]);   
				return false;
			}
			
			return this.CoreType.validate(data, mr);
		};			
	
		this.getPrimaryItemType = function() {
			if (this.Items != null) 
				return this.Items.PrimaryType;
			
			return null;
		}; 
	},
	ReqTypes: {
		True: 1,
		False: 2,
		IfPresent: 3
	},
	FieldOption: function(settings) {
		this.Options = [ ];
		this.Name = null;
		this.Required = dc.schema.ReqTypes.False;
		
		if (settings) {
			for (var skey in settings) 
				if (settings.hasOwnProperty(skey)) 
					this[skey] = settings[skey];
		
			this.Options = [ ];
			
			for (var item in settings.Options) 
				this.Options.push(new dc.schema.DataType(settings.Options[item]));
		}
		
		// don't call this with data == null from a field if field required - required means "not null" so put the error in
		this.validate = function(present, data, mr) {
			if (this.Options.length == 0) {
				mr.errorTr(423, [data]);			
				return;
			}
			
			if (this.Options.length == 1) {
				if (!this.Options[0].validate(data, mr))
					this.valueUnresolved(present, data, mr);
				
				return;
			}
			
			for (var dt in this.Options) {
				var opt = this.Options[dt];
				
				if (opt._match(data, mr)) 
					if (!opt.validate(data, mr))
						this.valueUnresolved(present, data, mr);
					
				return;
			}
			
			mr.errorTr(440, [data]);			
			return;
		};
		
		this.valueUnresolved = function(present, data, mr) {
			if ((typeof data != 'undefined') && (data != null)) {
				mr.errorTr(440, [data]);			
				return;
			}
			
			if (this.Required == dc.schema.ReqTypes.False)
				return;
			
			if (this.Required == dc.schema.ReqTypes.IfPresent && !present)
				return;
			
			mr.errorTr(424, [data, this.name]);
		};
		
		this.getPrimaryType = function() {
			if (this.Options.length == 0) 
				return null;
			
			return this.Options[0];
		};
	},
	ListOption: function(settings) {
		this.Options = [ ];
		
		if (settings) {
			for (var item in settings.Options) 
				this.Options.push(new dc.schema.DataType(settings.Options[item]));
		}
		
		// don't call this with data == null from a field if field required - required means "not null" so put the error in
		this.validate = function(data, mr) {
			if (this.Options.length == 0) {
				mr.errorTr(437, [data]);			
				return false;
			}
			
			if (this.Options.length == 1) 
				return this.Options[0].validate(data, mr);
			
			for (var dt in this.Options) {
				var opt = this.Options[dt];
				
				if (opt._match(data, mr)) 
					return opt.validate(data, mr);
			}
			
			mr.errorTr(438, [data]);			
			return false;
		};
		
		this.getPrimaryType = function() {
			if (this.Options.length == 0) 
				return null;
			
			return this.Options[0];
		};
	},
	RootType: {
		String: 1,
		Number: 2,
		Boolean: 3,
		Binary: 4,
		Component: 5,		// for scripting
		Null: 6,
		Any: 7
	},
	CoreType: function(settings) {
		this.RootType = dc.schema.RootType.String;
		this.DataRestrictions = [ ];
		
		if (settings) {
			if (settings.RootType)
				this.RootType = settings.RootType;
				
			if (settings.DataRestrictions) {
				for (var drest in settings.DataRestrictions) {
					if (this.RootType == dc.schema.RootType.Number) 
						this.DataRestrictions.push(new dc.schema.NumberRestriction(settings.DataRestrictions[drest]));
					else
						this.DataRestrictions.push(new dc.schema.StringRestriction(settings.DataRestrictions[drest]));
				}
			}
		}
	
		this._match = function(data, mr) {
			if (!this.RootType) 
				return false;
			
			if (this.RootType == dc.schema.RootType.String) 
				return dc.util.String.isString(data);
			
			if (this.RootType == dc.schema.RootType.Number) 
				return dc.util.Number.isNumber(data);
			
			if (this.RootType == dc.schema.RootType.Binary) 
				return dc.util.Binary.isBinary(data);
			
			if (this.RootType == dc.schema.RootType.Boolean) 
				return dc.util.Boolean.isBoolean(data);
			
			if (this.RootType == dc.schema.RootType.Null) 
				return (data == null);
			
			if (this.RootType == dc.schema.RootType.Any) 
				return true;
			
			return false;
		};
		
		// don't call this with data == null from a field if field required - required means "not null" so put the error in
		this.validate = function(data, mr) {
			if (!this.RootType) {
				mr.errorTr(407, [data]);			
				return false;
			}
			
			if (this.RootType == dc.schema.RootType.String) {
				var sdata = dc.util.String.toString(data);
					
				if (typeof sdata != 'string') {
					mr.errorTr(408, [data]);
					return false;
				}
				
				if (this.DataRestrictions.length == 0)
					return true;
				
				for (var i = 0; i < this.DataRestrictions.length; i++) {
					if (this.DataRestrictions[i].pass(sdata, data)) 
						return true;
				}
				
				for (var i = 0; i < this.DataRestrictions.length; i++) 
					this.DataRestrictions[i].fail(sdata, data, mr);
				
				return false;
			}
			
			if (this.RootType == dc.schema.RootType.Number) {
				var ndata = dc.util.Number.toNumber(data);
					
				if (typeof ndata != 'number') {
					mr.errorTr(409, [data]);
					return false;
				}
				
				if (this.DataRestrictions.length == 0)
					return true;
				
				for (var i = 0; i < this.DataRestrictions.length; i++) {
					if (this.DataRestrictions[i].pass(ndata, data)) 
						return true;
				}
				
				for (var i = 0; i < this.DataRestrictions.length; i++) 
					this.DataRestrictions[i].fail(ndata, data, mr);
				
				return false;
			}
			
			// TODO check limits, etc
			if (this.RootType == dc.schema.RootType.Binary) {
				if (!dc.util.Binary.isBinary(data)) {
					mr.errorTr(410, [data]);
					return false;
				}
				
				return true;
			}
			
			if (this.RootType == dc.schema.RootType.Boolean) {
				if(!dc.util.Boolean.isBoolean(data)) {
					mr.errorTr(411, [data]);
					return false;
				}
				
				return true;
			}
			
			if (this.RootType == dc.schema.RootType.Null) 
				return (data == null);
			
			if (this.RootType == dc.schema.RootType.Any) 
				return true;
			
			mr.errorTr(412, [data]);			
			return false;
		}
	},
	StringRestriction : function(settings) {
		this.Enums = null;
		this.Max = null;
		this.Min = null;
		this.Pattern = null;
		this.PassFunc = null;
		this.FailFunc = null;

		if (settings) {
			for (var skey in settings) 
				if (settings.hasOwnProperty(skey)) 
					this[skey] = settings[skey];
					
			if (typeof settings.Pattern == 'string')
				this.Pattern = new RegExp(settings.Pattern);
		}

		this.pass = function(x, src) {
			if ((this.Max != null) && (x.length > this.Max))
				return false;
			
			if ((this.Min != null) && (x.length < this.Min))
				return false;
			
			if (this.Enums) {
				var fnd = false;
				
				for (var i = 0; i < this.Enums.length; i++) {
					if (this.Enums[i] == x) {
						fnd = true;
						break;
					}
				}
				
				if (!fnd)
					return false;
			}
			
			if (this.Pattern && !this.Pattern.test(x))
				return false;
			
			if (this.PassFunc && !this.PassFunc.call(this, x))
				return false;
			
			return true;
		};

		this.fail = function(x, src, mr) {
			if ((this.Max != null) && (x.length > this.Max))
				mr.errorTr(404, [x]);		
		
			if ((this.Min != null) && (x.length < this.Min))
				mr.errorTr(405, [x]);		
			
			if (this.Enums) {
				var fnd = false;
				
				for (var i = 0; i < this.Enums.length; i++) {
					if (this.Enums[i] == x) {
						fnd = true;
						break;
					}
				}
				
				if (!fnd)
					mr.errorTr(406, [x]);		
			}
		
			if (this.Pattern && !this.Pattern.test(x))
				mr.errorTr(447, [x]);		
			
			if (this.FailFunc)
				this.FailFunc.call(this, x, mr);
		};
	},
	NumberRestriction: function(settings) {
		this.AllowDecimal = true;
		this.Min = null;
		this.Max = null;
		this.PassFunc = null;
		this.FailFunc = null;

		if (settings) {
			for (var skey in settings) 
				if (settings.hasOwnProperty(skey)) 
					this[skey] = settings[skey];
		}

		this.pass = function(x, src) {
			if (!this.AllowDecimal && (x % 1 != 0))
				return false;
			
			if (!this.AllowDecimal && (typeof src == 'string') && (src.indexOf('.') > 0) && (src.indexOf(' ') > 0))
				return false;
			
			if ((this.Min != null) && (x < this.Min))
				return false;
			
			if ((this.Max != null) && (x > this.Max))
				return false;
			
			if (this.PassFunc && !this.PassFunc.call(this, x))
				return false;
			
			return true;
		};

		this.fail = function(x, src, mr) {
			if (!this.AllowDecimal && (x % 1 != 0))
				mr.errorTr(400, [x]);		
			
			if (!this.AllowDecimal && (typeof src == 'string') && (src.indexOf('.') > 0) && (src.indexOf(' ') > 0))
				mr.errorTr(400, [x]);		
			
			if ((this.Min != null) && (x < this.Min))
				mr.errorTr(405, [x]);		
			
			if ((this.Max != null) && (x > this.Max))
				mr.errorTr(404, [x]);		
			
			if (this.FailFunc)
				this.FailFunc.call(this, x, mr);
		};				
	}
};

