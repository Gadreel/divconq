package divconq.hub;

import divconq.xml.XElement;

public class HubPackage {
	protected XElement xml = null;
	protected String name = null;
	
	public String getName() {
		return this.name;
	}
	
	public XElement getXml() {
		return this.xml;
	}
	
	public HubPackage(XElement pack) {
		this.xml = pack;
		this.name =  pack.getAttribute("Name");
	}
}
