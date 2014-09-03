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
 * FormatException.java May 2007
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package divconq.www.util;

/**
 * The <code>FormatException</code> is used to create exceptions that
 * can use a template string for the message. Each format exception
 * will accept a string and an ordered list of variables which can 
 * be used to complete the exception message.
 * 
 * @author Niall Gallagher
 */
public class FormatException extends Exception {

   /**
	 * 
	 */
	private static final long serialVersionUID = -8414835740524813963L;

/**
    * Constructor for the <code>FormatException</code> this requires
    * a template message and an ordered list of values that are to
    * be inserted in to the provided template to form the error.
    * 
    * @param template this is the template string to be modified
    * @param list this is the list of values that are to be inserted
    */
   public FormatException(String template, Object... list) {
      super(String.format(template, list));
   }
   
   /**
    * Constructor for the <code>FormatException</code> this requires
    * a template message and an ordered list of values that are to
    * be inserted in to the provided template to form the error.
    * 
    * @param cause this is the original cause of the exception
    * @param template this is the template string to be modified
    * @param list this is the list of values that are to be inserted
    */
   public FormatException(Throwable cause, String template, Object... list) {
      super(String.format(template, list), cause);
   }
}
