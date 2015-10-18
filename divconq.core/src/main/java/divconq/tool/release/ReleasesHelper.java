package divconq.tool.release;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import divconq.lang.op.FuncResult;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.IOUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ReleasesHelper {
	protected List<XElement> rellist = null;
	protected RecordStruct reldata = null;
	protected Path cspath = null;
	
	public List<String> names() {
		List<String> names = new ArrayList<String>();
		
		for (int i = 0; i < rellist.size(); i++)
			names.add(rellist.get(i).getAttribute("Name"));
		
		return names;
	}
	
	public void saveData() {
		IOUtil.saveEntireFile2(cspath, this.reldata.toPrettyString());
	}

	public XElement get(int i) {
		return this.rellist.get(i);
	}
	
	public XElement get(String name) {
		for (int i = 0; i < rellist.size(); i++)
			if (rellist.get(i).getAttribute("Name").equals(name))
				return rellist.get(i);
		
		return null;
	}
	
	public RecordStruct getData(String name) {
		return this.reldata.getFieldAsRecord(name);
	}
	
	public boolean init(Path relpath) {
		if (relpath == null) {
			System.out.println("Release path not defined");
			return false;
		}
			
		FuncResult<XElement> xres = XmlReader.loadFile(relpath.resolve("release.xml"), false);
		
		if (xres.hasErrors()) {
			System.out.println("Release settings file is not present or has bad xml structure");
			return false;
		}
		
		this.rellist = xres.getResult().selectAll("Release");
		
		this.cspath = relpath.resolve("release-data.json");

		if (Files.exists(cspath)) {
			FuncResult<CharSequence> res = IOUtil.readEntireFile(cspath);
			
			if (res.isEmptyResult()) {
				System.out.println("Release data unreadable");
				return false;
			}
			
			this.reldata = Struct.objectToRecord(res.getResult());
		}
		
		return true;
	}		
}