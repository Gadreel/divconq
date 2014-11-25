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
package divconq.filestore;

import divconq.schema.IDataExposer;
import divconq.util.IOUtil;
import divconq.util.StringUtil;

// object is to immutable, keep it that way :)
public class CommonPath implements IDataExposer {
	static public CommonPath ROOT = new CommonPath("/");
	
    // internal representation
    protected String pathparts[] = null;
    
    protected String path = null;

    // paths must always start with /
    public CommonPath(String pathname) {
        if (StringUtil.isEmpty(pathname) || !pathname.startsWith("/"))
        	throw new IllegalArgumentException("Path not valid content: " + pathname);
    	
        this.path = CommonPath.normalizeAndCheck(pathname);
        
        if (StringUtil.isEmpty(this.path))
        	throw new IllegalArgumentException("Path not valid format: " + pathname);
        
        // TODO really we want the byte length, but this is something
        if (this.path.length() > 32000)
        	throw new IllegalArgumentException("Path to long: " + pathname);

        int count = 0;
        int index = 0;
        boolean cntname = true;

        // count names
        while (index < this.path.length()) {
            char c = this.path.charAt(index);
            
            if (c == '/') {
            	cntname = true;
            }
            else if (cntname) {
                count++;
                cntname = false;
            }
            
            index++;
        }

        // populate array
        String[] result = new String[count];
        
        int start = 0;
        index = 0;
        count = 0;
        cntname = true;
        
        while (index < this.path.length()) {
            char c = this.path.charAt(index);
            
            if (c == '/') {
            	if (index > start) 
            		result[count - 1] = this.path.substring(start, index);
            	
        		start = index + 1;
            	cntname = true;
            }
            else if (cntname) {
                count++;
                cntname = false;
            }
            
            index++;
        }
        
        // if some name is left
    	if (!cntname) 
    		result[count - 1] = this.path.substring(start, index);

    	// don't allow .. or .
    	for (String pn : result) {
    		if (!IOUtil.isLegalFilename(pn))
            	throw new IllegalArgumentException("Path name not valid: " + pathname + " - part: " + pn);
    	}
    	
        this.pathparts = result;
    }

    // removes redundant slashes and check input for invalid characters
    static public String normalizeAndCheck(String input) {
        int n = input.length();
        
        char prevChar = 0;
        
        for (int i=0; i < n; i++) {
            char c = input.charAt(i);
            
            if ((c == '/') && (prevChar == '/'))
                return CommonPath.normalize(input, n, i - 1);
            
            prevChar = c;
        }
        
        if (prevChar == '/')
            return CommonPath.normalize(input, n, n - 1);
        
        return input;
    }

    protected static String normalize(String input, int len, int off) {
        if (len == 0)
            return input;
        
        int n = len;
        
        while ((n > 0) && (input.charAt(n - 1) == '/')) 
        	n--;
        
        if (n == 0)
            return "/";
        
        StringBuilder sb = new StringBuilder(input.length());
        
        if (off > 0)
            sb.append(input.substring(0, off));
        
        char prevChar = 0;
        
        for (int i=off; i < n; i++) {
            char c = input.charAt(i);
            
            if ((c == '/') && (prevChar == '/'))
                continue;

            sb.append(c);
            prevChar = c;
        }
        
        return sb.toString();
    }

    public boolean isRoot() {
        return ((this.pathparts == null) || (this.pathparts.length == 0));
    }

    public String getFileName() {
    	return this.isRoot() ? null : this.pathparts[this.pathparts.length - 1];
    }

	public boolean hasFileExtension() {
		String fname = this.getFileName();
		
		if (fname == null)
			return false;
		
		int pos = fname.lastIndexOf('.');
		
		return (pos != -1);
	}

	public String getFileExtension() {
		String fname = this.getFileName();
		
		if (fname == null)
			return null;
		
		int pos = fname.lastIndexOf('.');
		
		if (pos == -1)
			return null;
		
		return fname.substring(pos + 1);
	}

	public String getFileNameMinusExtension() {
		String fname = this.getFileName();
		
		if (fname == null)
			return null;
		
		int pos = fname.lastIndexOf('.');
		
		if (pos == -1)
			return fname;
		
		return fname.substring(0, pos);
	}

    public CommonPath getParent() {
    	return this.isRoot() ? CommonPath.ROOT : this.subpath(0, this.pathparts.length - 1);
    }

    public int getNameCount() {
    	if (this.pathparts == null)
            return 0;
    	
        return this.pathparts.length;
    }

    public String getName(int index) {
    	if ((this.pathparts == null) || (this.pathparts.length == 0))
            return null;
    	
        if ((index < 0) || (index >= this.pathparts.length))
			return null;

        return this.pathparts[index];
    }

    /**
     * If begin is 0 then we keep "absolute" 
     * 
     * @param beginIndex starting path part
     * @param length number of path parts
     * @return the sub path
     */
    public CommonPath subpath(int beginIndex, int length) {
    	if ((this.pathparts == null) || (this.pathparts.length == 0)) 
    		return CommonPath.ROOT;
    	
        if ((beginIndex < 0) || (beginIndex >= this.pathparts.length))
        	return CommonPath.ROOT;
        
        if ((length < 1) || (beginIndex + length > this.pathparts.length))
        	return CommonPath.ROOT;

        return new CommonPath("/" + StringUtil.join(this.pathparts, "/", beginIndex, beginIndex + length));
    }

    public CommonPath subpath(int beginIndex) {
    	int length = this.pathparts.length - beginIndex;
    	
    	return this.subpath(beginIndex, length);
    }

	public CommonPath subpath(CommonPath other) {
		if (other.isRoot())
			return this;
		
		if (this.path.startsWith(other.path))
			return new CommonPath(this.path.substring(other.path.length()));
		
		return CommonPath.ROOT;
	}

    // provide other path starting with /
    public CommonPath resolve(String other) {
    	if (StringUtil.isEmpty(other))
    		return null;
    	
    	if (!other.startsWith("/"))
    		other = "/" + other;
    	
        return this.isRoot() ? new CommonPath(other) : new CommonPath(this.path + other);
    }
    
    public CommonPath resolve(CommonPath other) {
        return this.isRoot() ? other : new CommonPath(this.path + other.path);
    }
    
    public CommonPath resolvePeer(String other) {
    	if (StringUtil.isEmpty(other))
    		return null;
    	
    	if (!other.startsWith("/"))
    		other = "/" + other;
    	
        return this.isRoot() ? new CommonPath(other) : this.getParent().resolve(other);
    }
    
    public boolean isParent(CommonPath other) {
    	if (this.isRoot())
    		return true;
    	
    	if (this.pathparts.length >= other.pathparts.length)
    		return false;
    	
    	for (int i = 0; i < this.pathparts.length; i++) {
    		if (!this.pathparts[i].equals(other.pathparts[i]))
    			return false;
    	}
    	
		return true;
    }

    /*  TODO
    // Resolve child against given base
    private static byte[] resolve(byte[] base, byte[] child) {
        int baseLength = base.length;
        int childLength = child.length;
        if (childLength == 0)
            return base;
        if (baseLength == 0 || child[0] == '/')
            return child;
        byte[] result;
        if (baseLength == 1 && base[0] == '/') {
            result = new byte[childLength + 1];
            result[0] = '/';
            System.arraycopy(child, 0, result, 1, childLength);
        } else {
            result = new byte[baseLength + 1 + childLength];
            System.arraycopy(base, 0, result, 0, baseLength);
            result[base.length] = '/';
            System.arraycopy(child, 0, result, baseLength+1, childLength);
        }
        return result;
    }

    UnixPath resolve(byte[] other) {
        return resolve(new UnixPath(getFileSystem(), other));
    }

    @Override
    public UnixPath relativize(Path obj) {
        UnixPath other = toUnixPath(obj);
        if (other.equals(this))
            return emptyPath();

        // can only relativize paths of the same type
        if (this.isAbsolute() != other.isAbsolute())
            throw new IllegalArgumentException("'other' is different type of Path");

        // this path is the empty path
        if (this.isEmpty())
            return other;

        int bn = this.getNameCount();
        int cn = other.getNameCount();

        // skip matching names
        int n = (bn > cn) ? cn : bn;
        int i = 0;
        while (i < n) {
            if (!this.getName(i).equals(other.getName(i)))
                break;
            i++;
        }

        int dotdots = bn - i;
        if (i < cn) {
            // remaining name components in other
            UnixPath remainder = other.subpath(i, cn);
            if (dotdots == 0)
                return remainder;

            // other is the empty path
            boolean isOtherEmpty = other.isEmpty();

            // result is a  "../" for each remaining name in base
            // followed by the remaining names in other. If the remainder is
            // the empty path then we don't add the final trailing slash.
            int len = dotdots*3 + remainder.path.length;
            if (isOtherEmpty) {
                assert remainder.isEmpty();
                len--;
            }
            byte[] result = new byte[len];
            int pos = 0;
            while (dotdots > 0) {
                result[pos++] = (byte)'.';
                result[pos++] = (byte)'.';
                if (isOtherEmpty) {
                    if (dotdots > 1) result[pos++] = (byte)'/';
                } else {
                    result[pos++] = (byte)'/';
                }
                dotdots--;
            }
            System.arraycopy(remainder.path, 0, result, pos, remainder.path.length);
            return new UnixPath(getFileSystem(), result);
        } else {
            // no remaining names in other so result is simply a sequence of ".."
            byte[] result = new byte[dotdots*3 - 1];
            int pos = 0;
            while (dotdots > 0) {
                result[pos++] = (byte)'.';
                result[pos++] = (byte)'.';
                // no tailing slash at the end
                if (dotdots > 1)
                    result[pos++] = (byte)'/';
                dotdots--;
            }
            return new UnixPath(getFileSystem(), result);
        }
    }

    @Override
    public Path normalize() {
        final int count = getNameCount();
        if (count == 0)
            return this;

        boolean[] ignore = new boolean[count];      // true => ignore name
        int[] size = new int[count];                // length of name
        int remaining = count;                      // number of names remaining
        boolean hasDotDot = false;                  // has at least one ..
        boolean isAbsolute = isAbsolute();

        // first pass:
        //   1. compute length of names
        //   2. mark all occurences of "." to ignore
        //   3. and look for any occurences of ".."
        for (int i=0; i<count; i++) {
            int begin = offsets[i];
            int len;
            if (i == (offsets.length-1)) {
                len = path.length - begin;
            } else {
                len = offsets[i+1] - begin - 1;
            }
            size[i] = len;

            if (path[begin] == '.') {
                if (len == 1) {
                    ignore[i] = true;  // ignore  "."
                    remaining--;
                }
                else {
                    if (path[begin+1] == '.')   // ".." found
                        hasDotDot = true;
                }
            }
        }

        // multiple passes to eliminate all occurences of name/..
        if (hasDotDot) {
            int prevRemaining;
            do {
                prevRemaining = remaining;
                int prevName = -1;
                for (int i=0; i<count; i++) {
                    if (ignore[i])
                        continue;

                    // not a ".."
                    if (size[i] != 2) {
                        prevName = i;
                        continue;
                    }

                    int begin = offsets[i];
                    if (path[begin] != '.' || path[begin+1] != '.') {
                        prevName = i;
                        continue;
                    }

                    // ".." found
                    if (prevName >= 0) {
                        // name/<ignored>/.. found so mark name and ".." to be
                        // ignored
                        ignore[prevName] = true;
                        ignore[i] = true;
                        remaining = remaining - 2;
                        prevName = -1;
                    } else {
                        // Case: /<ignored>/.. so mark ".." as ignored
                        if (isAbsolute) {
                            boolean hasPrevious = false;
                            for (int j=0; j<i; j++) {
                                if (!ignore[j]) {
                                    hasPrevious = true;
                                    break;
                                }
                            }
                            if (!hasPrevious) {
                                // all proceeding names are ignored
                                ignore[i] = true;
                                remaining--;
                            }
                        }
                    }
                }
            } while (prevRemaining > remaining);
        }

        // no redundant names
        if (remaining == count)
            return this;

        // corner case - all names removed
        if (remaining == 0) {
            return isAbsolute ? getFileSystem().rootDirectory() : emptyPath();
        }

        // compute length of result
        int len = remaining - 1;
        if (isAbsolute)
            len++;

        for (int i=0; i<count; i++) {
            if (!ignore[i])
                len += size[i];
        }
        byte[] result = new byte[len];

        // copy names into result
        int pos = 0;
        if (isAbsolute)
            result[pos++] = '/';
        for (int i=0; i<count; i++) {
            if (!ignore[i]) {
                System.arraycopy(path, offsets[i], result, pos, size[i]);
                pos += size[i];
                if (--remaining > 0) {
                    result[pos++] = '/';
                }
            }
        }
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof UnixPath))
            return false;
        UnixPath that = (UnixPath)other;

        // other path is longer
        if (that.path.length > path.length)
            return false;

        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();

        // other path has no name elements
        if (thatOffsetCount == 0 && this.isAbsolute()) {
            return that.isEmpty() ? false : true;
        }

        // given path has more elements that this path
        if (thatOffsetCount > thisOffsetCount)
            return false;

        // same number of elements so must be exact match
        if ((thatOffsetCount == thisOffsetCount) &&
            (path.length != that.path.length)) {
            return false;
        }

        // check offsets of elements match
        for (int i=0; i<thatOffsetCount; i++) {
            Integer o1 = offsets[i];
            Integer o2 = that.offsets[i];
            if (!o1.equals(o2))
                return false;
        }

        // offsets match so need to compare bytes
        int i=0;
        while (i < that.path.length) {
            if (this.path[i] != that.path[i])
                return false;
            i++;
        }

        // final check that match is on name boundary
        if (i < path.length && this.path[i] != '/')
            return false;

        return true;
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof UnixPath))
            return false;
        UnixPath that = (UnixPath)other;

        int thisLen = path.length;
        int thatLen = that.path.length;

        // other path is longer
        if (thatLen > thisLen)
            return false;

        // other path is the empty path
        if (thisLen > 0 && thatLen == 0)
            return false;

        // other path is absolute so this path must be absolute
        if (that.isAbsolute() && !this.isAbsolute())
            return false;

        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();

        // given path has more elements that this path
        if (thatOffsetCount > thisOffsetCount) {
            return false;
        } else {
            // same number of elements
            if (thatOffsetCount == thisOffsetCount) {
                if (thisOffsetCount == 0)
                    return true;
                int expectedLen = thisLen;
                if (this.isAbsolute() && !that.isAbsolute())
                    expectedLen--;
                if (thatLen != expectedLen)
                    return false;
            } else {
                // this path has more elements so given path must be relative
                if (that.isAbsolute())
                    return false;
            }
        }

        // compare bytes
        int thisPos = offsets[thisOffsetCount - thatOffsetCount];
        int thatPos = that.offsets[0];
        if ((thatLen - thatPos) != (thisLen - thisPos))
            return false;
        while (thatPos < thatLen) {
            if (this.path[thisPos++] != that.path[thatPos++])
                return false;
        }

        return true;
    }
*/
    
    public int compareTo(CommonPath other) {
        return this.path.compareTo(other.path);
    }

    @Override
    public boolean equals(Object ob) {
        if ((ob != null) && (ob instanceof CommonPath)) {
            return (this.compareTo((CommonPath)ob) == 0);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public String toString() {
        return this.path; 
    }

	public String getFull() {
		return this.path;
	}

	@Override
	public Object exposeData() {
		return this.path;
	}
}
