package divconq.cms.feed;

public class CollectContext {
	protected boolean forIndex = false;
	protected boolean forSitemap = false;

	public boolean isForIndex() {
		return this.forIndex;
	}

	public boolean isForSitemap() {
		return this.forSitemap;
	}
	
	public CollectContext forIndex() {
		this.forIndex = true;
		return this;
	}

	public CollectContext forSitemap() {
		this.forSitemap = true;
		return this;
	}
}
