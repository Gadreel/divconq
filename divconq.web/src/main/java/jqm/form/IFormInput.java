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
package jqm.form;

import divconq.struct.RecordStruct;

public interface IFormInput {
	ValidationInfo getValidation();
	String getInputId();
	String getInputName();
	String getRecord();
	String getField();
	RecordStruct getProps();
}
