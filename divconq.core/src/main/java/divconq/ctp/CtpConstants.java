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
package divconq.ctp;

public class CtpConstants {
    // ======================================================
	// CTP Common to All
    // ======================================================
	
	// all commands for all protocols are 1 bytes
	
	// "relay" messages
    public static final int CTP_CMD_RELAY = 0x00;					// send message and expect no response - full duplex mode
    public static final int CTP_CMD_ALIVE = 0x01;    				// keep alive - special case of RELAY
    public static final int CTP_CMD_STATE = 0x02;    				// for dcBus only - bus status from senders end - both sides send status instead of keep alive - otherwise treated like Alive - special case of RELAY
    public static final int CTP_CMD_EXIT = 0x03;    				// stop this connection with no further action  - special case of RELAY
    public static final int CTP_CMD_EXIT_SIGN_OUT = 0x04;			// only valid for client to send - special case of RELAY    	
    
    // "relay" response messages
    public static final int CTP_CMD_PROGRESS = 0x08;    			// progress message for use with SEND/RESPONSE or other uses - special case of RELAY 
    
    // "request/response" messages
    public static final int CTP_CMD_REQUEST = 0x10;					// send message and expect a RESPONSE - non full duplex mode
    public static final int CTP_CMD_ENGAGE = 0x11;					// tell the connection what you want it to do and how to Auth - EXPECT a RESPONSE
																	// "Prot" = "Message Bus" for dcBus trusted only
																	// "Destination" = "Message Xchange" for dcBus trusted only

    // "request/response" response messages 
    public static final int CTP_CMD_RESPONSE = 0x18;				// generic response to SEND (5) - non full duplex mode
	
    // ======================================================
	// CTP Message Bus for dcBus only
    // ======================================================
    
    // CTP_CMD_STATE - bus status from senders end - both sides send status instead of keep alive 
    // CTP_CMD_RELAY - send message async with a reply tag
    // CTP_CMD_RELAY - send forget message and no reply requested or accepted 
    
    // ======================================================
	// CTP Startup - external client has connected
    // ======================================================
    
    // CTP_CMD_ENGAGE - Requested Version, Host Name, Agent, JSON Mode - return features
	// Creds, Keys, or SessionId + Secret Key (JSON)
	// Name of Protocol and Service you want  
    
	
    // ======================================================
	// CTP Message Xchange
    // ======================================================
    // CTP_CMD_SEND - public static final int CTP_M_CMD_INIT = 10;				// Requested Version, return features
    // CTP_CMD_SEND - public static final int CTP_M_CMD_SETTING = 11;				// set settings , log level
	// CTP_CMD_SEND - public static final int CTP_M_CMD_START = 11;			
	    
	// CTP_CMD_RELAY - public static final int CTP_M_CMD_SEND = 30;				// send message async with a reply tag 
	// CTP_CMD_RELAY - public static final int CTP_M_CMD_SEND_FORGET = 31;			// send message and no reply requested or accepted 
    
    // ======================================================
	// CTP File Transfer Commands
    // ======================================================
    // replaced - public static final int CTP_F_CMD_INIT = 10;				// return features and versions 
    
    // replaced - public static final int CTP_F_CMD_SETTING = 11;				// set settings - for example default to progress on or off, log level
    
    // initial stream commands							
    public static final int CTP_F_CMD_STREAM_READ = 0x20;			// enter read blocks mode 
    public static final int CTP_F_CMD_STREAM_WRITE = 0x21;		// enter write blocks mode 
    
    // ongoing/inside stream commands
    public static final int CTP_F_CMD_STREAM_BLOCK = 0x22;
    // replaced - public static final int CTP_F_CMD_STREAM_PROGRESS = 0x23;		// file cnt, current file num and % done
    public static final int CTP_F_CMD_STREAM_FINAL = 0x24;
    public static final int CTP_F_CMD_STREAM_ABORT = 0x25;
    
    // logging/debugging commands use these
    // replaced - public static final int CTP_F_CMD_SEND_WAIT = 30;			// send message then wait (non-blocking) for reply (op message only, not general message)
    // replaced - public static final int CTP_F_CMD_SEND_FORGET = 31;			// send message and no reply requested or accepted (op message only, not general message) 

    // -----------------
    // OPs are CTP_CMD_SEND and CTP_CMD_RESPONSE based, with potential for CTP_CMD_PROGRESS
    // -----------------
    
    // simple ops
    public static final String CTP_F_OP_SELECT = "SELECT";			// Path/JSON in - includes sorting option
    public static final String CTP_F_OP_DELETE = "DELETE";			// last SELECT implied
    public static final String CTP_F_OP_MOVE = "MOVE";    			// last SELECT implied, Relative (src) Path, Dest Path in
    public static final String CTP_F_OP_UPDATE = "UPDATE";			// last SELECT implied, attributes to set
    
    // here are the manual evidence commands
    public static final String CTP_F_OP_EVIDENCE_MANUAL = "EVIDENCE";		// path, type and value OR passive: path and type OR pipe and type
    																		// or Path and Fail or Pipe and fail
    
    // contrib and obtain ops
    // BATCH UPLOAD or DOWNLOAD
    public static final String CTP_F_OP_TX_START = "TX_START";				// name/params for obtainment
    public static final String CTP_F_OP_TX_RESUME = "TX_RESUME";			// name/params for obtainment
    public static final String CTP_F_OP_TX_COMMIT = "TX_COMMIT";			// end marker, final params for obtainment - return a unique obtain id with SUCCESS, or return FAILURE/DENIED
    public static final String CTP_F_OP_TX_STATUS = "TX_STATUS";			// use obtain id to get status of obtainment - during or after
    public static final String CTP_F_OP_TX_ROLLBACK = "TX_ROLLBACK";		// retract all tracking of obtainment items
    public static final String CTP_F_OP_TX_SAVE = "TX_SAVE";				// save all obtainment items for future resume
        
    // ======================================================
	// CTP File Transfer Responses
    // ======================================================

    //public static final int CTP_F_CMD_PROGRESS = 81;			// amount
    
    // ======================================================
	// CTP File Transfer File/Folder selected attributes/content
    // ======================================================
    
    // ATTR type 2 bytes
    public static final int CTP_F_ATTR_END = 0;					// fake attribute, used to mark end of list of attributes
    public static final int CTP_F_ATTR_PATH = 1;				// file path
    public static final int CTP_F_ATTR_IS_FOLDER = 2;			// this is a folder
    public static final int CTP_F_ATTR_SIZE = 3;
    public static final int CTP_F_ATTR_MODTIME = 4; 
    public static final int CTP_F_ATTR_PERMISSIONS = 5;
    public static final int CTP_F_ATTR_PREFERED = 10;			// always 1, 2, 3, 4, 5 - plus options returned by CTP_F_CMD_INIT or set by CMD_SET
    public static final int CTP_F_ATTR_DATA = 20;				// means include data/body in stream
    public static final int CTP_F_ATTR_FILE_OFFSET = 30;				// means offset from file start for this file
    public static final int CTP_F_ATTR_MIME = 100;				// what MIME does server have for this file
    public static final int CTP_F_ATTR_MD5 = 200;			
    public static final int CTP_F_ATTR_SHA1 = 201;			
    public static final int CTP_F_ATTR_SHA256 = 202;			
    public static final int CTP_F_ATTR_SHA384 = 203;			
    public static final int CTP_F_ATTR_SHA512 = 204;			
    
    // PERM type 1 byte
    public static final int CTP_F_PERMISSIONS_NONE = 0;
    public static final int CTP_F_PERMISSIONS_EXECUTE = 1;
    public static final int CTP_F_PERMISSIONS_WRITE = 2;
    public static final int CTP_F_PERMISSIONS_READ = 4;
    
    // BLOCK TYPE 1 byte
    public static final int CTP_F_BLOCK_TYPE_HEADER = 1;		// combine values in 1 flag
    public static final int CTP_F_BLOCK_TYPE_CONTENT = 2;
    public static final int CTP_F_BLOCK_TYPE_EOF = 4;			// any block may contain CONTENT or HEADER, the last block for a file should have EOF - if nothing else
    
    // EVIDENCE TYPE 1 byte
    public static final int CTP_F_EVIDENCE_BASIC = 0;			// we checked and think it is good (only for downloads)
    public static final int CTP_F_EVIDENCE_SIZE = 1;
    public static final int CTP_F_EVIDENCE_MD5 = 2;			
    public static final int CTP_F_EVIDENCE_SHA1 = 3;			
    public static final int CTP_F_EVIDENCE_SHA256 = 4;			
    public static final int CTP_F_EVIDENCE_SHA384 = 5;			
    public static final int CTP_F_EVIDENCE_SHA512 = 6;			    
    
}
