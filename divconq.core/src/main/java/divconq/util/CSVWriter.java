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

/**
 * @author Thomas Davis (sunsetbrew)
 * @copyright Copyright (c) 2010, Thomas Davis
 * @license http://opensource.org/licenses/mit-license.php MIT License
 */
package divconq.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import divconq.lang.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * CSV4180Writer provides a simple way to export CSV values to a file or
 * console. The writer extends BufferedWriter for improved performance.
 */
public class CSVWriter extends BufferedWriter {
	static public OperationResult writeRecords(OutputStream out, ListStruct records, String... fields) {
		OperationResult or = new OperationResult();
		
		try {
			CSVWriter wrt = new CSVWriter(new OutputStreamWriter(out));
			
			for (String fld : fields) {
				wrt.writeField(fld);
			}
			
			wrt.newLine();
			
			for (Struct s : records.getItems()) {
				RecordStruct rec = (RecordStruct) s;

				for (String fld : fields) 
					wrt.writeField(Struct.objectToString(rec.getField(fld)));
				
				wrt.newLine();
			};
			
			wrt.close();
		}
		catch (Exception x) {
			or.error("Unable to write CSV: " + x);
		}
		
		return or;
	}

	/**
	 * Create a buffered character-output stream that uses a default-sized
	 * output buffer.
	 * 
	 * @param out
	 *            A writer
	 */
	public CSVWriter(Writer out) {
		super(out);
	}

	/**
	 * Create a new buffered character-output stream that uses an output buffer
	 * of the given size.
	 * 
	 * @param out
	 *            A writer
	 * @param sz
	 *            Output-buffer size, a positive integer
	 */
	public CSVWriter(Writer out, int sz) {
		super(out, sz);
	}

	/**
	 * Writes a line of fields to the writer. A new line is automatically added
	 * if beyond the first call to this method. This is a convenience method.
	 * 
	 * @param fields
	 *            fields to output
	 * @throws IOException
	 *             on general I/O error
	 */
	public void writeFields(ArrayList<String> fields) throws IOException {

		if (newWriter) {
			this.newWriter = false;
		} else {
			this.newLine();
		}
		Iterator<String> si = fields.iterator();
		while (si.hasNext()) {
			this.writeField(si.next());
		}
	}

	/**
	 * Writes a line of fields to the writer. A new line is automatically added
	 * if beyond the first call to this method. This is a convenience method.
	 * 
	 * @param fields
	 *            fields to output
	 * @throws IOException
	 *             on general I/O error
	 */
	public void writeFields(String[] fields) throws IOException {

		if (newWriter) {
			this.newWriter = false;
		} else {
			this.newLine();
		}
		for (int i = 0; i < fields.length; i++) {
			this.writeField(fields[i]);
		}
	}

	/**
	 * Write a line separator. 
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 * @see java.io.BufferedWriter#newLine()
	 */
	@Override
	public void newLine() throws IOException {
		this.newLine = true;
		super.write("\r\n");
	}

	/**
	 * Write a field to the output quoting as necessary and adding comma
	 * separators between fields.
	 * 
	 * @param field
	 *            String to write
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public void writeField(String field) throws IOException {

		if (this.newLine) {
			this.newLine = false;
		} else {
			this.write(',');
		}

		// case 0: empty string, simple :)
		if ((field == null) || (field.length() == 0)) {
			return;
		}

		// case 1: field has quotes in it, if so convert to, quote field and
		// double all quotes
		Matcher matcher = escapePattern.matcher(field);
		if (matcher.find()) {
			this.write('"');
			this.tmpBuffer.setLength(0);
			do {
				matcher.appendReplacement(this.tmpBuffer, "\"\"");
			} while (matcher.find());
			matcher.appendTail(this.tmpBuffer);
			this.write(this.tmpBuffer.toString());
			this.write('"');
			return;
		}

		// case 2: field has a comma, carriage return or new line in it, if so
		// quote field and double all quotes
		matcher = specialCharsPattern.matcher(field);
		if (matcher.find()) {
			this.write('"');
			this.write(field);
			this.write('"');
			return;
		}

		// case 3: safe string to just add
		this.append(field);
	}

	/** @ignore */
	private boolean newLine = true;

	/** @ignore */
	private final StringBuffer tmpBuffer = new StringBuffer();

	/** @ignore */
	private static Pattern escapePattern = Pattern.compile("(\")");

	/** @ignore */
	private static Pattern specialCharsPattern = Pattern.compile("[,\r\n]");

	/** @ignore */
	private boolean newWriter = true;

}