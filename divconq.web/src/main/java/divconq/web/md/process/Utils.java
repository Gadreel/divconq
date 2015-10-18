/*
 * Copyright (C) 2011 René Jeschke <rene_jeschke@yahoo.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package divconq.web.md.process;

import java.util.LinkedList;

/**
 * Utilities.
 * 
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
class Utils
{
    /** Random number generator value. */
    private static int RND = (int)System.nanoTime();

    /**
     * LCG random number generator.
     * 
     * @return A pseudo random number between 0 and 1023
     */
    public static int rnd()
    {
        return (RND = RND * 1664525 + 1013904223) >>> 22;
    }

    /**
     * Skips spaces in the given String.
     * 
     * @param in
     *            Input String.
     * @param start
     *            Starting position.
     * @return The new position or -1 if EOL has been reached.
     */
    public static int skipSpaces(String in, int start)
    {
        int pos = start;
        while(pos < in.length() && (in.charAt(pos) == ' ' || in.charAt(pos) == '\n'))
            pos++;
        return pos < in.length() ? pos : -1;
    }

    /**
     * Processed the given escape sequence.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param ch
     *            The character.
     * @param pos
     *            Current parsing position.
     * @return The new position.
     */
    public static int escape(StringBuilder out, char ch, int pos)
    {
        switch(ch)
        {
        case '\\':
        case '[':
        case ']':
        case '(':
        case ')':
        case '{':
        case '}':
        case '#':
        case '"':
        case '\'':
        case '.':
        case '>':
        case '<':
        case '*':
        case '+':
        case '-':
        case '_':
        case '!':
        case '`':
        case '^':
            out.append(ch);
            return pos + 1;
        default:
            out.append('\\');
            return pos;
        }
    }

    /**
     * Reads characters until any 'end' character is encountered.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            The Input String.
     * @param start
     *            Starting position.
     * @param end
     *            End characters.
     * @return The new position or -1 if no 'end' char was found.
     */
    public static int readUntil(StringBuilder out, String in, int start, char... end)
    {
        int pos = start;
        while(pos < in.length())
        {
            char ch = in.charAt(pos);
            if(ch == '\\' && pos + 1 < in.length())
            {
                pos = escape(out, in.charAt(pos + 1), pos);
            }
            else
            {
                boolean endReached = false;
                for(int n = 0; n < end.length; n++)
                {
                    if(ch == end[n])
                    {
                        endReached = true;
                        break;
                    }
                }
                if(endReached)
                    break;
                out.append(ch);
            }
            pos++;
        }

        return (pos == in.length()) ? -1 : pos;
    }

    /**
     * Reads a markdown link.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     * @param start
     *            Starting position.
     * @return The new position or -1 if this is no valid markdown link.
     */
    public static int readMdLink(StringBuilder out, String in, int start)
    {
        int pos = start;
        int counter = 1;
        while(pos < in.length())
        {
            char ch = in.charAt(pos);
            if(ch == '\\' && pos + 1 < in.length())
            {
                pos = escape(out, in.charAt(pos + 1), pos);
            }
            else
            {
                boolean endReached = false;
                switch(ch)
                {
                case '(':
                    counter++;
                    break;
                case ' ':
                    if(counter == 1)
                        endReached = true;
                    break;
                case ')':
                    counter--;
                    if(counter == 0)
                        endReached = true;
                    break;
                }
                if(endReached)
                    break;
                out.append(ch);
            }
            pos++;
        }

        return (pos == in.length()) ? -1 : pos;
    }

    /**
     * Reads a markdown link ID.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     * @param start
     *            Starting position.
     * @return The new position or -1 if this is no valid markdown link ID.
     */
    public static int readMdLinkId(StringBuilder out, String in, int start)
    {
        int pos = start;
        int counter = 1;
        while(pos < in.length())
        {
            char ch = in.charAt(pos);
            boolean endReached = false;
            switch(ch)
            {
            case '\n':
                out.append(' ');
                break;
            case '[':
                counter++;
                out.append(ch);
                break;
            case ']':
                counter--;
                if(counter == 0)
                    endReached = true;
                else
                    out.append(ch);
                break;
            default:
                out.append(ch);
                break;
            }
            if(endReached)
                break;
            pos++;
        }

        return (pos == in.length()) ? -1 : pos;
    }

    /**
     * Reads characters until the end character is encountered, ignoring escape
     * sequences.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            The Input String.
     * @param start
     *            Starting position.
     * @param end
     *            End characters.
     * @return The new position or -1 if no 'end' char was found.
     */
    public static int readRawUntil(StringBuilder out, String in, int start, char end)
    {
        int pos = start;
        while(pos < in.length())
        {
            char ch = in.charAt(pos);
            if(ch == end)
                break;
            out.append(ch);
            pos++;
        }

        return (pos == in.length()) ? -1 : pos;
    }

    /**
     * Extracts the tag from an XML element.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     */
    public static void getXMLTag(StringBuilder out, String in)
    {
        int pos = 1;
        if(in.charAt(1) == '/')
            pos++;
        while(Character.isLetterOrDigit(in.charAt(pos)))
        {
            out.append(in.charAt(pos++));
        }
    }

    public static int scanHTML(String in, int start) {
        LinkedList<String> tags = new LinkedList<String>();
        StringBuilder temp = new StringBuilder();
        int pos = start;
        
        if (in.length() <= 4)
        	return -1;
        
        /* TODO add comment support
        if (in.charAt(pos + 1) == '!') {
            if (this.readXMLComment(this, this.leading) > 0)
                return true;
        }
        */
        
        pos = Utils.readXML(temp, in, pos, false);
        String element, tag;
        
        if (pos > -1) {
            element = temp.toString();
            temp.setLength(0);
            
            Utils.getXMLTag(temp, element);
            tag = temp.toString().toLowerCase();
            
            char sl = element.charAt(element.length() - 2);
            
            if (sl == '/') 
                return pos;
            
            tags.add(tag);

            while (pos < in.length()) {
	        	// TODO check/add support for xml comments
	        	
	            while (pos < in.length() && in.charAt(pos) != '<')
	                pos++;
	
	            if (pos >= in.length()) 
	            	return -1;
	
	            temp.setLength(0);
	            int newPos = Utils.readXML(temp, in, pos, false);
	            
	            if (newPos > 0) {
	                element = temp.toString();
	                temp.setLength(0);
	                Utils.getXMLTag(temp, element);
	                tag = temp.toString().toLowerCase();
	                
	                sl = element.charAt(element.length() - 2);
	                
	                if(element.charAt(1) == '/') {
	                    if(!tags.getLast().equals(tag))
	                        return -1;
	                    
	                    tags.removeLast();
	                }
	                else if (sl != '/') {
	                    tags.addLast(tag);
	                }
	                
	                pos = newPos;
	            }
	            else {
	                pos++;
	            }
	            
	            if (tags.size() == 0)
	            	return pos;
            }
        }
        
        return -1;
    }    
    
    /**
     * Reads an XML element.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     * @param start
     *            Starting position.
     * @param safeMode
     *            Whether to escape unsafe HTML tags or not
     * @return The new position or -1 if this is no valid XML element.
     */
    public static int readXML(StringBuilder out, String in, int start, boolean safeMode)
    {
        int pos;
        boolean isCloseTag;
        try
        {
            if(in.charAt(start + 1) == '/')
            {
                isCloseTag = true;
                pos = start + 2;
            }
            else if(in.charAt(start + 1) == '!')
            {
                out.append("<!");
                return start + 1;
            }
            else
            {
                isCloseTag = false;
                pos = start + 1;
            }
            if(safeMode)
            {
                StringBuilder temp = new StringBuilder();
                pos = readRawUntil(temp, in, pos, ' ', '/', '>');
                if(pos == -1)
                    return -1;
                //String tag = temp.toString().trim().toLowerCase();
                
                // TODO?
                out.append("&lt;");
                if(isCloseTag)
                    out.append('/');
                out.append(temp);
            }
            else
            {
                out.append('<');
                if(isCloseTag)
                    out.append('/');
                pos = readRawUntil(out, in, pos, ' ', '/', '>');
            }
            if(pos == -1)
                return -1;
            pos = readRawUntil(out, in, pos, '/', '>');
            if(in.charAt(pos) == '/')
            {
                out.append(" /");
                pos = readRawUntil(out, in, pos + 1, '>');
                if(pos == -1)
                    return -1;
            }
            if(in.charAt(pos) == '>')
            {
                out.append('>');
                return pos;
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            return -1;
        }
        return -1;
    }
    

    /**
     * Reads characters until any 'end' character is encountered, ignoring
     * escape sequences.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            The Input String.
     * @param start
     *            Starting position.
     * @param end
     *            End characters.
     * @return The new position or -1 if no 'end' char was found.
     */
    public static int readRawUntil(StringBuilder out, String in, int start, char... end)
    {
        int pos = start;
        while(pos < in.length())
        {
            char ch = in.charAt(pos);
            boolean endReached = false;
            for(int n = 0; n < end.length; n++)
            {
                if(ch == end[n])
                {
                    endReached = true;
                    break;
                }
            }
            if(endReached)
                break;
            out.append(ch);
            pos++;
        }

        return (pos == in.length()) ? -1 : pos;
    }
    
    

    /**
     * Appends the given string to the given StringBuilder, replacing '&amp;',
     * '&lt;' and '&gt;' by their respective HTML entities.
     * 
     * @param out
     *            The StringBuilder to append to.
     * @param value
     *            The string to append.
     * @param offset
     *            The character offset into value from where to start
     */
    public static void codeEncode(StringBuilder out, String value, int offset)
    {
        for(int i = offset; i < value.length(); i++)
        {
            char c = value.charAt(i);
            switch(c)
            {
            case '&':
                out.append("&amp;");
                break;
            case '<':
                out.append("&lt;");
                break;
            case '>':
                out.append("&gt;");
                break;
            default:
                out.append(c);
            }
        }
    }

    /**
     * Removes trailing <code>`</code> and trims spaces.
     * 
     * @param fenceLine
     *            Fenced code block starting line
     * @return Rest of the line after trimming and backtick removal
     * @since 0.7
     */
    public static String getMetaFromFence(String fenceLine)
    {
        for(int i = 0; i < fenceLine.length(); i++)
        {
            char c = fenceLine.charAt(i);
            if(!Character.isWhitespace(c) && c != '`' && c != '~' && c != '%')
            {
                return fenceLine.substring(i).trim();
            }
        }
        return "";
    }
}
