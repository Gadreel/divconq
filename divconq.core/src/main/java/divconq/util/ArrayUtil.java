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
package divconq.util;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

public class ArrayUtil {
	/**
	 * verify an array copy before copying
	 * 
	 * @param source
	 * @param srcOffset
	 * @param dest
	 * @param destOffset
	 * @param count
	 * @return true if array can be copied
	 */
	public static boolean blockCopy(byte[] source, int srcOffset, byte[] dest, int destOffset, int count) {
        if ((source == null) || (dest == null))
        	return false;
        
        if ((srcOffset < 0) || (destOffset < 0) || (count <= 0)) 
        	return false;
        
        if ((source.length - srcOffset) < count) 
        	return false;
        
        if ((dest.length - destOffset) < count) 
        	return false;

        System.arraycopy(source, srcOffset, dest, destOffset, count);
        
        return true;
	}

	/**
	 * verify and assist a byte buffer to byte array copy 
	 * 
	 * @param source
	 * @param srcOffset
	 * @param dest
	 * @param destOffset
	 * @param count
	 * @return true if copy allowed
	 */
	public static boolean blockCopy(ByteBuffer source, int srcOffset, byte[] dest, int destOffset, int count) {
        if ((source == null) || (dest == null))
        	return false;
        
        if ((srcOffset < 0) || (destOffset < 0) || (count <= 0)) 
        	return false;
        
        if ((source.limit() - srcOffset) < count) 
        	return false;
        
        if ((dest.length - destOffset) < count) 
        	return false;

        source.position(srcOffset);
        source.get(dest,destOffset, count);

        return true;
	}

	/**
	 * verify and assist a byte array to byte buffer copy 
	 * 
	 * @param source
	 * @param srcOffset
	 * @param dest
	 * @param destOffset
	 * @param count
	 * @return true if copy allowed
	 */
	public static boolean blockCopy(byte[] source, int srcOffset, ByteBuffer dest, int destOffset, int count) {
        if ((source == null) || (dest == null))
        	return false;
        
        if ((srcOffset < 0) || (destOffset < 0) || (count <= 0)) 
        	return false;
        
        if ((source.length - srcOffset) < count) 
        	return false;
        
        if ((dest.limit() - destOffset) < count) 
        	return false;

        dest.position(destOffset);
        dest.put(source, srcOffset, count);
        
        return true;
	}
	
	/**
	 * verify and assist a byte buffer to byte buffer copy 
	 * 
	 * @param source
	 * @param srcOffset
	 * @param dest
	 * @param destOffset
	 * @param count
	 * @return true if copy allowed
	 */
	public static boolean blockCopy(ByteBuffer source, int srcOffset, ByteBuffer dest, int destOffset, int count) {
        if ((source == null) || (dest == null))
        	return false;
        
        if ((srcOffset < 0) || (destOffset < 0) || (count <= 0)) 
        	return false;
        
        if ((source.limit() - srcOffset) < count) 
        	return false;
        
        if ((dest.limit() - destOffset) < count) 
        	return false;
        
        int sl = source.limit();
        source.limit(srcOffset + count);
        dest.position(destOffset);
        dest.put(source);
        source.limit(sl);
        
        return true;
	}
	
    @SuppressWarnings("unchecked") // OK, because array is of type T
    public static <T> T[] addAll(final T[] array1, final T... array2) {
        if ((array1 == null) && (array2 == null)) 
        	return null;
        
        if (array1 == null) 
            return array2.clone();
        
        if (array2 == null) 
            return array1.clone();
        
        final Class<?> type1 = array1.getClass().getComponentType();
        
        final T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
        
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        
        try {
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        } catch (final ArrayStoreException ase) {
            // Check if problem was due to incompatible types
            /*
             * We do this here, rather than before the copy because:
             * - it would be a wasted check most of the time
             * - safer, in case check turns out to be too strict
             */
            final Class<?> type2 = array2.getClass().getComponentType();
            if (!type1.isAssignableFrom(type2)){
                throw new IllegalArgumentException("Cannot store "+type2.getName()+" in an array of "
                        +type1.getName(), ase);
            }
            throw ase; // No, so rethrow original
        }
        return joinedArray;
    }
	
    public static void reverse(final Object[] array) {
        if (array == null) {
            return;
        }
        reverse(array, 0, array.length);
    }
	
    public static void reverse(final Object[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (array == null) 
            return;

        int i = startIndexInclusive < 0 ? 0 : startIndexInclusive;
        int j = Math.min(array.length, endIndexExclusive) - 1;
        
        Object tmp;
        
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }
 }
