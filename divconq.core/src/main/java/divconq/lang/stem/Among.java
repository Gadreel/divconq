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
package divconq.lang.stem;

import java.lang.reflect.Method;

public class Among {
	public Among(String s, int substring_i, int result, String methodname, Snowball methodobject) {
		this.s_size = s.length();
		this.s = s.toCharArray();
		this.substring_i = substring_i;
		this.result = result;
		this.methodobject = methodobject;
		
		if (methodname.length() == 0) {
			this.method = null;
		} 
		else {
			try {
				this.method = methodobject.getClass().getDeclaredMethod(methodname, new Class[0]);
			} 
			catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final int s_size; /* search string */
	public final char[] s; /* search string */
	public final int substring_i; /* index to longest matching substring */
	public final int result; /* result of the lookup */
	public final Method method; /* method to use if substring matches */
	public final Snowball methodobject; /* object to invoke method on */
};
