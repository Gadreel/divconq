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
package divconq.locale;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import divconq.lang.op.FuncResult;
import divconq.log.Logger;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

// TODO code so that we work with Alpha 2, 3 or 4 codes automatically
public class Dictionary {
	protected Dictionary parent = null;
	protected Map<String,Translation> translations = new HashMap<String,Translation>();

	// caching
	protected Map<String,TranslationChain> translationscache = new HashMap<String,TranslationChain>();
	
	public void setParent(Dictionary v) {
		this.parent = v;
	}
	
	public Dictionary() {
	}	
	
	public Collection<Translation> getTranslations() {
		return this.translations.values();
	}
	
	public String findToken(ILocaleResource resource, LocaleDefinition locale, String token) {
		if (StringUtil.isEmpty(token))
			return null;
		
		TranslationChain tc = this.getTranslationChain(locale);
		
		if (tc != null) {
			String fnd = tc.findToken(token);
			
			if (fnd != null)
				return fnd;
		}
		
		while (resource != null) {
			tc = this.getTranslationChain(resource.getDefaultLocaleDefinition());
			
			if (tc != null) {
				String fnd = tc.findToken(token);
				
				if (fnd != null)
					return fnd;
			}
			
			resource = resource.getParentLocaleResource();
		}
		
		return null;
	}
	
	public String tr(ILocaleResource resource, LocaleDefinition locale, String token, Object... params) {
		String val = this.findToken(resource, locale, token);
		
    	if (StringUtil.isEmpty(val))
    		return null;
    	
        // the expansion of variables is per Attribute Value Templates in XSLT
        // http://www.w3.org/TR/xslt#attribute-value-templates

        StringBuilder sb = new StringBuilder();

        int lpos = 0;
        int bpos = val.indexOf("{$");

        while (bpos != -1) {
            int epos = val.indexOf("}", bpos);
            if (epos == -1) 
            	break;

            sb.append(val.substring(lpos, bpos));

            lpos = epos + 1;

            String varname = val.substring(bpos + 2, epos).trim();

            // TODO add some formatting features for numbers/datetimes
            
            Long parampos = StringUtil.parseInt(varname);
            
            if ((parampos != null) && (parampos <= params.length)) {
            	if (params[parampos.intValue() - 1] != null)
            		sb.append(params[parampos.intValue() -1].toString());
            }
            else 
                sb.append(val.substring(bpos, epos + 1));

            bpos = val.indexOf("{$", epos);
        }

        sb.append(val.substring(lpos));
		
		return sb.toString();
	}
	
	public String trp(ILocaleResource resource, LocaleDefinition locale, String pluraltoken, String singulartoken, Object... params) {
		if ((params.length > 0) && (params[0] instanceof Number) && (((Number)params[0]).intValue() == 1))
			return this.tr(resource, locale, singulartoken, params);
		
		return this.tr(resource, locale, pluraltoken, params);
	}
	
	public TranslationChain getTranslationChain(LocaleDefinition locale) {
		if (locale == null)
			return null;
		
		TranslationChain tc = this.translationscache.get(locale.getName());
		
		if (tc == null) {
			TranslationChain parent = (this.parent != null) ? this.parent.getTranslationChain(locale) : null;
			
			// we want language check above variant in chain
			if (locale.hasVariant()) {
				Translation tr = this.translations.get(locale.getLanguage());
				
				if (tr != null) {
					TranslationChain lang = new TranslationChain(parent, tr);
					parent = lang;
				}
			}
			
			Translation tr = this.translations.get(locale.getName());

			if (tr != null) 
				tc = new TranslationChain(parent, tr);
			else
				tc = parent;
			
			this.translationscache.put(locale.getName(), tc);
		}
		
		return tc;
	}
	
	public void load(Path fl) {
		if (fl == null) {
			// do not use Tr because dictionary may not be loaded
			Logger.error("Unable to apply dictionary file, file null", "Code", "106");
			return;
		}
		
		if (!Files.exists(fl)) {
			// do not use Tr because dictionary may not be loaded
			Logger.error("Missing dictionary file, expected: " + fl.normalize(), "Code", "107");
			return;
		}
		
		FuncResult<XElement> xres3 = XmlReader.loadFile(fl, false);
		
		if (xres3.hasErrors()) 
			return;
		
		this.load(xres3.getResult());
	}
	
	protected Translation getOrAddLocale(String name) {
		Translation t = this.translations.get(name);
		
		if (t == null) {
			t = new Translation(name);
			this.translations.put(name, t);
		}
		
		return t;
	}
	
	public void load(XElement trroot) {
		if (trroot == null) {
			// do not use Tr because dictionary may not be loaded
			Logger.error("Unable to apply dictionary file, missing xml", "Code", "105");
			return;
		}
		
		for (XElement lel : trroot.selectAll("Locale")) {
			String lname = lel.getAttribute("Id");
			
			if (StringUtil.isEmpty(lname))
				continue;
			
			Translation t = this.getOrAddLocale(lname);
			
			for (XElement tel : lel.selectAll("Entry")) {
				String tname = tel.getAttribute("Token");
				
				if (StringUtil.isEmpty(tname))
					continue;
				
				String v = tel.getAttribute("Value");
				
				if (StringUtil.isEmpty(v))
					v = tel.getText();
				
				if (StringUtil.isEmpty(v))
					continue;
				
				t.put(tname, v);
			}
		}
	}
}
