package divconq.locale;

import divconq.hub.Hub;
import divconq.lang.op.OperationContext;

public class Tr {
	static public LocaleDefinition getLocale() {
		OperationContext ctx = OperationContext.get();
		
		if (ctx != null)
			return ctx.getWorkingLocaleDefinition();
		
		return Hub.instance.getResources().getDefaultLocaleDefinition();
	}
	
	static public String tr(long code, Object... params) {
		return Tr.tr("_code_" + code, params);
	}
	
	static public String tr(String token, Object... params) {
		OperationContext ctx = OperationContext.get();
		
		if (ctx != null)
			return ctx.tr(token, params);
		
		ILocaleResource tr = Hub.instance.getResources();
		LocaleDefinition def = tr.getDefaultLocaleDefinition();
	
		return tr.getDictionary().tr(tr, def, token, params);
	}
	
	static public String trp(long pluralcode, long singularcode, Object... params) {
		return Tr.tr("_code_" + pluralcode, "_code_" + singularcode, params);
	}
		
	static public String trp(String pluraltoken, String singulartoken, Object... params) {
		OperationContext ctx = OperationContext.get();
		
		if (ctx != null)
			return ctx.trp(pluraltoken, singulartoken, params);
		
		ILocaleResource tr = Hub.instance.getResources();
		LocaleDefinition def = tr.getDefaultLocaleDefinition();
	
		return tr.getDictionary().trp(tr, def, pluraltoken, singulartoken, params);
	}
}
