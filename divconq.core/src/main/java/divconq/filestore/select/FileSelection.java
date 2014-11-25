package divconq.filestore.select;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import divconq.ctp.CtpConstants;
import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreFile;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

/*
	Select: {
		Mode: "[Detail]|Listing|Expanded",
		RelativeTo: "the base path for all Files Sets, defaults to /",
		// Select contains either File (fields below) or File Sets, not both - File checked first
		Path: "path relative to RelativeTo",
		Recursion: N [1] - where 0 means none, 1 means 1 level, etc,     
		Rename: "optional, new name, used during MOVE, name only no path",
		Offset: "optional, bytes from start, used during READ for resume download",
		// if no file sets listed then match every thing under RelativeTo
		FileSets: [		// each file set is independent and cumulative - list within a list
			[
				{
					Type: "File",			// select specific files/folders
					Path: "path relative to RelativeTo",
					Recursion: N [1] - where 0 means none, 1 means 1 level, etc,     
					Rename: "optional, new name, used during MOVE, name only no path",
					Offset: "optional, bytes from start, used during READ for resume download",
					Not: t/[f]
				},
				{
					Type: "NameFilter",
					Pattern: "reg ex for name",
					Not: t/[f]
				},
				{
					Type: "PathFilter",
					Pattern: "reg ex for path relative to RelativeTo",
					Not: t/[f]
				},
				{
					Type: "SizeFilter",							// conditions may be combined
					Equal: N, 
					LessThan: N, 
					GreaterThan: N, 
					LessThanOrEqual: N, 
					GreaterThanOrEqual: N,
					Not: t/[f]
				},
				{
					Type: "ModifiedFilter",						// if time is left off then matches on DATE only
					Equal: "ISO UTC DATE/TIME", 
					LessThan: "ISO UTC DATE", 
					GreaterThan: "ISO UTC DATE", 
					LessThanOrEqual: "ISO UTC DATE", 
					GreaterThanOrEqual: "ISO UTC DATE",
					Not: t/[f]
				}
			]
		],
		Sort: {
			// Match gets value from first RegEx pattern in filter
			Type: "[Name]|Path|Modified|Size|Match|Value",		// if any sort is defined but not Type then Name is default
			Direction: "Asc|Desc",
			SortAs: "Number|[String]",							// used with Match
			Value: "mix in %attrib% with text"					// used with Value
		},
		Attributes: [
			10, 20
		]
	}
}
 * 
 */
public class FileSelection {
	protected CommonPath relativeTo = CommonPath.ROOT;
	protected List<List<FileMatcher>> sets = new ArrayList<>();
	protected List<Integer> attributes = new ArrayList<>();
	
	protected FileSortType sortType = FileSortType.Name; 
	protected FileSortDirection sortDirection = FileSortDirection.Ascending; 
	protected FileSortAs sortAs = FileSortAs.String; 
	protected String sortValueTemplate = null;
	
	protected FileSelectionMode mode = FileSelectionMode.Detail;
	
	public FileSelection withRelativeTo(CommonPath v) {
		this.relativeTo = v;		
		return this;
	}
	
	public FileSelection withRelativeTo(String v) {
		this.relativeTo = new CommonPath(v);		
		return this;
	}
	
	public CommonPath relativeTo() {
		return this.relativeTo;
	}
	
	public FileSelection withFileSet(FileMatcher... v) {
		List<FileMatcher> set = new ArrayList<>();
		
		for (FileMatcher m : v)
			set.add(m);
			
		this.sets.add(set);	
		return this;
	}
	
	public FileSelection withMode(FileSelectionMode v) {
		this.mode = v;
		return this;
	}
	
	public FileSelection withFileSet(CommonPath v) {
		this.withFileSet(new FileMatcherFile().withPath(v));	
		return this;
	}
	
	public FileSelection withFileSet(CommonPath path, int recursion) {
		this.withFileSet(new FileMatcherFile().withPath(path).withRecursion(recursion));	
		return this;
	}
	
	public FileSelection withFileSet(String path) {
		this.withFileSet(new FileMatcherFile().withPath(new CommonPath(path)));	
		return this;
	}
	
	public FileSelection withFileSet(String path, int recursion) {
		this.withFileSet(new FileMatcherFile().withPath(new CommonPath(path)).withRecursion(recursion));	
		return this;
	}
	
	// see Ctp.CTP_F_ATTR_*
	public boolean hasAttr(int attr) {
		return this.attributes.contains(attr);
	}
	
	public FileSelection withAttrs(Integer... attrs) {
		for (Integer attr : attrs)
			this.attributes.add(attr);
		
		return this;
	}
	
	public FileSelection withSize() {
		this.attributes.add(CtpConstants.CTP_F_ATTR_SIZE);		
		return this;
	}
	
	public FileSelection withModified() {
		this.attributes.add(CtpConstants.CTP_F_ATTR_MODTIME);		
		return this;
	}
	
	public FileSelection withPermissions() {
		this.attributes.add(CtpConstants.CTP_F_ATTR_PERMISSIONS);		
		return this;
	}
	
	public boolean hasContent() {
		return this.attributes.contains(CtpConstants.CTP_F_ATTR_DATA);
	}
	
	public FileSelection withContent() {
		this.attributes.add(CtpConstants.CTP_F_ATTR_DATA);		
		return this;
	}

	public FileSelectionMode getMode() {
		return this.mode;
	}
	
	public void setMode(FileSelectionMode v) {
		this.mode = v;
	}

	// value shall be the first value matched by the approving Set
	public boolean approve(IFileStoreFile file, AtomicReference<String> value) {
		for (List<FileMatcher> set : this.sets) {
			boolean setApproved = true;
			
			for (FileMatcher m : set) {
				if (!m.approve(file, value, this)) {
					setApproved = false;
					break;
				}
			}
			
			if (setApproved)
				return true;
			
			value.set(null);					// reset, only accept a value from approving set
		}
		
		return false;
	}

	// only FileMatcherFile can be a source of file lists, find them
	public List<FileMatcherFile> searchList() {
		this.init();
		
		List<FileMatcherFile> list = new ArrayList<>();
		
		for (List<FileMatcher> set : this.sets) {
			for (FileMatcher m : set) {
				if (m instanceof FileMatcherFile) {
					FileMatcherFile mf = (FileMatcherFile) m;

					// if it is an exclude ignore it, really it is a filter with the NOT
					if (!mf.exclude)
						list.add(mf);
				}
			}
		}
		
		return list;
	}
	
	public void init() {
		for (List<FileMatcher> set : this.sets) {
			for (FileMatcher m : set) {
				if (m instanceof FileMatcherFile) {
					FileMatcherFile mf = (FileMatcherFile) m;
					mf.expandedPath = this.relativeTo.resolve(mf.path);
				}
			}
		}
	}

	/*
	{
		Mode: "[Detail]|Listing|Download",
		RelativeTo: "the base path for all Files Sets, defaults to /",
		// Select contains either File (fields below) or File Sets, not both - File checked first
		Path: "path relative to RelativeTo",
		Rename: "optional, new name, used during MOVE, name only no path",
		Offset: "optional, bytes from start, used during READ for resume download",
	}
	 * 
	 */
	public FileSelection withInstructions(RecordStruct inst) {
		this.mode = FileSelectionMode.valueOf(inst.getFieldAsString("Mode"));
		
		if (!inst.isFieldEmpty("RelativeTo"))
			this.relativeTo = new CommonPath(inst.getFieldAsString("RelativeTo"));
		
		if (!inst.isFieldEmpty("Path")) {
			FileMatcherFile m = new FileMatcherFile()
				.withPath(new CommonPath(inst.getFieldAsString("Path")));

			if (!inst.isFieldEmpty("Rename"))
				m.withRename(inst.getFieldAsString("Rename"));

			if (!inst.isFieldEmpty("Offset"))
				m.withOffset(inst.getFieldAsInteger("Offset"));
			
			this.withFileSet(m);	
		}
		
		if (!inst.isFieldEmpty("Attributes"))
			inst.getFieldAsList("Attributes").integerStream()
				.forEach(num -> this.attributes.add(num.intValue()));
		
		return this;
	}

	public RecordStruct toInstructions() {
		RecordStruct inst = new RecordStruct();
		
		inst.setField("Mode", this.mode.toString());
		inst.setField("RelativeTo", this.relativeTo.toString());
		
		if (this.attributes.size() > 0) 
			inst.setField("Attributes", new ListStruct(this.attributes));
		
		if (this.sets.size() == 0)
			return inst;

		// TODO get more advanced than this in checking filters/options
		
		List<FileMatcher> set = this.sets.get(0);
		
		if (set.size() == 0)
			return inst;
		
		FileMatcher fm = set.get(0);
		
		if (!(fm instanceof FileMatcherFile))
			return inst;
		
		FileMatcherFile fmf = (FileMatcherFile) fm;
		
		inst.setField("Path", fmf.path.toString());
		
		if (StringUtil.isNotEmpty(fmf.newname))
			inst.setField("Rename", fmf.newname);
		
		if (fmf.offset > 0)
			inst.setField("Offset", fmf.offset);
		
		return inst;
	}
}
