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

import divconq.util.StringUtil;

/**
 * Take English words and place them in reverse order, not to simulate RTL reading so much as to test that
 * dictionary tokens will work as desired when used in a non-ltr settings. 
 * 
 * @author Andy
 *
 */
public class RtlEnglish {
	/**
	 * Translate test in English to Rtl-English  
	 * 
	 * @param sText text to translate
	 * @return translated text
	 */
	public static String translate(String sText) {
		if (StringUtil.isEmpty(sText))
			return null;

		   String sRtlEn = "";    // translation result
		   String sLine = "";        // Line by line translation for multiple lines (text area)
		   String sWord = "";        // Current word being built & xlated (or separator)
		   boolean bWord = true;        // Word/Separator mode flag
		   char sChar = ' ';        // Current character from text being xlated

		   for (int iChar = 0; iChar < sText.length(); ++iChar) {

		      // The null at the end of the text signals final end of text/word

		      sChar = sText.charAt (iChar);  // Get the next character

		      if ((sChar >= 'A' && sChar <= 'Z') ||
		            (sChar >= 'a' && sChar <= 'z') ||
		            (sChar == '\'' && bWord && StringUtil.isNotEmpty(sWord))) {  // If alphabetic character

		         if (!bWord) {  // If last not a word, then must be non-word/separator
		            sLine = sWord + sLine;  // Append punctuation & whitespace to line
		            sWord = "";  // Clear word text
		            bWord = true;  // We're working on a real word
		         }

		         sWord += sChar;  // Append alpha character to word
		      } 
		      else {  // A non-alpha character
		         if (bWord && StringUtil.isNotEmpty(sWord)) {  // If word mode and a word was found
		            sLine = sWord + sLine;  // Append translated word to line
		            sWord = "";  // Clear word text
		         }

		         sWord += sChar;  // Build punctuation, symbol & whitespace "word"
		         bWord = false;  // Switch to non-word/separator mode

		         if (sChar == '\r' || sChar == '\n') {  // If end of line
		            sRtlEn += sLine + sWord;  // Append line and word to result
		            sLine = "";  // Clear line text
		            sWord = "";  // Clear word text
		         }
		         /* TODO improve someday so it comes out nicer with some punc symbols
		         else if (sChar == ':' || sChar == '.' || sChar == '?' || sChar == ' ') {  // If colon, treat special and append immediately
			            sLine = sWord + sLine;  // Append immediately
			            sWord = "";  // Clear word text
			     }
			     */
		      }

		   }  // for

		   return sRtlEn + sWord + sLine;  // Append final line and word to result
	}
}
