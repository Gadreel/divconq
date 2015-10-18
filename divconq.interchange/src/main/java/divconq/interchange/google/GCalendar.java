package divconq.interchange.google;

import java.net.URL;

import divconq.lang.op.FuncResult;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

public class GCalendar {
	static public ListStruct listAvailable(String token) {
		try {
			URL url = new URL("https://www.googleapis.com/calendar/v3/users/me/calendarList");
			
			FuncResult<CompositeStruct> res = CompositeParser.parseJson(url, 
					new RecordStruct().withField("Authorization", "Bearer " + token));
			
			if (res.isEmptyResult()) 
				return null;
			
			return ((RecordStruct) res.getResult()).getFieldAsList("items");
		}
		catch (Exception x) {
			// TODO error handling?
		}
		
		return null;
	}
	
	/*
PUT https://www.googleapis.com/calendar/v3/calendars/6eel2dop36q8b2l9283h7ip8j8%40group.calendar.google.com/events

{
 "end": {
  "timeZone": "America/Los_Angeles",
  "dateTime": "2015-07-25T17:00:00-07:00"
 },
 "start": {
  "dateTime": "2015-07-25T09:00:00-07:00",
  "timeZone": "America/Los_Angeles"
 },
 "summary": "Google I/O 2015 test 3",
 "description": "A chance to hear more about Google's developer products.",
 "location": "800 Howard St., San Francisco, CA 94103"
}


{

 "kind": "calendar#event",
 "etag": "\"2883599176228000\"",
 "id": "4b5usqpkggn9pb00t8sbknrug8",
 "status": "confirmed",
 "htmlLink": "https://www.google.com/calendar/event?eid=NGI1dXNxcGtnZ245cGIwMHQ4c2JrbnJ1ZzggNmVlbDJkb3AzNnE4YjJsOTI4M2g3aXA4ajhAZw",
 "created": "2015-09-09T11:53:08.000Z",
 "updated": "2015-09-09T11:53:08.114Z",
 "summary": "Google I/O 2015 test 3",
 "description": "A chance to hear more about Google's developer products.",
 "location": "800 Howard St., San Francisco, CA 94103",
 "creator": {
  "email": "lightofgadrel@gmail.com",
  "displayName": "Andrew White"
 },
 "organizer": {
  "email": "6eel2dop36q8b2l9283h7ip8j8@group.calendar.google.com",
  "displayName": "MNS",
  "self": true
 },
 "start": {
  "dateTime": "2015-07-25T11:00:00-05:00",
  "timeZone": "America/Los_Angeles"
 },
 "end": {
  "dateTime": "2015-07-25T19:00:00-05:00",
  "timeZone": "America/Los_Angeles"
 },
 "iCalUID": "4b5usqpkggn9pb00t8sbknrug8@google.com",
 "sequence": 0,
 "reminders": {
  "useDefault": true
 }
}
 

 	 * 
	 */
	
	static public RecordStruct addEvent(String token, String calendarId, RecordStruct eventinfo) {
		try {
			URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events");
			
			FuncResult<CompositeStruct> res = CompositeParser.transactJson(eventinfo, url, 
					new RecordStruct().withField("Authorization", "Bearer " + token));
			
			if (res.isEmptyResult()) 
				return null;
			
			return (RecordStruct) res.getResult();
		}
		catch (Exception x) {
			// TODO error handling?
		}
		
		return null;
	}
	
	/*
PUT https://www.googleapis.com/calendar/v3/calendars/6eel2dop36q8b2l9283h7ip8j8%40group.calendar.google.com/events/4b5usqpkggn9pb00t8sbknrug8
 
{
 "end": {
  "timeZone": "America/Los_Angeles",
  "dateTime": "2015-07-25T18:00:00-07:00"
 },
 "start": {
  "dateTime": "2015-07-25T09:00:00-07:00",
  "timeZone": "America/Los_Angeles"
 },
 "summary": "Google I/O 2015 test 3",
 "description": "A chance to hear more about Google's developer products.",
 "location": "800 Howard St., San Francisco, CA 94103"
}


{

 "kind": "calendar#event",
 "etag": "\"2883599574810000\"",
 "id": "4b5usqpkggn9pb00t8sbknrug8",
 "status": "confirmed",
 "htmlLink": "https://www.google.com/calendar/event?eid=NGI1dXNxcGtnZ245cGIwMHQ4c2JrbnJ1ZzggNmVlbDJkb3AzNnE4YjJsOTI4M2g3aXA4ajhAZw",
 "created": "2015-09-09T11:53:08.000Z",
 "updated": "2015-09-09T11:56:27.405Z",
 "summary": "Google I/O 2015 test 3",
 "description": "A chance to hear more about Google's developer products.",
 "location": "800 Howard St., San Francisco, CA 94103",
 "creator": {
  "email": "lightofgadrel@gmail.com",
  "displayName": "Andrew White"
 },
 "organizer": {
  "email": "6eel2dop36q8b2l9283h7ip8j8@group.calendar.google.com",
  "displayName": "MNS",
  "self": true
 },
 "start": {
  "dateTime": "2015-07-25T11:00:00-05:00",
  "timeZone": "America/Los_Angeles"
 },
 "end": {
  "dateTime": "2015-07-25T20:00:00-05:00",
  "timeZone": "America/Los_Angeles"
 },
 "iCalUID": "4b5usqpkggn9pb00t8sbknrug8@google.com",
 "sequence": 0,
 "reminders": {
  "useDefault": true
 }
}
 	 * 
	 */
	
	static public RecordStruct changeEvent(String token, String calendarId, String eventId, RecordStruct eventinfo) {
		try {
			URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events/" + eventId);
			
			FuncResult<CompositeStruct> res = CompositeParser.transactJson(eventinfo, url, 
					new RecordStruct().withField("Authorization", "Bearer " + token));
			
			if (res.isEmptyResult()) 
				return null;
			
			return (RecordStruct) res.getResult();
		}
		catch (Exception x) {
			// TODO error handling?
		}
		
		return null;
	}
}
