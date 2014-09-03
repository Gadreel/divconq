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
package divconq.lang;

// TODO this is no good as is...
public class ComparableNumber {
	@SuppressWarnings("unchecked")
	static public ComparableNumber build(Number num) {
		if (num == null)
			return null;
		
		//if (! (num instanceof BigDecimal))
		//	bum = new bigde
		
		if (num instanceof Comparable<?>)
			return new ComparableNumber((Comparable<Number>)num);
			
		return null;
	}
	
	protected Comparable<Number> value = null;
	
	protected ComparableNumber(Comparable<Number> value) {
		this.value = value;
	}
	
	public boolean isEqual(Number num) {
		if (num instanceof Comparable<?>)
			return (this.value.compareTo(num) == 0);
		
		return false;
	}
	
	public boolean isLessThan(Number num) {
		if (num instanceof Comparable<?>)
			return (this.value.compareTo(num) == -1);
		
		return false;
	}
	
	public boolean isGreaterThan(Number num) {
		if (num instanceof Comparable<?>)
			return (this.value.compareTo(num) == 1);
		
		return false;
	}
}
