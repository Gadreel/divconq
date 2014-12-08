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
	ICompositeBuilder record(Object... props) throws BuilderStateException;
	ICompositeBuilder startRecord() throws BuilderStateException;
	ICompositeBuilder endRecord() throws BuilderStateException;
	ICompositeBuilder list(Object... props) throws BuilderStateException;
	ICompositeBuilder startList() throws BuilderStateException;
	ICompositeBuilder endList() throws BuilderStateException;
	ICompositeBuilder field() throws BuilderStateException;
	ICompositeBuilder field(String name) throws BuilderStateException;
	ICompositeBuilder field(String name, Object value) throws BuilderStateException;
	ICompositeBuilder rawJson(Object value) throws BuilderStateException;
	ICompositeBuilder value(Object value) throws BuilderStateException;
	
	boolean needFieldName();
	BuilderState getState();
	Memory toMemory() throws BuilderStateException;
	CompositeStruct toLocal();
}
