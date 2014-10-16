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
 * PigLatin support is based on the online "English to Pig Latin Translator" (http://users.snowcrest.net/donnelly/piglatin.html)
 * by William H. Donnelly
 * 
 * @author Andy
 *
 */
public class PigLatin {
	static final String gcsVOWELS = "AEIOUaeiou";     // Standard English vowels
	static final String gcsVOWELSY = "AEIOUaeiouYy";  // Sometimes "Y" and "W", but only "Y" here
	static final String gcsWAY = "way";               // Vowel word suffix
	static final String gcsAY = "ay";                 // Consonant word suffix

	/**
	 * Translate test in English to Pig Latin  
	 * 
	 * @param sText text to translate
	 * @return translated text
	 */
	public static String translate(String sText) {
		return PigLatin.translate(sText, false);
	}

	/**
	 * Translate test in English to Pig Latin  
	 * 
	 * @param sText text to translate
	 * @return translated text
	 */
	public static String translate(String sText, boolean bLearnMode) {
		if (StringUtil.isEmpty(sText))
			return null;

		// Translate text from English to Pig Latin, line by line, word by word

		   String sPigLatin = "";    // Pig Latin translation result
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
		            sLine += sWord;  // Append punctuation & whitespace to line
		            sWord = "";  // Clear word text
		            bWord = true;  // We're working on a real word
		         }

		         sWord += sChar;  // Append alpha character to word
		      } 
		      else {  // A non-alpha character
		         if (bWord && StringUtil.isNotEmpty(sWord)) {  // If word mode and a word was found
		            sWord = PigLatin.fPigLatin (sWord, bLearnMode);  // Translate word to Pig Latin
		            sLine += sWord;  // Append translated word to line
		            sWord = "";  // Clear word text
		         }

		         sWord += sChar;  // Build punctuation, symbol & whitespace "word"
		         bWord = false;  // Switch to non-word/separator mode

		         if (sChar == '\r' || sChar == '\n') {  // If end of line
		            sPigLatin += sLine + sWord;  // Append line and word to result
		            sLine = "";  // Clear line text
		            sWord = "";  // Clear word text
		         }
		      }

		   }  // for

		   if (bWord && StringUtil.isNotEmpty(sWord))   // If word mode and a word was found
			   sWord = PigLatin.fPigLatin (sWord, bLearnMode);  // Translate word to Pig Latin

		   return sPigLatin + sLine + sWord;  // Append final line and word to result
	}

	private static String fPigLatin(String sWord, boolean bLearnMode) {

		// Translate a word from English to Pig Latin -- word is in parameter #1

		   char sFirst = sWord.charAt(0);       // First character of word
		   String sSuffix = "";      // Pig Latin suffix text
		   char sLast = ' ';        // Last character of word (used differently below)
		   
		// Word capitalization flag
		   boolean bCapitalize = (sFirst == Character.toUpperCase(sFirst)) ? true : false;    
		   boolean bCapsFlag = false;    // Suffix capitalization flag


		   if (PigLatin.gcsVOWELS.indexOf (sFirst) >= 0) {  // Word starts with a vowel?
		      sSuffix = PigLatin.gcsWAY;  // Suffix is "way"
		      sLast = sWord.charAt(sWord.length() - 1);  // Get last char of word

		      if (sLast == Character.toUpperCase(sLast) && sWord.length() > 1)  // If last char of word is uppercase (except "I")
		         sSuffix = PigLatin.gcsWAY.toUpperCase();  // Make suffix uppercase to match

		      // At this point, the word is translated correctly

		   } else {  // Word starts with consonant(s) -- more complex processing required

		      // Move all consonants at front of word to the end and add "ay"

		      if (sWord != sWord.toUpperCase())  // If not all caps
		         sFirst = Character.toLowerCase(sFirst);  // Format for display

		      // For typos and any possible all-consonant "words"
		      while (sWord.length() > 1) {
		        sSuffix += sFirst;  // Build suffix with leading consonants
		        sLast = sFirst;  // Save last character (for "qu" testing)

		        bCapsFlag = (sFirst == Character.toUpperCase(sFirst)) ? true : false;  // Capitalize flag

		        sWord = sWord.substring (1, sWord.length());  // Remove first/next char of word
		        
		        if (sWord.length() == 0)
		        	continue;
		        
		        sFirst = sWord.charAt (0);  // Get next/first char of new word

		        if (PigLatin.gcsVOWELSY.indexOf (sFirst) >= 0) {  // Vowel signals end
		           if (!((sLast == 'q' || sLast == 'Q') &&
		                 (sFirst == 'u' || sFirst == 'U')))  // Check for "qu"
		              break;  // Quit loop if we hit a vowel or "y" (unless "qu")
		        }

		      }  // while

		      if (bCapsFlag)  // If the first char of the new word is capitalized
		         sSuffix += PigLatin.gcsAY.toUpperCase();  // Append "AY"

		      else
		         sSuffix += PigLatin.gcsAY;  // Append "ay"
		   }

		   sWord += (bLearnMode ? "-" : "") + sSuffix;  // Put final translated word together

		   if (bCapitalize) {  // If original word was capitalized...
		      sFirst = sWord.charAt (0);  // ...ensure translated word is too
		      sWord = Character.toUpperCase(sFirst) + sWord.substring (1, sWord.length());
		   }

		   return sWord;  // Return Pig Latin word
	}
}
