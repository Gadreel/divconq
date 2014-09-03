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
 * This package provides the fundamentals for processing a GET request on
 * the DivConq server.  It was originally designed to support the View
 * templating system: {@link divconq.view}.
 * 
 * However now it currently processes only static file requests and .gas
 * dynamic files - .gas files are very simple Groovy scripts that do not 
 * async processing.
 * 
 *  Static files may contain macros, but that is as far as dynamic goes
 *  right now.  Our focus is on PUI and client side dynamicism.
 *  
 *  TODO we need to support Content Security Policy (CSP), CORS and
 *  also be aware that some files may need to be loaded from CDN
 *  so design should be that content generators should be able to
 *  provide a list of "static" links and then generate all pages for
 *  those links - which then may be placed on the CDN.
 */
package divconq.web;


