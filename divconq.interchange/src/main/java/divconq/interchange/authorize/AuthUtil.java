package divconq.interchange.authorize;

import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class AuthUtil {
	static public final String AUTH_TEST_ENDPOINT = "https://apitest.authorize.net/xml/v1/request.api";
	static public final String AUTH_LIVE_ENDPOINT = "https://api.authorize.net/xml/v1/request.api";
	
	static public void authXCard(String authid, String authkey, String refid, boolean live, boolean test, RecordStruct order, FuncCallback<RecordStruct> callback) {
		OperationResult res = order.validate("dcmOrderInfo");
		
		if (res.hasErrors()) {
			callback.complete();
			return;
		}

		if (order.isFieldEmpty("PaymentInfo")) {
			callback.error("Missing payment details.");
			callback.complete();
			return;
		}
		
		RecordStruct paymentinfo = (RecordStruct) order.removeField("PaymentInfo").getValue();
		RecordStruct custinfo = order.getFieldAsRecord("CustomerInfo");		// required
		RecordStruct billinginfo = order.getFieldAsRecord("BillingInfo");	// not required
		RecordStruct shipinfo = order.getFieldAsRecord("ShippingInfo");	// not required
		RecordStruct calcinfo = order.getFieldAsRecord("CalcInfo");	// not required in schema

		if (paymentinfo.isFieldEmpty("CardNumber") || paymentinfo.isFieldEmpty("Expiration") || paymentinfo.isFieldEmpty("Code")) {
			callback.error("Missing payment details.");
			callback.complete();
			return;
		}
		
		if (billinginfo == null) {
			callback.error("Missing billing details.");
			callback.complete();
			return;
		}
		
		if (calcinfo == null) {
			callback.error("Missing billing computations.");
			callback.complete();
			return;
		}
		
	    XElement root = new XElement("createTransactionRequest", 
	    		new XAttribute("xmlns", "AnetApi/xml/v1/schema/AnetApiSchema.xsd"));
		
	    root.add(
	    		new XElement("merchantAuthentication",
	    				new XElement("name", authid),
						new XElement("transactionKey", authkey)
	    		)
	    );
		
	    root.add(
	    		new XElement("refId", refid.replace("_", ""))
	    );
	    
	    BigDecimal tax = calcinfo.getFieldAsDecimal("TaxTotal");
	    BigDecimal ship = calcinfo.getFieldAsDecimal("ShipTotal");
	    BigDecimal total = calcinfo.getFieldAsDecimal("GrandTotal");
		
	    XElement txreq = new XElement("transactionRequest",
				new XElement("transactionType", "authCaptureTransaction"),		// or authOnlyTransaction
				new XElement("amount", total.toPlainString()),
				new XElement("payment", 
						new XElement("creditCard", 
	    						new XElement("cardNumber", paymentinfo.getFieldAsString("CardNumber")),
	    						new XElement("expirationDate", paymentinfo.getFieldAsString("Expiration")),
	    						new XElement("cardCode", paymentinfo.getFieldAsString("Code"))
						)
				)
		);
	    
		ListStruct items = order.getFieldAsList("Items");
		
		if ((items != null) && (items.getSize() > 0)) {
			XElement ilist = new XElement("lineItems");
			
			for (Struct i : items.getItems()) {
				RecordStruct itm = (RecordStruct) i;
				
				String price = itm.isFieldEmpty("SalePrice") 
						? itm.getFieldAsString("Price") : itm.getFieldAsString("SalePrice");
						
				if (StringUtil.isEmpty(price))
					price = "0";
				
				String title = itm.getFieldAsString("Title");
				
				if (StringUtil.isEmpty(title))
					title = "[unkown]";
				
				if (title.length() > 31)
					title = title.substring(0, 31);
				
				String desc = itm.getFieldAsString("Description");
				
				if (StringUtil.isEmpty(desc))
					desc = "[not availale]";
				
				if (desc.length() > 255)
					desc = desc.substring(0, 255);
				
				XElement iline = new XElement("lineItem");
				
				if (itm.hasField("Sku"))
					iline.add(new XElement("itemId").withText(StringUtil.stripAllNonAscci(itm.getFieldAsString("Sku"))));
				
				iline.add(new XElement("name").withText(StringUtil.stripAllNonAscci(title)));
				//iline.add(new XElement("description").withText(desc));
				iline.add(new XElement("quantity").withText(itm.getFieldAsString("Quantity")));
				iline.add(new XElement("unitPrice").withText(price));
				
				ilist.add(iline);
			}
			
		    txreq.add(ilist);
		}

		txreq.add(
			new XElement("tax", 
					new XElement("amount", tax.toPlainString()),
					new XElement("name", billinginfo.getFieldAsString("State"))
			)
		);
		
		txreq.add(
			new XElement("shipping", 
					new XElement("amount", ship.toPlainString()),
					new XElement("name", billinginfo.getFieldAsString("State"))
			)
		);
			
	    if (!custinfo.isFieldEmpty("CustomerId"))
		    txreq.add(
		    		new XElement("customer", 
						new XElement("id", custinfo.getFieldAsString("CustomerId").replace("_", "")),
		    			new XElement("email", custinfo.getFieldAsString("Email"))
		    		)
		    );
	    else
		    txreq.add(
		    		new XElement("customer", 
		    			new XElement("email", custinfo.getFieldAsString("Email"))
		    		)
		    );
	    
	    txreq.add(
	    		new XElement("billTo", 
					new XElement("firstName", billinginfo.getFieldAsString("FirstName")),
					new XElement("lastName", billinginfo.getFieldAsString("LastName")),
					new XElement("address", billinginfo.getFieldAsString("Address")),
					new XElement("city", billinginfo.getFieldAsString("City")),
					new XElement("state", billinginfo.getFieldAsString("State")),
					new XElement("zip", billinginfo.getFieldAsString("Zip")),
					new XElement("country", "USA"),			// TODO add international support
					new XElement("phoneNumber", custinfo.getFieldAsString("Phone"))
	    		)
	    );
	    
	    if (shipinfo != null)
		    txreq.add(
		    		new XElement("shipTo", 
						new XElement("firstName", shipinfo.getFieldAsString("FirstName")),
						new XElement("lastName", shipinfo.getFieldAsString("LastName")),
						new XElement("address", shipinfo.getFieldAsString("Address")),
						new XElement("city", shipinfo.getFieldAsString("City")),
						new XElement("state", shipinfo.getFieldAsString("State")),
						new XElement("zip", shipinfo.getFieldAsString("Zip")),
						new XElement("country", "USA")		// TODO add international support
		    		)
		    );
	    
	    String origin = OperationContext.get().getOrigin();
	    
	    // track web customers
	    if (StringUtil.isNotEmpty(origin) && origin.startsWith("http:")) 
	    	txreq.add(new XElement("customerIP", origin.substring(5)));
	    
	    /* TODO possible enhancements
    	XElement settings = new XElement("transactionSettings");

    	if (test)
	    	settings.add(
	    		new XElement("setting",
	    			new XElement("settingName", "testRequest"),
	    			new XElement("settingValue", "true")
	    		)
	    	);
    	
    	settings.add(
	    		new XElement("setting",
	    			new XElement("settingName", "emailCustomer"),
	    			new XElement("settingValue", "false")
	    		)
	    	);
	    
      	txreq.add(settings);
    	*/
      
	    root.add(txreq);

	    // Auth documentation:
	    // http://developer.authorize.net/api/reference/
	    
	    try {
	    	OperationContext.get().touch();
	    	
			URL url = new URL(live ? AUTH_LIVE_ENDPOINT : AUTH_TEST_ENDPOINT);
		    
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			 
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
			con.setRequestProperty("Content-Type", "text/xml");
	 
			String body = root.toString();

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(body);
			wr.flush();
			wr.close();
	 
			int responseCode = con.getResponseCode();
	
			if (responseCode == 200) {
				// parse and close response stream
				FuncResult<XElement> xres = XmlReader.parse(con.getInputStream(), false);
				
				XElement resp = xres.getResult();
				
				XElement tr = resp.find("transactionResponse");
				
				if (tr == null) {
					callback.error("Error processing payment: Gateway sent an incomplete response.");
					
					callback.setResult(
							new RecordStruct()
								.withField("Message", resp)
					);
				}
				else {
					XElement trc = tr.find("responseCode");
					XElement trid = tr.find("transId");
					
					if ((trc == null) || (trid == null)) {
						callback.error("Error processing payment: Gateway sent an incomplete transaction response.");
						
						callback.setResult(
								new RecordStruct()
									.withField("Message", resp)
						);
					}
					else {
						String rcodeout = trc.getText();
						String txidout = trid.getText();
						
						if (!"1".equals(rcodeout))
							callback.error("Payment was rejected by gateway");
						
						callback.setResult(
								new RecordStruct()
									.withField("Code", rcodeout)
									.withField("TxId", txidout)
									.withField("Message", resp)
						);
					}
				}
			}
			else 
				callback.error("Error processing payment: Unable to connect to payment gateway.");
	    }
	    catch (Exception x) {
	    	callback.error("Error processing payment: Unable to connect to payment gateway.");
	    }
	    
		callback.complete();
	}
}
