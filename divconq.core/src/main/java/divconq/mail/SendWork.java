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
package divconq.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.io.InputWrapper;
import divconq.io.OutputWrapper;
import divconq.lang.Memory;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.DebugLevel;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.HexUtil;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class SendWork implements IWork {
	@Override
	public void run(TaskRun task) {
		XElement settings = MailTaskFactory.getSettings();
		
		if (settings == null) {
			task.error("Missing email settings");
			task.complete();
			return;
		}
		
		String smtpHost = settings.getAttribute("SmtpHost");
		int smtpPort = (int) StringUtil.parseInt(settings.getAttribute("SmtpPort"), 587);
		boolean smtpAuth = Struct.objectToBoolean(settings.getAttribute("SmtpAuth", "false"));
		boolean smtpDebug = Struct.objectToBoolean(settings.getAttribute("SmtpDebug", "false"));
		String smtpUsername = settings.getAttribute("SmtpUsername");
		String smtpPassword = settings.hasAttribute("SmtpPassword") 
				? Hub.instance.getClock().getObfuscator().decryptHexToString(settings.getAttribute("SmtpPassword"))
				: null;
		
		String debugBCC = settings.getAttribute("BccDebug");
		String skipto = settings.getAttribute("SkipToAddress");
		
		RecordStruct req = (RecordStruct) task.getTask().getParams();
		
		try {
			String from = req.getFieldAsString("From");		
			String reply = req.getFieldAsString("ReplyTo");		
			
			if (StringUtil.isEmpty(from))
				from = settings.getAttribute("DefaultFrom");
			
			if (StringUtil.isEmpty(reply))
				reply = settings.getAttribute("DefaultReplyTo");
			
			String to = req.getFieldAsString("To");
			String subject = req.getFieldAsString("Subject");
			String body = req.getFieldAsString("Body");
			String textbody = req.getFieldAsString("TextBody");
			
			task.info(0, "Sending email from: " + from);
			task.info(0, "Sending email to: " + to);
			
			Properties props = new Properties();
			
			if (smtpAuth) {
				props.put("mail.smtp.auth", "true");
				
				// TODO put this back in for Java8 - until then we have issues with Could not generate DH keypair
				// see http://stackoverflow.com/questions/12743846/unable-to-send-an-email-using-smtp-getting-javax-mail-messagingexception-could
				props.put("mail.smtp.starttls.enable", "true");
			}
			
	        Session session = Session.getInstance(props);
	
	        // do debug on task with trace level
	        if (smtpDebug || (OperationContext.get().getLevel() == DebugLevel.Trace)) {
		        session.setDebugOut(new DebugPrintStream(task));
		        session.setDebug(true);			        
	        }
	        
	        // Create a new Message
	    	javax.mail.Message email = new MimeMessage(session);
		        
			InternetAddress fromaddr = StringUtil.isEmpty(from) ? null : InternetAddress.parse(from.replace(';', ','))[0];
			InternetAddress[] rplyaddrs = StringUtil.isEmpty(reply) ? null : InternetAddress.parse(reply.replace(';', ','));
			InternetAddress[] toaddrs = StringUtil.isEmpty(to) ? new InternetAddress[0] : InternetAddress.parse(to.replace(';', ','));
			InternetAddress[] dbgaddrs = StringUtil.isEmpty(debugBCC) ? new InternetAddress[0] : InternetAddress.parse(debugBCC.replace(';', ','));
			
			if (StringUtil.isNotEmpty(skipto)) {
				List<InternetAddress> passed = new ArrayList<InternetAddress>();
				
				for (int i = 0; i < toaddrs.length; i++) {
					InternetAddress toa = toaddrs[i];
					
					if (!toa.getAddress().contains(skipto))
						passed.add(toa);
				}
				
				toaddrs = passed.stream().toArray(InternetAddress[]::new);
			}
			
	        try {				
				email.setFrom(fromaddr);
	        	
	        	if (rplyaddrs != null)
	        		email.setReplyTo(rplyaddrs);
	        	
	        	if (toaddrs != null)
	        		email.addRecipients(javax.mail.Message.RecipientType.TO, toaddrs);
	        	
	        	if (dbgaddrs != null)
	        		email.addRecipients(javax.mail.Message.RecipientType.BCC, dbgaddrs);
	        	
	        	email.setSubject(subject);
	     
	            // ALTERNATIVE TEXT/HTML CONTENT
	            MimeMultipart cover = new MimeMultipart((textbody != null) ? "alternative" : "mixed");
	            
	            if (textbody != null) {
		            MimeBodyPart txt = new MimeBodyPart();
		            txt.setText(textbody);
		            cover.addBodyPart(txt);
	            }
	        	
	            // add the message part 
	            MimeBodyPart html = new MimeBodyPart();
	            html.setContent(body, "text/html");
	            cover.addBodyPart(html);
	     
	            // add the attachment parts, if any
	            ListStruct attachments = req.getFieldAsList("Attachments");
	            
	            if ((attachments != null) && (attachments.getSize() > 0)) {
	            	// hints - https://mlyly.wordpress.com/2011/05/13/hello-world/
		            // COVER WRAP
		            MimeBodyPart wrap = new MimeBodyPart();
		            wrap.setContent(cover);
		            
		            MimeMultipart content = new MimeMultipart("related");
		            content.addBodyPart(wrap);	        	
	            	
	            	for (Struct itm : attachments.getItems()) {
	            		RecordStruct attachment = (RecordStruct) itm;
	            	
	            		final String name = attachment.getFieldAsString("Name"); 
	            		final String mime = attachment.getFieldAsString("Mime"); 
	            		final Memory mem = attachment.getFieldAsBinary("Content");
	            		
	            		mem.setPosition(0);
	            		
	            		MimeBodyPart apart = new MimeBodyPart();

			            DataSource source = new DataSource() {								
							@Override
							public OutputStream getOutputStream() throws IOException {
								return new OutputWrapper(mem);		// TODO technically we should reset mem to pos 0
							}
							
							@Override
							public String getName() {
								return name;
							}
							
							@Override
							public InputStream getInputStream() throws IOException {
								return new InputWrapper(mem);		// TODO technically we should reset mem to pos 0 
							}
							
							@Override
							public String getContentType() {
								return mime;
							}
						};
						
						apart.setDataHandler(new DataHandler(source));
						apart.setFileName(name);
			            
			            content.addBodyPart(apart);
	            	}
	            	
	            	email.setContent(content);
	            }
	            else {
	            	email.setContent(cover);
	            }
	            
	        	email.saveChanges();		        	
	        } 
	        catch (Exception x) {
	        	task.error(1, "dciSendMail unable to send message due to invalid fields.");
	        }

	        InternetAddress[] recip = Stream.concat(Arrays.stream(toaddrs), Arrays.stream(dbgaddrs)).toArray(InternetAddress[]::new);
	        
        	if (!task.hasErrors() && (recip.length > 0)) {
    	    	Transport t = null;
	        	
		        try {
					t = session.getTransport("smtp");
					
					t.connect(smtpHost, smtpPort, smtpUsername, smtpPassword);
					
	        		t.sendMessage(email, recip);
					
		            t.close();
		        	
		            // TODO wish we could get INFO: Received successful response: 200, AWS Request ID: b599ca95-bc82-11e0-846a-ab5fa57d84d4
		        } 
		        catch (Exception x) {
		        	task.error(1, "dciSendMail unable to send message due to service problems.  Error: " + x);
		        }
		        
		        if (t != null) {
		        	if (t.isConnected()) {
		        		try {
							t.close();
						} 
		        		catch (MessagingException e) {
						}
		        	}
		        }
        	}
			
	        if (task.hasErrors())
	        	task.info(0, "Unable to send email to: " + to);
	        else
	        	task.info(0, "Email sent to: " + to);
		}
        catch (AddressException x) {
        	task.error(1, "dciSendMail unable to send message due to addressing problems.  Error: " + x);
        }
		finally {
			RecordStruct smsg = req.getFieldAsRecord("StatusMessage");		
			
			if (smsg != null) {
				Message smsg2 = task.toLogMessage();
				smsg2.copyFields(smsg);
				Hub.instance.getBus().sendMessage(smsg2);
			}
			
			task.complete();
		}
	}
	
	public class DebugPrintStream extends PrintStream {
		protected OperationResult or = null;
		
		public DebugPrintStream(OperationResult or) {
			super(new OutputStream() {				
				@Override
				public void write(int b) throws IOException {
					if (b == 13)
						System.out.println();
					else
						System.out.print(HexUtil.charToHex(b));
				}
			});
			
			this.or = or;			
		}
		
		@Override
		public void println(String msg) {
			or.trace(0, msg);
		}
	}

}
