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
  * Struct is a very fundamental concept in DivConq and used throughout.
  * The Struct system is similar to JSON or YAML, where your principle
  * elements are {@link RecordStruct} which is like Object, {@link ListStruct}
  * which is like Array and {@link ScalarStruct} which is like a value.
  * </p>
  * 
  * <p>
  * Quick introduction to Struct use:
  * </p>
  * 
  * <pre><code>import static divconq.struct.StructUtil.*;
  * ...
  *
  * // create records, fields and lists easily
  * Struct rec = record(
  *    field("Name", "Fred"),
  *    field("Age", 49),
  *    field("FavoriteFoods", list("pizza", "cereal", "lima beans"))
  * );
  *
  * // output as JSON
  * rec.toString();
  *
  * // output as formatted JSON
  * rec.toPrettyString();
  *
  * // validate the structure follows the schema
  * rec.validate("dcTestPeopleExample");
  * </code></pre>
  * 
  * <p>
  * Struct has a schema system by which to validate {@link divconq.schema}.
  * The dcSchema system allows you to extend/customize data types.  Each
  * instance of Struct also has an optional datatype so while Struct is
  * mostly like JSON it can be used with YAML too.  
  * </p>
  * 
  * <p>
  * Struct is used throughout DivConq.  
  * </p>
  * 
  * <ul>
  * <li>Disconnected Result Sets from data base</li>
  * <li>DivConq RPC calls (Web Services)</li>
  * <li>On the DivConq Bus (server to server communication)</li>
  * <li>DivConq scripting system</li>
  * </ul>
  * 
  * <p>
  * Support for streaming parsing of structures is available {@link divconq.struct.builder}.
  * </p>
  * 
  */
package divconq.struct;

