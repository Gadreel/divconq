package divconq.locale;

public class TranslationChain {
	protected TranslationChain parent = null;
	protected Translation tr = null;

	public TranslationChain getParent() {
		return this.parent;
	}

	public Translation getTranslation() {
		return this.tr;
	}
	
	public TranslationChain(TranslationChain parent, Translation tr) {
		this.parent = parent;
		this.tr = tr;
	}

	public String findToken(String token) {
		if (this.tr != null) {
			String fnd = this.tr.get(token);
			
			if (fnd != null)
				return fnd;
		}
		
		if (this.parent != null)
			return this.parent.findToken(token);
		
		return null;
	}
}
