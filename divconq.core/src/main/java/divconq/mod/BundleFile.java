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
package divconq.mod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

public class BundleFile implements JavaFileObject {
	protected String name = null;
	protected byte[] content = null;

	public BundleFile(String name, byte[] content) {
		if (name.startsWith("/"))
			name = name.substring(1).replace('/', '.');
		
		this.name = name;
		this.content = content;
	}

	@Override
	public URI toUri() {
		return null;  //new URI(this.name);
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Writer openWriter() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	@Override
	public boolean delete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Kind getKind() {
		return Kind.CLASS;
	}

	@Override
	// copied from SImpleJavaFileManager
	public boolean isNameCompatible(String simpleName, Kind kind) {
		String baseName = simpleName + kind.extension;
		
		return kind.equals(getKind()) && (baseName.equals(getName()) || getName().endsWith("/" + baseName));
	}

	@Override
	public NestingKind getNestingKind() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Modifier getAccessLevel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return this.name;
	}
}
