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

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

// TODO code so that we work with Alpha 2, 3 or 4 codes automatically
public class Localization {
	protected String defaultLocale = "en";  
	protected Map<String,LocaleInfo> locales = new HashMap<String,LocaleInfo>();

	public Localization() {
		// TODO configure
		this.defaultLocale = Locale.getDefault().getLanguage();  
	}	
	
	public Collection<LocaleInfo> getLocales() {
		return this.locales.values();
	}
	
	// TODO this should look in TaskContext too
	public LocaleInfo getLocalization(String locale) {
		if (StringUtil.isEmpty(locale))
			return this.locales.get(this.defaultLocale);
		
		LocaleInfo tr = this.locales.get(locale);
		
		if ((tr == null) && (locale.indexOf("-") > -1)) {
			locale = locale.substring(0, locale.indexOf("-"));
			tr = this.locales.get(locale);
		}
		
		if (tr == null) 
			tr = this.locales.get(this.defaultLocale);
		
		return tr;
	}
	
	public String getLocalizedToken(String locale, String token) {
		if (StringUtil.isEmpty(locale))
			return this.locales.get(this.defaultLocale).get(token);
		
		LocaleInfo tr = this.locales.get(locale);
		
		if ((tr != null) && tr.has(token))
			return tr.get(token);
		
		if (locale.indexOf("-") > -1) {
			locale = locale.substring(0, locale.indexOf("-"));
			tr = this.locales.get(locale);
			
			if ((tr != null) && tr.has(token))
				return tr.get(token);
		}
		
		return this.locales.get(this.defaultLocale).get(token);
	}
	
	public String tr(String locale, String token, Object... params) {
		if (StringUtil.isEmpty(locale) || StringUtil.isEmpty(token))
			return null;
		
		String val = this.getLocalizedToken(locale, token);
		
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
	
	public String trp(String locale, String pluraltoken, String singulartoken, Object... params) {
		if ((params.length > 0) && (params[0] instanceof Number) && (((Number)params[0]).intValue() == 1))
			return this.tr(locale, singulartoken, params);
		
		return this.tr(locale, pluraltoken, params);
	}
	
	public String getTrValue(String locale, String token) {
		if (StringUtil.isEmpty(locale) || StringUtil.isEmpty(token))
			return null;
		
		return this.getLocalizedToken(locale, token);
	}
	
	public void init(XElement trroot) {
		// TODO config
	}
	
	public void load(OperationResult or, File fl) {
		if (fl == null) {
			or.error(106, "Unable to apply dictionary file, file null");
			return;
		}
		
		if (!fl.exists()) {
			or.error(107, "Missing dictionary file, expected: " + fl.getAbsolutePath());
			return;
		}
		
		FuncResult<XElement> xres3 = XmlReader.loadFile(fl, false);
		
		if (xres3.hasErrors()) {
			or.copyMessages(xres3);
			return;
		}
		
		this.load(or, xres3.getResult());
	}
	
	public void load(OperationResult or, InputStream fl) {
		if (fl == null) {
			or.error(106, "Unable to apply dictionary file, file null");
			return;
		}
		
		FuncResult<XElement> xres3 = XmlReader.parse(fl, false);
		
		if (xres3.hasErrors()) {
			or.copyMessages(xres3);
			return;
		}
		
		this.load(or, xres3.getResult());
	}
	
	public LocaleInfo getOrAddLocale(String name) {
		LocaleInfo t = this.locales.get(name);
		
		if (t == null) {
			t = new LocaleInfo(name);
			this.locales.put(name, t);
		}
		
		return t;
	}
	
	public void load(OperationResult or, XElement trroot) {
		if (trroot == null) {
			or.error(105, "Unable to apply dictionary file, missing xml");
			return;
		}
		
		for (XElement lel : trroot.selectAll("Locale")) {
			String lname = lel.getAttribute("Id");
			
			if (StringUtil.isEmpty(lname))
				continue;
			
			LocaleInfo t = this.getOrAddLocale(lname);
			
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

	public String getDefault() {
		return this.defaultLocale;
	}
}
