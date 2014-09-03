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

import java.util.HashSet;
import java.util.Set;

import divconq.lang.StringBuilder32;
import divconq.lang.stem.English;

public class IndexUtility {
	static protected Set<String> stopwords = new HashSet<String>();

	/*
	static public boolean isStopWord(String word) {
		return FullTextIndex.stopwords.contains(FullTextIndex.prepPhrase(word));
	}
	*/

	static public IndexInfo stemEnglishPhrase(CharSequence phrase, int score) {
		IndexInfo info = new IndexInfo();		
		IndexUtility.stemEnglishPhraseAppend(phrase, score, info);        
		return info;
	}
	
	static public void stemEnglishPhraseAppend(CharSequence phrase, int score, IndexInfo info) {
		if ((phrase == null) || (info == null))
			return;
		
	    StringBuilder32 word = new StringBuilder32();
        int start = 0;
	    int prior = info.getContentLength();
	    
	    if (prior > 0)
	    	prior++;		// for the padding 
	    
	    // TODO strip out any "words" (alphanumerics) more than 125 characters long
	    
        info.addContent(phrase);
	            
        for (int i = 0; i < phrase.length(); i++) {
        	char l = phrase.charAt(i);
        	
        	// translate extended chars
    		if (l == 'ƒ')
    			l = 'f';
    		else if (l == 'Š')
    			l = 's';
    		else if (l == 'Ž')
    			l = 'z';
    		else if (l == 'š')
    			l = 's';
    		else if (l == 'ž')
    			l = 'z';
    		else if (l == 'Ÿ')
    			l = 'y';
    		else if (l == '¡')
    			l = 'i';
    		else if (l == 'À')
    			l = 'a';
    		else if (l == 'Á')
    			l = 'a';
    		else if (l == 'Â')
    			l = 'a';
    		else if (l == 'Ã')
    			l = 'a';
    		else if (l == 'Ä')
    			l = 'a';
    		else if (l == 'Å')
    			l = 'a';
    		else if (l == 'Ç')
    			l = 'c';
    		else if (l == 'È')
    			l = 'e';
    		else if (l == 'É')
    			l = 'e';
    		else if (l == 'Ê')
    			l = 'e';
    		else if (l == 'Ë')
    			l = 'e';
    		else if (l == 'Ì')
    			l = 'i';
    		else if (l == 'Í')
    			l = 'i';
    		else if (l == 'Î')
    			l = 'i';
    		else if (l == 'Ï')
    			l = 'i';
    		else if (l == 'Ñ')
    			l = 'n';
    		else if (l == 'Ò')
    			l = 'o';
    		else if (l == 'Ó')
    			l = 'o';
    		else if (l == 'Ô')
    			l = 'o';
    		else if (l == 'Õ')
    			l = 'o';
    		else if (l == 'Ö')
    			l = 'o';
    		else if (l == 'Ø')
    			l = 'o';
    		else if (l == 'Ù')
    			l = 'u';
    		else if (l == 'Ú')
    			l = 'u';
    		else if (l == 'Û')
    			l = 'u';
    		else if (l == 'Ü')
    			l = 'u';
    		else if (l == 'Ý')
    			l = 'y';
    		else if (l == 'ß')
    			l = 's';
    		else if (l == 'à')
    			l = 'a';
    		else if (l == 'á')
    			l = 'a';
    		else if (l == 'â')
    			l = 'a';
    		else if (l == 'ã')
    			l = 'a';
    		else if (l == 'ä')
    			l = 'a';
    		else if (l == 'å')
    			l = 'a';
    		else if (l == 'ç')
    			l = 'c';
    		else if (l == 'è')
    			l = 'e';
    		else if (l == 'é')
    			l = 'e';
    		else if (l == 'ê')
    			l = 'e';
    		else if (l == 'ë')
    			l = 'e';
    		else if (l == 'ì')
    			l = 'i';
    		else if (l == 'í')
    			l = 'i';
    		else if (l == 'î')
    			l = 'i';
    		else if (l == 'ï')
    			l = 'i';
    		else if (l == 'ñ')
    			l = 'n';
    		else if (l == 'ò')
    			l = 'o';
    		else if (l == 'ó')
    			l = 'o';
    		else if (l == 'ô')
    			l = 'o';
    		else if (l == 'õ')
    			l = 'o';
    		else if (l == 'ö')
    			l = 'o';
    		else if (l == 'ø')
    			l = 'o';
    		else if (l == 'ù')
    			l = 'u';
    		else if (l == 'ú')
    			l = 'u';
    		else if (l == 'û')
    			l = 'u';
    		else if (l == 'ü')
    			l = 'u';
    		else if (l == 'ý')
    			l = 'y';
    		else if (l == 'ÿ')
    			l = 'y';	
        	
        	// allow only A - Z, a - z and '
        	if (((l < 65) && (l != 39)) || ((l > 90) && (l < 97)) || (l > 122)) {
        		if (word.length() > 2) {
        			String proposed = word.toString().toLowerCase();
        			
        			if (!IndexUtility.stopwords.contains(proposed)) { 
	        			info.add(English.toStem(proposed), score, prior + start + 1);
	        			
	        			if (proposed.indexOf('\'') > -1)
		        			info.add(English.toStem(proposed).replaceAll("'", ""), score, prior + start + 1);
        			}
        		}
        		
        		word.reset();
        		start = i;
        	}
        	else
        		word.append(l);
        }
        
		if (word.length() > 2) {
			String proposed = word.toString().toLowerCase();
			
			if (!IndexUtility.stopwords.contains(proposed)) { 
    			info.add(English.toStem(proposed), score, prior + start + 1);
    			
    			if (proposed.indexOf('\'') > -1)
        			info.add(English.toStem(proposed).replaceAll("'", ""), score, prior + start + 1);
			}
		}
	}
	
	static {
		IndexUtility.stopwords.add("i");		
		IndexUtility.stopwords.add("me");
		IndexUtility.stopwords.add("my");
		IndexUtility.stopwords.add("myself");
		IndexUtility.stopwords.add("we");
		IndexUtility.stopwords.add("our");
		IndexUtility.stopwords.add("ours");
		IndexUtility.stopwords.add("ourselves");
		IndexUtility.stopwords.add("you");
		IndexUtility.stopwords.add("your");
		IndexUtility.stopwords.add("yours");
		IndexUtility.stopwords.add("yourself");
		IndexUtility.stopwords.add("yourselves");
		IndexUtility.stopwords.add("he");
		IndexUtility.stopwords.add("him");
		IndexUtility.stopwords.add("his");
		IndexUtility.stopwords.add("himself");
		IndexUtility.stopwords.add("she");
		IndexUtility.stopwords.add("her");
		IndexUtility.stopwords.add("hers");
		IndexUtility.stopwords.add("herself");
		IndexUtility.stopwords.add("it");
		IndexUtility.stopwords.add("its");
		IndexUtility.stopwords.add("itself");
		IndexUtility.stopwords.add("they");
		IndexUtility.stopwords.add("them");
		IndexUtility.stopwords.add("their");
		IndexUtility.stopwords.add("theirs");
		IndexUtility.stopwords.add("themselves");
		IndexUtility.stopwords.add("what");
		IndexUtility.stopwords.add("which");
		IndexUtility.stopwords.add("who");
		IndexUtility.stopwords.add("whom");
		IndexUtility.stopwords.add("this");
		IndexUtility.stopwords.add("that");
		IndexUtility.stopwords.add("these");
		IndexUtility.stopwords.add("those");
		IndexUtility.stopwords.add("am");
		IndexUtility.stopwords.add("is");
		IndexUtility.stopwords.add("are");
		IndexUtility.stopwords.add("was");
		IndexUtility.stopwords.add("were");
		IndexUtility.stopwords.add("be");
		IndexUtility.stopwords.add("been");
		IndexUtility.stopwords.add("being");
		IndexUtility.stopwords.add("have");
		IndexUtility.stopwords.add("has");
		IndexUtility.stopwords.add("had");
		IndexUtility.stopwords.add("having");
		IndexUtility.stopwords.add("do");
		IndexUtility.stopwords.add("does");
		IndexUtility.stopwords.add("did");
		IndexUtility.stopwords.add("doing");
		IndexUtility.stopwords.add("would");
		IndexUtility.stopwords.add("should");
		IndexUtility.stopwords.add("could");
		IndexUtility.stopwords.add("ought");
		IndexUtility.stopwords.add("i'm");
		IndexUtility.stopwords.add("you're");
		IndexUtility.stopwords.add("he's");
		IndexUtility.stopwords.add("she's");
		IndexUtility.stopwords.add("it's");
		IndexUtility.stopwords.add("we're");
		IndexUtility.stopwords.add("they're");
		IndexUtility.stopwords.add("i've");
		IndexUtility.stopwords.add("you've");
		IndexUtility.stopwords.add("we've");
		IndexUtility.stopwords.add("they've");
		IndexUtility.stopwords.add("i'd");
		IndexUtility.stopwords.add("you'd");
		IndexUtility.stopwords.add("he'd");
		IndexUtility.stopwords.add("she'd");
		IndexUtility.stopwords.add("we'd");
		IndexUtility.stopwords.add("they'd");
		IndexUtility.stopwords.add("i'll");
		IndexUtility.stopwords.add("you'll");
		IndexUtility.stopwords.add("he'll");
		IndexUtility.stopwords.add("she'll");
		IndexUtility.stopwords.add("we'll");
		IndexUtility.stopwords.add("they'll");
		IndexUtility.stopwords.add("isn't");
		IndexUtility.stopwords.add("aren't");
		IndexUtility.stopwords.add("wasn't");
		IndexUtility.stopwords.add("weren't");
		IndexUtility.stopwords.add("hasn't");
		IndexUtility.stopwords.add("haven't");
		IndexUtility.stopwords.add("hadn't");
		IndexUtility.stopwords.add("doesn't");
		IndexUtility.stopwords.add("don't");
		IndexUtility.stopwords.add("didn't");
		IndexUtility.stopwords.add("won't");
		IndexUtility.stopwords.add("wouldn't");
		IndexUtility.stopwords.add("shan't");
		IndexUtility.stopwords.add("shouldn't");
		IndexUtility.stopwords.add("can't");
		IndexUtility.stopwords.add("cannot");
		IndexUtility.stopwords.add("couldn't");
		IndexUtility.stopwords.add("mustn't");
		IndexUtility.stopwords.add("let's");
		IndexUtility.stopwords.add("that's");
		IndexUtility.stopwords.add("who's");
		IndexUtility.stopwords.add("what's");
		IndexUtility.stopwords.add("here's");
		IndexUtility.stopwords.add("there's");
		IndexUtility.stopwords.add("when's");
		IndexUtility.stopwords.add("where's");
		IndexUtility.stopwords.add("why's");
		IndexUtility.stopwords.add("how's");
		IndexUtility.stopwords.add("a");
		IndexUtility.stopwords.add("an");
		IndexUtility.stopwords.add("the");
		IndexUtility.stopwords.add("and");
		IndexUtility.stopwords.add("but");
		IndexUtility.stopwords.add("if");
		IndexUtility.stopwords.add("or");
		IndexUtility.stopwords.add("because");
		IndexUtility.stopwords.add("as");
		IndexUtility.stopwords.add("until");
		IndexUtility.stopwords.add("while");
		IndexUtility.stopwords.add("of");
		IndexUtility.stopwords.add("at");
		IndexUtility.stopwords.add("by");
		IndexUtility.stopwords.add("for");
		IndexUtility.stopwords.add("with");
		IndexUtility.stopwords.add("about");
		IndexUtility.stopwords.add("against");
		IndexUtility.stopwords.add("between");
		IndexUtility.stopwords.add("into");
		IndexUtility.stopwords.add("through");
		IndexUtility.stopwords.add("during");
		IndexUtility.stopwords.add("before");
		IndexUtility.stopwords.add("after");
		IndexUtility.stopwords.add("above");
		IndexUtility.stopwords.add("below");
		IndexUtility.stopwords.add("to");
		IndexUtility.stopwords.add("from");
		IndexUtility.stopwords.add("up");
		IndexUtility.stopwords.add("down");
		IndexUtility.stopwords.add("in");
		IndexUtility.stopwords.add("out");
		IndexUtility.stopwords.add("on");
		IndexUtility.stopwords.add("off");
		IndexUtility.stopwords.add("over");
		IndexUtility.stopwords.add("under");
		IndexUtility.stopwords.add("again");
		IndexUtility.stopwords.add("further");
		IndexUtility.stopwords.add("then");
		IndexUtility.stopwords.add("once");
		IndexUtility.stopwords.add("here");
		IndexUtility.stopwords.add("there");
		IndexUtility.stopwords.add("when");
		IndexUtility.stopwords.add("where");
		IndexUtility.stopwords.add("why");
		IndexUtility.stopwords.add("how");
		IndexUtility.stopwords.add("all");
		IndexUtility.stopwords.add("any");
		IndexUtility.stopwords.add("both");
		IndexUtility.stopwords.add("each");
		IndexUtility.stopwords.add("few");
		IndexUtility.stopwords.add("more");
		IndexUtility.stopwords.add("most");
		IndexUtility.stopwords.add("other");
		IndexUtility.stopwords.add("some");
		IndexUtility.stopwords.add("such");
		IndexUtility.stopwords.add("no");
		IndexUtility.stopwords.add("nor");
		IndexUtility.stopwords.add("not");
		IndexUtility.stopwords.add("only");
		IndexUtility.stopwords.add("own");
		IndexUtility.stopwords.add("same");
		IndexUtility.stopwords.add("so");
		IndexUtility.stopwords.add("than");
		IndexUtility.stopwords.add("too");
		IndexUtility.stopwords.add("very");
	}

}
