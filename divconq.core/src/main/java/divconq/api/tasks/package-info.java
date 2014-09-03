/**
 * This package contains classes for common API Tasks.
 * 
 * 
 * TODO
 * 
 * @see ncc.uploader.Main Class that processes Uploader Web service requests. 
 * @see ncc.uploader.AuthService Class that processes admin login requests. 
 * 
 * 
 * How uploads work:
 * 
 * 1) Request an upload via the DataChannel of the current session
 * 
 * 		Message msg = new Message("Session", "DataChannel", "Establish", new RecordStruct(
 *				new FieldStruct("Title", title),
 *				new FieldStruct("Mode", mode),
 *				new FieldStruct("StreamRequest", streamRequest)
 *		));
 *
 *	attach to that a request (StreamRequest) that will be sent to a service.  If using the recommended Service structure
 *  this is what that attachment would look like:
 *  
 *		RecordStruct rec = new RecordStruct();
 *		rec.setField("FilePath", optional - File Name or Common Path);		
 *		rec.setField("FileToken", optional - a token that represents a file, or path);
 *		rec.setField("FileSize", optional - expected size of the upload);
 *		
 *		Message streamRequest = new Message(Name of File Store Service, "FileStore", "StartUpload", rec);
 *  
 *  You do not have to use the recommended structure, any Message is allowed for StreamRequest but the service must 
 *  respond with correct values.
 * 
 * 
 * 
 */
package divconq.api.tasks;
