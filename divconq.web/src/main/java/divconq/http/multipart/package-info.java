/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 * 
 */

 /**
 * This package is not currently used.  It is based off of io.netty.handler.codec.http.multipart
 * and may be used in the future to support multi-part file POSTs as an alternative to the existing
 * support for straight PUT and POST methods for file uploads. 
 * 
 * Judging from the browser landscape and HTTP/2 this functionality will not be needed and may soon
 * be removed.
 * 
 * TODO If we do keep this, we should be sure to switch to io.netty.handler.codec.http.multipart as 
 * soon as HttpPostMultipartRequestDecoder is enhanced so it can tell us if it is completed or not.
 * The check for "completed" is the only reason at all for forking this package from Netty, we'd
 * prefer to return to using Netty. 
 */
package divconq.http.multipart;


