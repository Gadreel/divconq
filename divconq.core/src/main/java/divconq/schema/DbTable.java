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
package divconq.schema;

import java.util.HashMap;
import java.util.Map;

import divconq.util.StringUtil;
import divconq.xml.XElement;

public class DbTable {
	public String name = null;
	public Map<String, DbField> fields = new HashMap<String, DbField>();

	public String getName() {
		return this.name;
	}
	
	public void addField(XElement fel, DataType table) {
		String name = fel.getAttribute("Name");
		
		if (StringUtil.isEmpty(name))
			return;
		
		DbField fld = this.fields.get(name);
		
		if (fld == null) {
			fld = new DbField();
			fld.name = name;
			this.fields.put(name, fld);
		}
		
		if (fel.hasAttribute("Required")) 
			fld.required = "true".equals(fel.getAttribute("Required").toLowerCase());
		
		if (fel.hasAttribute("Unique")) 
			fld.unique = "true".equals(fel.getAttribute("Unique").toLowerCase());
		
		if (fel.hasAttribute("Indexed")) 
			fld.indexed = "true".equals(fel.getAttribute("Indexed").toLowerCase());
		
		if (fel.hasAttribute("Dynamic")) 
			fld.dynamic = "true".equals(fel.getAttribute("Dynamic").toLowerCase());
		
		if (fel.hasAttribute("List")) 
			fld.list = "true".equals(fel.getAttribute("List").toLowerCase());
		
		if (fel.hasAttribute("Type")) 
			fld.typeid = fel.getAttribute("Type");
		
		if (fel.hasAttribute("ForeignKey")) 
			fld.fkey = fel.getAttribute("ForeignKey");
	}

	public void compile(SchemaManager man) {
		DataType tbtype = man.getType(this.name);
		
		if (tbtype == null)
			return;
		
		for (DbField fld : this.fields.values()) {
			if (StringUtil.isNotEmpty(fld.fkey)) { 
				fld.type = "Id";
				fld.typeid = "Id";
				fld.indexed = true;
				//fld.unique = true;
			}
			else {
				fld.fkey = null;
				
				Field tfld = tbtype.getField(fld.name);
	
				if (tfld != null) {
					DataType dtype = tfld.getPrimaryType();
					
					if (dtype != null) {
						fld.typeid = dtype.getId();
						fld.type = fld.typeid;
						
						if (!"Json".equals(fld.typeid) && !"Xml".equals(fld.typeid) && !"Time".equals(fld.typeid) && !"DateTime".equals(fld.typeid) && !"Date".equals(fld.typeid)
								&& !"BigString".equals(fld.typeid) && !"String".equals(fld.typeid) && !"Id".equals(fld.typeid) && !"Integer".equals(fld.typeid)
								 && !"Decimal".equals(fld.typeid) && !"BigDecimal".equals(fld.typeid) && !"BigInteger".equals(fld.typeid) && !"UtcDateTime".equals(fld.typeid)
								 && !"Number".equals(fld.typeid) && !"Boolean".equals(fld.typeid) && !"Binary".equals(fld.typeid) && !"BigDateTime".equals(fld.typeid))
						{				
							CoreType ctype = dtype.getCoreType();
						
							if (ctype != null)
								fld.type = ctype.getType().toString();
						}
	
						// these types cannot index (even other types are truncated to 64 chars for indexing, though value can be larger)
						if ("Json".equals(fld.typeid) || "Xml".equals(fld.typeid) || "BigString".equals(fld.typeid) || "Binary".equals(fld.typeid)) {
							fld.indexed = false;
							fld.unique = false;
						}
					}
				}
				else {
					fld.type = "String";
				}
			}
		}
	}

	protected DbField getField(String name2) {
		return this.fields.get(name2);
	}

}
