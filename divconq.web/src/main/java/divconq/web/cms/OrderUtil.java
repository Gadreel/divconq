package divconq.web.cms;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.joda.time.DateTime;

import divconq.db.ObjectResult;
import divconq.db.query.CollectorField;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.query.WhereField;
import divconq.db.query.WhereNotEqual;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.hub.Hub;
import divconq.interchange.authorize.AuthUtil;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.UserContext;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class OrderUtil {
	
	static public void processAuthOrder(RecordStruct order, FuncCallback<String> callback) {
		OrderUtil.santitizeAndCalculateOrder(order, new OperationCallback() {
			@Override
			public void callback() {
		    	OperationContext.get().touch();
		    	
				if (this.hasErrors()) {
					callback.complete();
					return;
				}
				
				DateTime now = new DateTime();
				
				RecordStruct orderclean = (RecordStruct) order.deepCopy();
				
				// remove sensitive information before saving
				RecordStruct cleanpay = orderclean.getFieldAsRecord("PaymentInfo");
				
				if (cleanpay != null) {
					cleanpay.removeField("CardNumber");
					cleanpay.removeField("Expiration");
					cleanpay.removeField("Code");
				}
				
				// insert the order
				DbRecordRequest req = new InsertRecordRequest()
					.withTable("dcmOrder")		
					.withSetField("dcmOrderDate", now)
					.withSetField("dcmStatus", "AwaitingPayment")
					.withSetField("dcmLastStatusDate", now)
					.withSetField("dcmOrderInfo", orderclean)
					.withSetField("dcmGrandTotal", order.getFieldAsRecord("CalcInfo").getFieldAsDecimal("GrandTotal"));
				
				UserContext uctx = OperationContext.get().getUserContext();
				
				// if this is an authenticated user then we want to track the customer id too
				if (uctx.isAuthenticated())
					req.withSetField("dcmCustomer", uctx.getUserId());
				
				Hub.instance.getDatabase().submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
				    	OperationContext.get().touch();
				    	
						if (this.hasErrors()) {
							callback.complete();
							return;
						}
						
						RecordStruct resultrec = (RecordStruct) result;
						
						String refid = resultrec.getFieldAsString("Id");
						
						callback.setResult(refid);
						
						boolean testing = Struct.objectToBooleanOrFalse(Hub.instance.getConfig().getAttribute("ForTesting"));
						
						// TODO lookup user and see if they are in "test" mode - this way some people can run test orders through system
						
						XElement dsettings = OperationContext.get().getDomain().getSettings();
						
						XElement sset = dsettings.find("Store");
						
						if (sset == null) {
							callback.error("Missing store settings.");
							callback.complete();
							return;
						}
						
						if (sset.hasAttribute("Mode"))
							testing = "Dev".equals(sset.getAttribute("Mode"));
						
						XElement auth = sset.selectFirst(testing ? "AuthorizeDev" : "AuthorizeLive");
						
						if (auth == null) {
							callback.error("Missing store Authorize settings.");
							callback.complete();
							return;
						}
						
						String lid = auth.getAttribute("LoginId");
						String key = auth.getAttribute("TransactionKey");
						
						String authid = Hub.instance.getClock().getObfuscator().decryptHexToString(lid);
						String authkey = Hub.instance.getClock().getObfuscator().decryptHexToString(key);
						
						// TODO store order items as independent records? order audits? other fields/tables to fill in?
						
						AuthUtil.authXCard(authid, authkey, refid, !testing, false, order, new FuncCallback<RecordStruct>() {
							@Override
							public void callback() {
						    	OperationContext.get().touch();
						    	
								DbRecordRequest upreq = new UpdateRecordRequest()
									.withId(refid)
									.withTable("dcmOrder");
								
								if (this.hasErrors() || this.isEmptyResult()) 
									upreq.withSetField("dcmStatus", "VerificationRequired");
								else 
									upreq.withSetField("dcmStatus", "AwaitingFulfillment");
								
								if (this.isEmptyResult())
									upreq.withSetField("dcmPaymentResponse", this.toLogMessage());
								else
									upreq.withSetField("dcmPaymentId", this.getResult().getFieldAsString("TxId"))
										.withSetField("dcmPaymentResponse", this.getResult().getFieldAsString("Message"));
								
								Hub.instance.getDatabase().submit(upreq, new ObjectResult() {
									@Override
									public void process(CompositeStruct result) {
								    	OperationContext.get().touch();
								    	
										callback.complete();
									}
								});
							}
						});
					}
				});
			}
		});
	}
	
	static public void santitizeAndCalculateOrder(RecordStruct order, OperationCallback callback) {
		// -------------------------------------------
		// be sure that the customer info is good
		RecordStruct custinfo = order.getFieldAsRecord("CustomerInfo");		// required

		UserContext uctx = OperationContext.get().getUserContext();
		
		// if this is an authenticated user then we want to track the customer id too
		if (uctx.isAuthenticated())
			custinfo.setField("CustomerId", uctx.getUserId());
		else
			custinfo.removeField("CustomerId");

		// -------------------------------------------
		// check products are real and priced right 
		
		ListStruct items = order.getFieldAsList("Items");
		ListStruct pidlist = new ListStruct();
		//ListStruct remlist = new ListStruct();
		
		for (Struct itm : items.getItems()) 
			pidlist.addItem(((RecordStruct) itm).getFieldAsString("Product"));
		
		// TODO grab weight / ship cost from here too but then remove before 'complete'
		SelectDirectRequest req = new SelectDirectRequest() 
			.withTable("dcmProduct")
			.withSelect(new SelectFields()
				.withField("Id", "Product")
				.withField("dcmTitle", "Title")
				.withField("dcmAlias", "Alias")
				.withField("dcmSku", "Sku")
				.withField("dcmDescription", "Description")
				.withField("dcmTag", "Tags")
				.withField("dcmPrice", "Price")
				.withField("dcmSalePrice", "SalePrice")			
				.withField("dcmTaxFree", "TaxFree")
				.withField("dcmShipFree", "ShipFree")
				.withField("dcmShipAmount", "ShipAmount")
				.withField("dcmShipWeight", "ShipWeight")
				.withForeignField("dcmCategory", "CatShipAmount", "dcmShipAmount")
			)
			.withCollector(new CollectorField("Id").withValues(pidlist))
			.withWhere(new WhereNotEqual(new WhereField("dcmDisabled"), true));

		// do search
		Hub.instance.getDatabase().submit(req, new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
		    	OperationContext.get().touch();
		    	
				if (this.hasErrors()) {
					callback.complete();
					return;
				}
				
				XElement dsettings = OperationContext.get().getDomain().getSettings();
				
				BigDecimal itmcalc = new BigDecimal(0);
				BigDecimal taxcalc = new BigDecimal(0);
				BigDecimal shipcalc = new BigDecimal(0);
				
				BigDecimal itemshipcalc = new BigDecimal(0);
				BigDecimal catshipcalc = new BigDecimal(0);
				BigDecimal itemshipweight = new BigDecimal(0);
				
				// if we find items from submitted list in the database then add those items to our true item list
				ListStruct fnditems = new ListStruct();
				
				// loop our items
				for (Struct itm : items.getItems()) {
					RecordStruct item = (RecordStruct) itm;
				
					for (Struct match : ((ListStruct)result).getItems()) {
						RecordStruct rec = (RecordStruct) match;
						
						if (!rec.getFieldAsString("Product").equals(item.getFieldAsString("Product")))
							continue;
						
						// make sure we are using the 'real' values here, from the DB
						item.copyFields(rec, "ShipAmount", "ShipWeight", "CatShipAmount");
						
						// add to the 'real' order list
						fnditems.addItem(item);
						
						BigDecimal price = item.isFieldEmpty("SalePrice") 
								? item.getFieldAsDecimal("Price", BigDecimal.ZERO) : item.getFieldAsDecimal("SalePrice");
						
						BigDecimal qty = item.getFieldAsDecimal("Quantity", BigDecimal.ZERO);
						BigDecimal total = price.multiply(qty);
						
						item.withField("Total", total);
						
						itmcalc = itmcalc.add(total);
						
						if (!item.getFieldAsBooleanOrFalse("ShipFree"))
							shipcalc = shipcalc.add(total);
						
						if (!item.getFieldAsBooleanOrFalse("TaxFree"))
							taxcalc = taxcalc.add(total);
						
						if (!rec.isFieldEmpty("ShipAmount"))
							itemshipcalc = itemshipcalc.add(rec.getFieldAsDecimal("ShipAmount").multiply(qty));
						
						if (!rec.isFieldEmpty("ShipWeight"))
							itemshipweight = itemshipweight.add(rec.getFieldAsDecimal("ShipWeight").multiply(qty));
						
						if (!rec.isFieldEmpty("CatShipAmount"))
							catshipcalc = catshipcalc.add(rec.getFieldAsDecimal("CatShipAmount").multiply(qty));
						
						break;
					}
				}
				
				// replace the proposed items with the found and cleaned items
				order.setField("Items", fnditems);
				
				/* Enum="Disabled,OrderWeight,OrderTotal,PerItem,PerItemFromCategory,Custom" 
				 */
				
				// TODO look up shipping
				BigDecimal shipamt = new BigDecimal(0);		
				
				XElement shipsettings = dsettings.selectFirst("Store/Shipping");
				String shipmode = "Disabled";

				// shipping is based on Order Total before discounts
				if (shipsettings != null) {
					shipmode = shipsettings.getAttribute("Mode", shipmode);
					
					// TODO if OrderWeight,OrderTotal then do a table lookup in shipsettings 
					
					// TODO if custom then harass the domain watcher with a shipping calc
					
					if ("PerItem".equals(shipmode))
						shipamt = itemshipcalc;
					else if ("PerItemFromCategory".equals(shipmode))
						shipamt = catshipcalc;
				}
				
				// TODO look up coupons, check them and apply them
				BigDecimal itmdiscount = new BigDecimal(0);		// do not exceed itmcalc
				BigDecimal shipdiscount = new BigDecimal(0);		// do not exceed shipamt
				
				BigDecimal itmtotal = itmcalc.add(itmdiscount.negate());
				
				if (itmtotal.stripTrailingZeros().compareTo(BigDecimal.ZERO) < 0)
					itmtotal = BigDecimal.ZERO;
				
				BigDecimal shiptotal = shipamt.add(shipdiscount.negate());
				
				if (shiptotal.stripTrailingZeros().compareTo(BigDecimal.ZERO) < 0)
					shiptotal = BigDecimal.ZERO;
				
				// look up taxes
				BigDecimal taxat = BigDecimal.ZERO;
				
				XElement taxtable = dsettings.selectFirst("Store/TaxTable");
				
				if (taxtable != null) {
					String state = null;
					
					RecordStruct shipinfo = order.getFieldAsRecord("ShippingInfo");	// not required
					
					if ((shipinfo != null) && !shipinfo.isFieldEmpty("State"))
						state = shipinfo.getFieldAsString("State");
						
					if (StringUtil.isEmpty(state)) {
						RecordStruct billinfo = order.getFieldAsRecord("BillingInfo");	// not required
						
						if ((billinfo != null) && !billinfo.isFieldEmpty("State"))
							state = billinfo.getFieldAsString("State");
					}
					
					if (StringUtil.isNotEmpty(state)) {
						for (XElement stel : taxtable.selectAll("State")) {
							if (state.equals(stel.getAttribute("Alias"))) {
								taxat = new BigDecimal(stel.getAttribute("Rate", "0.0"));
								break;
							}
						}
					}
				}
				
				// TODO account for product discounts in taxcalc, apply discounts to the taxfree part first then reduce taxcalc by any remaining discount amt
				BigDecimal taxtotal = taxcalc.multiply(taxat).setScale(2, RoundingMode.HALF_EVEN);
				
				// correct order calculations, totals
				RecordStruct calcinfo = new RecordStruct()
					.withField("ItemCalc", itmcalc)
					.withField("ProductDiscount", itmdiscount)
					.withField("ItemTotal", itmtotal)
					.withField("ShipCalc", shipcalc)
					.withField("ShipAmount", shipamt)
					.withField("ShipDiscount", shipdiscount)
					.withField("ShipTotal", shiptotal)
					.withField("TaxCalc", taxcalc)
					.withField("TaxAt", taxat)
					.withField("TaxTotal", taxtotal)
					.withField("GrandTotal", itmtotal.add(shiptotal).add(taxtotal));
				
				order.setField("CalcInfo", calcinfo);
				
				callback.complete();
			}
		});
	}

}
