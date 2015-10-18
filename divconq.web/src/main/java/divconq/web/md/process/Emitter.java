package divconq.web.md.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import divconq.lang.op.FuncResult;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XText;
import divconq.xml.XmlReader;

class Emitter {
    protected Configuration config = null;
    protected HashMap<String, LinkRef> linkRefs = new HashMap<String, LinkRef>();
    protected Map<String, Plugin> plugins = new HashMap<String, Plugin>();
    
    public Emitter(Configuration config) {
        this.config = config;
        
        for(Plugin plugin : config.plugins) 
          	register(plugin);
    }
    
	public void register(Plugin plugin) {
		plugins.put(plugin.getIdPlugin(), plugin);
	}
    
    public void addLinkRef(String key, LinkRef linkRef) {
        this.linkRefs.put(key.toLowerCase(), linkRef);
    }

    public void emit(WebContext ctx, XElement parent, Block root) {
        root.removeSurroundingEmptyLines();

        XElement target = null;
        
        switch(root.type) {
        case RULER:
        	parent.add(new XElement("hr"));
            return;
        case NONE:
        case XML:
        case PLUGIN:
        	target = parent;
            break;
        case HEADLINE: {
        	target = new XElement("h" + root.hlDepth);
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        }
        case PARAGRAPH:
        	target = new XElement("p");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        case CODE:
        case FENCED_CODE:
        	XElement targetparent = new XElement("pre");
        	target = new XElement("code");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
            targetparent.add(target);
        	parent.add(targetparent);
            
            break;
        case BLOCKQUOTE:
        	target = new XElement("blockquote");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        case UNORDERED_LIST:
        	target = new XElement("ul");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        case ORDERED_LIST:
        	target = new XElement("ol");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        case LIST_ITEM:
        	target = new XElement("li");
        	
            if (root.id != null)
            	target.setAttribute("id", root.id);
            
        	parent.add(target);
            
            break;
        }

        if(root.hasLines())  {
            switch(root.type)
            {
            case CODE:
                this.emitCodeLines(ctx, target, root.lines, root.meta, true);
                break;
            case FENCED_CODE:
                this.emitCodeLines(ctx, target, root.lines, root.meta, false);
                break;
            case PLUGIN:
                this.emitPluginLines(ctx, target, root.lines, root.meta);
                break;
            case XML:
                this.emitRawLines(ctx, target, root.lines);
                break;
            default:
                this.emitMarkedLines(ctx, target, root.lines);
                break;
            }
        }
        else {
            Block block = root.blocks;
            
            while (block != null) {
                this.emit(ctx, target, block);
                block = block.next;
            }
        }
    }

    /**
     * Finds the position of the given Token in the given String.
     * 
     * @param in
     *            The String to search on.
     * @param start
     *            The starting character position.
     * @param token
     *            The token to find.
     * @return The position of the token or -1 if none could be found.
     */
    private int findToken(String in, int start, MarkToken token)
    {
        int pos = start;
        while(pos < in.length())
        {
            if(this.getToken(in, pos) == token)
                return pos;
            pos++;
        }
        return -1;
    }

    /*
     * Checks if there is a valid markdown link definition.
     */
    protected int emitLink(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
        boolean isAbbrev = false;
        int pos = start + (token == MarkToken.LINK ? 1 : (token == MarkToken.X_IMAGE) ? 3 : 2);
        
        StringBuilder temp = new StringBuilder();
        temp.setLength(0);
        
        pos = Utils.readMdLinkId(temp, in, pos);
        
        if (pos < start)
            return -1;

        String name = temp.toString(), link = null, comment = null;
        int oldPos = pos++;
        pos = Utils.skipSpaces(in, pos);
        
        if (pos < start) {
            LinkRef lr = this.linkRefs.get(name.toLowerCase());
            
            if (lr == null) 
                return -1;
            	
            isAbbrev = lr.isAbbrev;
            link = lr.link;
            comment = lr.title;
            pos = oldPos;
        }
        else if (in.charAt(pos) == '(') {
            pos++;
            pos = Utils.skipSpaces(in, pos);
            
            if (pos < start)
                return -1;
            
            temp.setLength(0);
            boolean useLt = in.charAt(pos) == '<';
            
            pos = useLt ? Utils.readUntil(temp, in, pos + 1, '>') : Utils.readMdLink(temp, in, pos);
            
            if (pos < start)
                return -1;
            
            if (useLt)
                pos++;
            
            link = temp.toString();

            if (in.charAt(pos) == ' ') {
                pos = Utils.skipSpaces(in, pos);
                
                if (pos > start && in.charAt(pos) == '"') {
                    pos++;
                    temp.setLength(0);
                    pos = Utils.readUntil(temp, in, pos, '"');
                    
                    if (pos < start)
                        return -1;
                    
                    comment = temp.toString();
                    pos++;
                    pos = Utils.skipSpaces(in, pos);
                    
                    if (pos == -1)
                        return -1;
                }
            }

            // grab the position for X_IMAGE
            if (pos > start && in.charAt(pos) == '"') {
                pos++;
                temp.setLength(0);
                pos = Utils.readUntil(temp, in, pos, '"');
                
                if(pos < start)
                    return -1;
                
                //position = temp.toString();
                pos++;
                pos = Utils.skipSpaces(in, pos);
                
                if(pos == -1)
                    return -1;
            }
            
            if (in.charAt(pos) != ')')
                return -1;
        }
        else if (in.charAt(pos) == '[') {
            pos++;
            temp.setLength(0);
            pos = Utils.readRawUntil(temp, in, pos, ']');
            
            if (pos < start)
                return -1;
            
            String id = (temp.length() > 0) ? temp.toString() : name;
            LinkRef lr = this.linkRefs.get(id.toLowerCase());
            
            if (lr != null) {
                link = lr.link;
                comment = lr.title;
            }
        }
        else {
            LinkRef lr = this.linkRefs.get(name.toLowerCase());
            
            if (lr == null)
                return -1;
            
            isAbbrev = lr.isAbbrev;
            link = lr.link;
            comment = lr.title;
            pos = oldPos;
        }

        if (link == null)
            return -1;

        if (token == MarkToken.LINK) {
            if(isAbbrev && comment != null) {
            	XElement anchr = new XElement("abbr")
	        		.withAttribute("title", comment);
            	
                this.recursiveEmitLine(ctx, anchr, name, 0, MarkToken.NONE);  
            }
            else {
            	XElement anchr = new XElement("a")
	        		.withAttribute("href", link)
	        		.withAttribute("alt", name);
            	
            	parent.add(anchr);
            	
                if(comment != null)
                	anchr.withAttribute("title", comment);

                this.recursiveEmitLine(ctx, anchr, name, 0, MarkToken.NONE);  
            }
        }
        else if (token == MarkToken.IMAGE) {
        	XElement img = new XElement("img")
        		.withAttribute("src", link)
        		.withAttribute("alt", name);
        	
            if (comment != null)
            	img.withAttribute("title", comment);
        	
        	parent.add(img);
        }
        else {		// X_IMAGE a captioned image
        	XElement div = new XElement("div")
	    		.withAttribute("class", "inline-img");
	    		
	    	div.add(new XElement("img")
	    		.withAttribute("src", link)
	    		.withAttribute("alt", name));
	    	
	        if (comment != null)
	        	div.add(new XElement("div").withText(comment));
        	
	    	parent.add(div);
        }

        return pos;
    }

    /**
     * Check if there is a valid HTML tag here. This method also transforms auto
     * links and mailto auto links.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     * @param start
     *            Starting position.
     * @return The new position or -1 if nothing valid has been found.
     */
    protected int emitHtml(WebContext ctx, XElement parent, String in, int start) {
        StringBuilder temp = new StringBuilder();
        int pos;

        // Check for auto links
        temp.setLength(0);
        
        pos = Utils.readUntil(temp, in, start + 1, ':', ' ', '>', '\n');
        
        if (pos != -1 && in.charAt(pos) == ':' && in.charAt(pos - 1) == '/' && in.charAt(pos - 2) == '/') {
            pos = Utils.readUntil(temp, in, pos, '>');
            
            if (pos != -1) {
                String link = temp.toString();
                
                parent.add(new XElement("a").withAttribute("href", link).withText(link));
                return pos;
            }
        }

        // Check for mailto or address auto link
        temp.setLength(0);
        pos = Utils.readUntil(temp, in, start + 1, '@', ' ', '>', '\n');
        
        if (pos != -1 && in.charAt(pos) == '@') {
            pos = Utils.readUntil(temp, in, pos, '>');
            
            if (pos != -1) {
                String link = temp.toString();
                
                XElement xml = new XElement("a");
                
                parent.add(xml);
                
                //address auto links
                if(link.startsWith("@")) {
                	String slink = link.substring(1);
            		String url = "https://maps.google.com/maps?q=" + slink.replace(' ', '+');
                    
                    xml.withAttribute("href", url).withText(slink);
                }
                //mailto auto links
                else {
                    xml.withAttribute("href", "mailto:" + link).withText(link);
                }
                
                return pos;
            }
        }

        // Check for inline html
        if (start + 2 < in.length()) {
            //temp.setLength(0);
            //pos = Utils.readXML(temp, in, start, this.config.safeMode);
            
        	pos = Utils.scanHTML(in, start);
        	
            if (pos > 0) {
            	String xml = in.substring(start, pos + 1);
            	
            	FuncResult<XElement> xres = XmlReader.parse(xml, false);
            	
            	if (xres.isNotEmptyResult())
            		parent.add(xres.getResult());
            }
            
            return pos;
        }

        return -1;
    }

    /**
     * Check if this is a valid XML/HTML entity.
     * 
     * @param out
     *            The StringBuilder to write to.
     * @param in
     *            Input String.
     * @param start
     *            Starting position
     * @return The new position or -1 if this entity in invalid.
     */
    protected int emitEntity(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
    	/*
        int pos = start;
        
        while (pos < in.length()) {
            if (in.charAt(pos) == ';')
                break;
            
            pos++;
        }
    	
        // nothing found
    	if ((pos == in.length()) || (pos - start < 3))
    		return -1;
        */
    	
    	if (in.length() - start < 3)
    		return -1;
    	
    	int pos = -1;
    	
        if (in.charAt(start + 1) == '#') {
            if (in.charAt(start + 2) == 'x' || in.charAt(start + 2) == 'X') {
                if (in.length() - start < 4)
                    return -1;
                
                for (int i = start + 3; i < in.length(); i++) {
                    char c = in.charAt(i);
                    
                    if (c == ';') {
                    	pos = i;
                    	break;
                    }
                    
                    if ((c < '0' || c > '9') && ((c < 'a' || c > 'f') && (c < 'A' || c > 'F')))
                        return -1;
                }
            }
            else {
                for (int i = start + 2; i < in.length(); i++) {
                    char c = in.charAt(i);
                    
                    if (c == ';') {
                    	pos = i;
                    	break;
                    }
                    
                    if (c < '0' || c > '9')
                        return -1;
                }
            }
        }
        else {
            for (int i = start + 1; i < pos; i++) {
                char c = in.charAt(i);
                
                if (c == ';') {
                	pos = i;
                	break;
                }
                
                if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z'))
                    return -1;
            }
        }
        
        parent.appendRaw(in.substring(0, pos));

        return pos;
    }
    
    protected int emitCode(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
    	boolean dub = (token == MarkToken.CODE_DOUBLE);
    	
	    int a = start + (dub ? 2 : 1);
	    int b = this.findToken(in, a, token);
	    
	    if (b < start + 1)
	    	return -1;
	    	
        int pos = b + (dub ? 1 : 0);
        
        parent.add(new XElement("code").withCData(in.substring(a, b - 1)));
        
        return pos;
	}

    protected int emitEm(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
		int b = this.findToken(in, start + 1, token);

		if (b > 0) {
			XElement em = new XElement("em");
			
			parent.add(em);
			
			this.recursiveEmitLine(ctx, em, in.substring(start + 1, b), 0, token);

			return b;
		}

		return -1;
    }

    protected int emitSuper(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
		int b = this.findToken(in, start + 1, token);

		if (b > 0) {
			XElement em = new XElement("sup");
			
			parent.add(em);
			
			this.recursiveEmitLine(ctx, em, in.substring(start + 1, b - 1), 0, token);
			//this.recursiveEmitLine(ctx, em, in.substring(1, in.length() - 2), 0, token);

			return b;
		}

		return -1;
    }

    protected int emitStrong(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
		int b = this.findToken(in, start + 2, token);

		if (b > 0) {
			XElement em = new XElement("strong");
			
			parent.add(em);

			this.recursiveEmitLine(ctx, em, in.substring(start + 2, b), 0, token);
			//this.recursiveEmitLine(ctx, em, in.substring(start + 2, b - 4), 0, token);

			return b + 1;
		}

		return -1;
    }

    protected int emitStrike(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
		int b = this.findToken(in, start + 2, token);

		if (b > 0) {
			XElement em = new XElement("strike");
			
			parent.add(em);

			this.recursiveEmitLine(ctx, em, in.substring(start + 2, b), 0, token);
			//this.recursiveEmitLine(ctx, em, in.substring(2, in.length() - 4), 0, token);

			return b + 1;
		}

		return -1;
    }
    
    /*
     * Recursively scans through the given line, taking care of any markdown
     * stuff.
     */
    protected void recursiveEmitLine(WebContext ctx, XElement parent, String in, int start, MarkToken token) {
        int pos = start;
        int b = 0;
        
        while (pos < in.length()) {
            MarkToken mt = this.getToken(in, pos);
            
            if ((token != MarkToken.NONE)
            		&& (mt == token || token == MarkToken.EM_STAR && mt == MarkToken.STRONG_STAR || token == MarkToken.EM_UNDERSCORE
                            && mt == MarkToken.STRONG_UNDERSCORE))
                return;

            switch(mt) {
            case IMAGE:
            case X_IMAGE:
            case LINK:
                b = this.emitLink(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
                
                break;
            case X_LINK_OPEN:
            	b = 0;
            	
                //b = this.recursiveEmitLine(ctx, parent, in, pos + 2, MarkToken.X_LINK_CLOSE);
                //b = this.emitXLink(ctx, parent, in, pos, mt);
            	/* TODO 
                temp.setLength(0);
                b = this.recursiveEmitLine(temp, in, pos + 2, MarkToken.X_LINK_CLOSE);
                if(b > 0 && this.config.specialLinkEmitter != null)
                {
                    this.config.specialLinkEmitter.emitSpan(out, temp.toString());
                    pos = b + 1;
                }
                else
                {
                    out.append(in.charAt(pos));
                }
                */

                if (b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));

                break;
            case EM_STAR:
            case EM_UNDERSCORE:
                b = this.emitEm(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
                
                break;
            case STRONG_STAR:
            case STRONG_UNDERSCORE:
                b = this.emitStrong(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
                
                break;
            case STRIKE:
                b = this.emitStrike(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));

                break;
            case SUPER:
                b = this.emitSuper(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
                
                break;
            case CODE_SINGLE:
            case CODE_DOUBLE:
                b = this.emitCode(ctx, parent, in, pos, mt);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
            	
                break;
            case HTML:
                b = this.emitHtml(ctx, parent, in, pos);
                
                if(b > 0) 
                    pos = b;
                else 
                    parent.append("&lt;");

                break;
            case ENTITY:
                b = this.emitEntity(ctx, parent, in, pos, mt);
                
                if (b > 0) 
                    pos = b;
                else 
                    parent.append(in.charAt(pos));
                
                break;
            case X_COPY:
            	parent.appendRaw("&copy;");
                pos += 2;
                break;
            case X_REG:
                parent.appendRaw("&reg;");
                pos += 2;
                break;
            case X_TRADE:
                parent.appendRaw("&trade;");
                pos += 3;
                break;
            case X_NDASH:
                parent.appendRaw("&ndash;");
                pos++;
                break;
            case X_MDASH:
                parent.appendRaw("&mdash;");
                pos += 2;
                break;
            case X_HELLIP:
                parent.appendRaw("&hellip;");
                pos += 2;
                break;
            case X_LAQUO:
                parent.appendRaw("&laquo;");
                pos++;
                break;
            case X_RAQUO:
                parent.appendRaw("&raquo;");
                pos++;
                break;
            case X_RDQUO:
                parent.appendRaw("&rdquo;");
                break;
            case X_LDQUO:
                parent.appendRaw("&ldquo;");
                break;
            case ESCAPE:
                pos++;
                //$FALL-THROUGH$
            default:
            	char ch = in.charAt(pos);
            	
            	if (ch != '\n')
            		parent.append(ch);
                
            	break;
            }
            
            pos++;
        }
    }

    /**
     * Turns every whitespace character into a space character.
     * 
     * @param c
     *            Character to check
     * @return 32 is c was a whitespace, c otherwise
     */
    private static char whitespaceToSpace(char c) {
        return Character.isWhitespace(c) ? ' ' : c;
    }

    /**
     * Check if there is any markdown Token.
     * 
     * @param in
     *            Input String.
     * @param pos
     *            Starting position.
     * @return The Token.
     */
    protected MarkToken getToken(String in, int pos) {
        char c0 = pos > 0 ? whitespaceToSpace(in.charAt(pos - 1)) : ' ';
        char c = whitespaceToSpace(in.charAt(pos));
        char c1 = pos + 1 < in.length() ? whitespaceToSpace(in.charAt(pos + 1)) : ' ';
        char c2 = pos + 2 < in.length() ? whitespaceToSpace(in.charAt(pos + 2)) : ' ';
        char c3 = pos + 3 < in.length() ? whitespaceToSpace(in.charAt(pos + 3)) : ' ';

        switch(c)
        {
        case '*':
            if(c1 == '*')
            {
                return c0 != ' ' || c2 != ' ' ? MarkToken.STRONG_STAR : MarkToken.EM_STAR;
            }
            return c0 != ' ' || c1 != ' ' ? MarkToken.EM_STAR : MarkToken.NONE;
        case '_':
            if(c1 == '_')
            {
                return c0 != ' ' || c2 != ' ' ? MarkToken.STRONG_UNDERSCORE : MarkToken.EM_UNDERSCORE;
            }

            return Character.isLetterOrDigit(c0) && c0 != '_' && Character.isLetterOrDigit(c1) ? MarkToken.NONE : MarkToken.EM_UNDERSCORE;
        case '~':
            if(c1 == '~')
            {
                return MarkToken.STRIKE;
            }
            return MarkToken.NONE;
        case '!':
            if((c1 == '!') && (c2 == '['))
                return MarkToken.X_IMAGE;
            if(c1 == '[')
                return MarkToken.IMAGE;
            return MarkToken.NONE;
        case '[':
            if(c1 == '[')
                return MarkToken.X_LINK_OPEN;
            return MarkToken.LINK;
        case ']':
            if(c1 == ']')
                return MarkToken.X_LINK_CLOSE;
            return MarkToken.NONE;
        case '`':
            return c1 == '`' ? MarkToken.CODE_DOUBLE : MarkToken.CODE_SINGLE;
        case '\\':
            switch(c1)
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
                return MarkToken.ESCAPE;
            default:
                return MarkToken.NONE;
            }
        case '<':
            if(c1 == '<')
                return MarkToken.X_LAQUO;
            return MarkToken.HTML;
        case '&':
            return MarkToken.ENTITY;
        case '-':
            if(c1 == '-')
                return c2 == '-' ? MarkToken.X_MDASH : MarkToken.X_NDASH;
            break;
        case '^':
            return c0 == '^' || c1 == '^' ? MarkToken.NONE : MarkToken.SUPER;
        case '>':
            if(c1 == '>')
                return MarkToken.X_RAQUO;
            break;
        case '.':
            if(c1 == '.' && c2 == '.')
                return MarkToken.X_HELLIP;
            break;
        case '(':
            if(c1 == 'C' && c2 == ')')
                return MarkToken.X_COPY;
            if(c1 == 'R' && c2 == ')')
                return MarkToken.X_REG;
            if(c1 == 'T' & c2 == 'M' & c3 == ')')
                return MarkToken.X_TRADE;
            break;
        case '"':
            if(!Character.isLetterOrDigit(c0) && c1 != ' ')
                return MarkToken.X_LDQUO;
            if(c0 != ' ' && !Character.isLetterOrDigit(c1))
                return MarkToken.X_RDQUO;
            break;
        default:
            return MarkToken.NONE;
        }
        
        return MarkToken.NONE;
    }

    protected void emitMarkedLines(WebContext ctx, XElement parent, Line lines) {
        StringBuilder in = new StringBuilder();
        Line line = lines;
        
        while(line != null) {
            if(!line.isEmpty)
                in.append(line.value.substring(line.leading, line.value.length() - line.trailing));
            
            if(line.next != null) 
                in.append("\n<br />");
            
            line = line.next;
        }

        this.recursiveEmitLine(ctx, parent, in.toString(), 0, MarkToken.NONE);
    }

    protected void emitRawLines(WebContext ctx, XElement parent, Line lines) {
        Line line = lines;
        
        if (this.config.safeMode) {
            StringBuilder sb = new StringBuilder();
            
            while (line != null) {
                if(!line.isEmpty)
                    sb.append(line.value);

                sb.append('\n');
                
                line = line.next;
            }
            
            parent.add(new XText(false, sb.toString()));
        }
        else {
    		StringBuilder sb = new StringBuilder();

            while (line != null) {
                if (!line.isEmpty)
                    sb.append(line.value);
                
                sb.append("\n");

                line = line.next;
            }
            
            // TODO need to parse html
            
            parent.add(new XText(true, sb.toString()));
        }
    }

    protected void emitCodeLines(WebContext ctx, XElement parent, Line lines, String meta, boolean removeIndent) {
        Line line = lines;

		if (StringUtil.isNotEmpty(meta))
			parent.setAttribute("class", meta);
		
		StringBuilder sb = new StringBuilder();

        while (line != null) {
            if (!line.isEmpty)
                sb.append(removeIndent ? line.value.substring(4) : line.value);
            
            sb.append("\n");
            
            line = line.next;
        }
        
        parent.add(new XText(true, sb.toString()));
    }
    
    /*
     * interprets a plugin block into the StringBuilder.
     */
    protected void emitPluginLines(WebContext ctx, XElement parent, Line lines, String meta) {
		String idPlugin = meta;		
		String sparams = null;
		Map<String, String> params = null;
		int iow = meta.indexOf(' '); 
		
		if (iow != -1) {
			idPlugin = meta.substring(0, iow);
			sparams = meta.substring(iow+1);
			
			if(sparams != null) 
				params = parsePluginParams(sparams);
		}
		
		if (params == null) 
			params = new HashMap<String, String>();
		
        ArrayList<String> list = new ArrayList<String>();
        
        Line line = lines;
        
        while (line != null) {
            if (line.isEmpty)
                list.add("");
            else
                list.add(line.value);
            
            line = line.next;
        }

		Plugin plugin = plugins.get(idPlugin);
		
		if(plugin != null) 
			plugin.emit(ctx, parent, list, params);
    }
    
	protected Map<String, String> parsePluginParams(String s) {
		Map<String, String> params = new HashMap<String, String>();
	     Pattern p = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");

	     Matcher m = p.matcher(s);

	     while(m.find()){
	    	 params.put(m.group(1), m.group(2));
	     }	     
	     
	     return params;
	}
    
}
