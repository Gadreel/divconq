package divconq.locale;

public interface ILocaleResource {
	ILocaleResource getParentLocaleResource();
	int rateLocale(String locale);
	LocaleDefinition getLocaleDefinition(String locale);
	String getDefaultLocale();
	LocaleDefinition getDefaultLocaleDefinition();
	Dictionary getDictionary();
}
