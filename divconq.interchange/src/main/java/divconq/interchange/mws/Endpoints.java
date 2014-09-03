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
package divconq.interchange.mws;

import java.net.URI;

import divconq.log.Logger;

/**
 * Lookup URI for MWS services.
 */
public class Endpoints {

    /** URI for CN production. */
    /** URI for DE production. */
    /** URI for ES production. */
    /** URI for EU production. */
    /** URI for FR production. */
    /** URI for IN production. */
    /** URI for IT production. */
    /** URI for JP production. */
    /** URI for NA production. */
    /** URI for UK production. */

    static public URI lookup(String name) {
        try {
        	switch (name)
        	{
        	case "CN_PROD": return new URI("https://mws.amazonservices.com.cn");
        	case "DE_PROD": return new URI("https://mws.amazonservices.de");
        	case "ES_PROD": return new URI("https://mws.amazonservices.es");
        	case "EU_PROD": return new URI("https://mws-eu.amazonservices.com");
        	case "FR_PROD": return new URI("https://mws.amazonservices.fr");
        	case "IN_PROD": return new URI("https://mws.amazonservices.in");
        	case "IT_PROD": return new URI("https://mws.amazonservices.it");
        	case "JP_PROD": return new URI("https://mws.amazonservices.jp");
        	case "NA_PROD": return new URI("https://mws.amazonservices.com");
        	case "UK_PROD": return new URI("https://mws.amazonservices.co.uk");
        	}
        } 
        catch (Exception x) {
            Logger.error("Unable to initialize MWS endpoints: " + x);
        }
        
        return null;
    }
}
