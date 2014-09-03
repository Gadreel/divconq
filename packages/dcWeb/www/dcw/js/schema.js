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


dc.schema.Manager.load([ 
      { "CoreType" : { "RootType" : 1 },
        "Id" : "Date",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ {  } ],
            "RootType" : 2
          },
        "Id" : "Decimal",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^\\d{4}(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])T(0[0-9]|1[0-9]|2[0-4])([0-5][0-9])([0-5][0-9])(\\d{3})?Z$" } ],
            "RootType" : 1
          },
        "Id" : "DateTime",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$" } ],
            "RootType" : 1
          },
        "Id" : "DateYMD",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^(0[1-9]|1[012])\\/(0[1-9]|[12][0-9]|3[01])$" } ],
            "RootType" : 1
          },
        "Id" : "DateMD",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^((([01]?[0-9]|2[0-3])(:[0-5][0-9])?|(24(:00)?))\\s?-\\s?(([01]?[0-9]|2[0-3])(:[0-5][0-9])?|(24(:00)?)))$" } ],
            "RootType" : 1
          },
        "Id" : "TimeRange",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Max" : 250 } ],
            "RootType" : 1
          },
        "Id" : "dcSmallString",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 1 },
        "Id" : "Xml",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 6 },
        "Id" : "Null",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 1 },
        "Id" : "BigString",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 1 },
        "Id" : "Time",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^t\\d{11,21}$" } ],
            "RootType" : 1
          },
        "Id" : "BigDateTime",
        "Kind" : 1
      },
      
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^(0[1-9]|1[012])\\/(0[1-9]|[12][0-9]|3[01])\\/\\d{4}$" } ],
            "RootType" : 1
          },
        "Id" : "DateMDY",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Max" : 64 } ],
            "RootType" : 1
          },
        "Id" : "dcTinyString",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 4 },
        "Id" : "Binary",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ {  } ],
            "RootType" : 2
          },
        "Id" : "Number",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "AllowDecimal" : false } ],
            "RootType" : 2
          },
        "Id" : "BigInteger",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Max" : 1000000 } ],
            "RootType" : 1
          },
        "Id" : "String",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^\\d{5}_\\d{15}$" } ],
            "RootType" : 1
          },
        "Id" : "Id",
        "Kind" : 1
      },
      { "CoreType" : { "RootType" : 7 },
        "Id" : "Any",
        "Kind" : 1
      },
      { "Fields" : [ { "Name" : "Domain",
              "Options" : [ { "CoreType" : { "DataRestrictions" : [ { "Pattern" : "^\\d{5}_\\d{15}$" } ],
                        "RootType" : 1
                      },
                    "Id" : "Id",
                    "Kind" : 1
                  } ],
              "Required" : 1
            } ],
        "Id" : "dcmLoadSiteRequest",
        "Kind" : 3
      },
      { "AnyRec" : true,
        "Fields" : [  ],
        "Id" : "AnyRecord",
        "Kind" : 3
      },
      { "CoreType" : { "RootType" : 1 },
        "Id" : "Json",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ {  } ],
            "RootType" : 2
          },
        "Id" : "BigDecimal",
        "Kind" : 1
      },
      { "Id" : "List",
        "Items" : { "Options" : [ { "CoreType" : { "RootType" : 7 },
                  "Id" : "Any",
                  "Kind" : 1
                } ] },
        "Kind" : 2
      },
      { "CoreType" : { "RootType" : 3 },
        "Id" : "Boolean",
        "Kind" : 1
      },
      { "CoreType" : { "DataRestrictions" : [ { "AllowDecimal" : false } ],
            "RootType" : 2
          },
        "Id" : "Integer",
        "Kind" : 1
      }
    ]);
	
	
dc.lang.Dict.load( [ 
	{ Token: "_code_400", Value: "Expected a BigDecimal, got: {$1}" },
	{ Token: "_code_401", Value: "Expected a Decimal, got: {$1}" },
	{ Token: "_code_402", Value: "Expected a BigInteger, got: {$1}" },
	{ Token: "_code_403", Value: "Expected a Integer, got: {$1}" },
	{ Token: "_code_404", Value: "Value too long: {$1}" },
	{ Token: "_code_405", Value: "Value too short: {$1}" },
	{ Token: "_code_406", Value: "Value does not match accepted list: {$1}" },
	{ Token: "_code_407", Value: "Core type missing.  Could not validate: {$1}" },
	{ Token: "_code_408", Value: "Unable to resolve as String: {$1}" },
	{ Token: "_code_409", Value: "Unable to resolve as Number: {$1}" },
	{ Token: "_code_410", Value: "Unable to resolve as Binary: {$1}" },
	{ Token: "_code_411", Value: "Unable to resolve as Boolean: {$1}" },
	{ Token: "_code_412", Value: "No core type options match.  Could not validate: {$1}" },
	{ Token: "_code_413", Value: "Missing required data type: {$1}" },
	{ Token: "_code_414", Value: "Tried to validate a record, but got an unknown object: {$1}" },
	{ Token: "_code_415", Value: "Tried to validate a list, but got an unknown object: {$1}" },
	{ Token: "_code_416", Value: "Missing data type to check for: {$1}" },
	{ Token: "_code_417", Value: "List contains too few items: {$1}" },
	{ Token: "_code_418", Value: "List contains too many items: {$1}" },
	{ Token: "_code_419", Value: "Field '{$1}' not allowed: {$2}" },
	{ Token: "_code_420", Value: "Missing data type to check for: {$1}" },
	{ Token: "_code_421", Value: "Tried to wrap a non-record, unknown resolution: {$1}" },
	{ Token: "_code_422", Value: "Tried to wrapChild a record, unknown resolution: {$1}" },
	{ Token: "_code_423", Value: "Data type missing.  Could not validate: {$1}" },
	{ Token: "_code_424", Value: "Value is required, but could not resolve: {$1} for field: {$2}" },
	{ Token: "_code_425", Value: "Missing task context" },
	{ Token: "_code_426", Value: "Unknown procedure" },
	{ Token: "_code_427", Value: "Not authorized to call service" },
	{ Token: "_code_428", Value: "Request should be empty" },
	{ Token: "_code_429", Value: "Unknown procedure" },
	{ Token: "_code_430", Value: "Procedure response should be empty" },
	{ Token: "_code_431", Value: "Missing task context" },
	{ Token: "_code_432", Value: "Unknown operation" },
	{ Token: "_code_433", Value: "Unknown request type" },
	{ Token: "_code_434", Value: "Not authorized to call service" },
	{ Token: "_code_435", Value: "Unknown response type" },
	{ Token: "_code_436", Value: "Unknown data type" },
	{ Token: "_code_437", Value: "Data type missing.  Could not validate: {$1}" },
	{ Token: "_code_438", Value: "No data type options match.  Could not validate: {$1}" },
	{ Token: "_code_439", Value: "Tried to wrap a non-list, unknown resolution: {$1}" },
	{ Token: "_code_440", Value: "No data type options match.  Could not validate: {$1}" }
	] );
	
