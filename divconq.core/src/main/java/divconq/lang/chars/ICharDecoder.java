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
package divconq.lang.chars;

public interface ICharDecoder {
    int getCharacter();
    boolean needsMore();
    int getCharacterAndReset();
    void reset();
    CharSequence processBytes(byte[] values);
    boolean readByteNeedMore(byte ch, boolean safe) throws Exception;
}
