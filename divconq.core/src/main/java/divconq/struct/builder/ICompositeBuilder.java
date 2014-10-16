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
package divconq.struct.builder;

import divconq.lang.Memory;
import divconq.struct.CompositeStruct;

public interface ICompositeBuilder {
	void record(Object... props) throws BuilderStateException;
	void startRecord() throws BuilderStateException;
	void endRecord() throws BuilderStateException;
	void list(Object... props) throws BuilderStateException;
	void startList() throws BuilderStateException;
	void endList() throws BuilderStateException;
	void field() throws BuilderStateException;
	void field(String name) throws BuilderStateException;
	void field(String name, Object value) throws BuilderStateException;
	boolean needFieldName();
	void rawJson(Object value) throws BuilderStateException;
	// scalar is just an opportunity to end items in list, change state, etc - it is not about outputting a marker
	//void scalar() throws BuilderStateException;
	
	// value can be called in 3 different circumstances:
	// 1) in a list, a new item is being added
	// 2) after a "field" call to take the field name
	// 3) after a "field" call and after one call to "value" (the field name) then this is the field value
	void value(Object value) throws BuilderStateException;
	BuilderState getState();
	Memory toMemory() throws BuilderStateException;
	CompositeStruct toLocal();
}
