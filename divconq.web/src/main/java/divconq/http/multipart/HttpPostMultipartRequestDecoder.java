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
 */
package divconq.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import divconq.http.multipart.HttpPostBodyUtil.SeekAheadNoBackArrayException;
import divconq.http.multipart.HttpPostBodyUtil.SeekAheadOptimize;
import divconq.http.multipart.HttpPostBodyUtil.TransferEncodingMechanism;

import static io.netty.buffer.Unpooled.*;

/**
 * This decoder will decode Body and can handle POST BODY.
 *
 * You <strong>MUST</strong> call {@link #destroy()} after completion to release all resources.
 *
 */
public class HttpPostMultipartRequestDecoder implements InterfaceHttpPostRequestDecoder {
    /**
     * Factory used to create InterfaceHttpData
     */
    private final HttpDataFactory factory;

    /**
     * Request to decode
     */
    private final HttpRequest request;

    /**
     * Default charset to use
     */
    private Charset charset;

    /**
     * Does the last chunk already received
     */
    private boolean isLastChunk;

    /**
     * HttpDatas from Body
     */
    private final List<InterfaceHttpData> bodyListHttpData = new ArrayList<InterfaceHttpData>();

    /**
     * HttpDatas as Map from Body
     */
    private final Map<String, List<InterfaceHttpData>> bodyMapHttpData = new TreeMap<String, List<InterfaceHttpData>>(
            CaseIgnoringComparator.INSTANCE);

    /**
     * The current channelBuffer
     */
    private ByteBuf undecodedChunk;

    /**
     * Body HttpDatas current position
     */
    private int bodyListHttpDataRank;

    /**
     * If multipart, this is the boundary for the flobal multipart
     */
    private String multipartDataBoundary;

    /**
     * If multipart, there could be internal multiparts (mixed) to the global
     * multipart. Only one level is allowed.
     */
    private String multipartMixedBoundary;

    /**
     * Current getStatus
     */
    private MultiPartStatus currentStatus = MultiPartStatus.NOTSTARTED;

    /**
     * Used in Multipart
     */
    private Map<String, Attribute> currentFieldAttributes;

    /**
     * The current FileUpload that is currently in decode process
     */
    private FileUpload currentFileUpload;

    /**
     * The current Attribute that is currently in decode process
     */
    private Attribute currentAttribute;

    private boolean destroyed;

    private int discardThreshold = HttpPostRequestDecoder.DEFAULT_DISCARD_THRESHOLD;

    /**
     *
     * @param request
     *            the request to decode
     * @throws NullPointerException
     *             for request
     * @throws ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public HttpPostMultipartRequestDecoder(HttpRequest request) {
        this(new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE), request, HttpConstants.DEFAULT_CHARSET);
    }

    /**
     *
     * @param factory
     *            the factory used to create InterfaceHttpData
     * @param request
     *            the request to decode
     * @throws NullPointerException
     *             for request or factory
     * @throws ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public HttpPostMultipartRequestDecoder(HttpDataFactory factory, HttpRequest request) {
        this(factory, request, HttpConstants.DEFAULT_CHARSET);
    }

    /**
     *
     * @param factory
     *            the factory used to create InterfaceHttpData
     * @param request
     *            the request to decode
     * @param charset
     *            the charset to use as default
     * @throws NullPointerException
     *             for request or charset or factory
     * @throws ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public HttpPostMultipartRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        if (request == null) {
            throw new NullPointerException("request");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.request = request;
        this.charset = charset;
        this.factory = factory;
        // Fill default values

        setMultipart(this.request.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        if (request instanceof HttpContent) {
            // Offer automatically if the given request is als type of HttpContent
            // See #1089
            offer((HttpContent) request);
        } else {
            undecodedChunk = buffer();
            parseBody();
        }
    }

    /**
     * Set from the request ContentType the multipartDataBoundary and the possible charset.
     */
    private void setMultipart(String contentType) {
        String[] dataBoundary = HttpPostRequestDecoder.getMultipartDataBoundary(contentType);
        if (dataBoundary != null) {
            multipartDataBoundary = dataBoundary[0];
            if (dataBoundary.length > 1 && dataBoundary[1] != null) {
                charset = Charset.forName(dataBoundary[1]);
            }
        } else {
            multipartDataBoundary = null;
        }
        currentStatus = MultiPartStatus.HEADERDELIMITER;
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException(HttpPostMultipartRequestDecoder.class.getSimpleName()
                    + " was destroyed already");
        }
    }

    /**
     * True if this request is a Multipart request
     *
     * @return True if this request is a Multipart request
     */
    @Override
    public boolean isMultipart() {
        checkDestroyed();
        return true;
    }

    public MultiPartStatus getStatus() {
		return this.currentStatus;
	}
    
    /**
     * Set the amount of bytes after which read bytes in the buffer should be discarded.
     * Setting this lower gives lower memory usage but with the overhead of more memory copies.
     * Use {@code 0} to disable it.
     */
    @Override
    public void setDiscardThreshold(int discardThreshold) {
        if (discardThreshold < 0) {
          throw new IllegalArgumentException("discardThreshold must be >= 0");
        }
        this.discardThreshold = discardThreshold;
    }

    /**
     * Return the threshold in bytes after which read data in the buffer should be discarded.
     */
    @Override
    public int getDiscardThreshold() {
        return discardThreshold;
    }

    /**
     * This getMethod returns a List of all HttpDatas from body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return the list of HttpDatas from Body part for POST getMethod
     * @throws NotEnoughDataDecoderException
     *             Need more chunks
     */
    @Override
    public List<InterfaceHttpData> getBodyHttpDatas() {
        checkDestroyed();

        if (!isLastChunk) {
            throw new NotEnoughDataDecoderException();
        }
        return bodyListHttpData;
    }

    /**
     * This getMethod returns a List of all HttpDatas with the given name from
     * body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return All Body HttpDatas with the given name (ignore case)
     * @throws NotEnoughDataDecoderException
     *             need more chunks
     */
    @Override
    public List<InterfaceHttpData> getBodyHttpDatas(String name) {
        checkDestroyed();

        if (!isLastChunk) {
            throw new NotEnoughDataDecoderException();
        }
        return bodyMapHttpData.get(name);
    }

    /**
     * This getMethod returns the first InterfaceHttpData with the given name from
     * body.<br>
     *
     * If chunked, all chunks must have been offered using offer() getMethod. If
     * not, NotEnoughDataDecoderException will be raised.
     *
     * @return The first Body InterfaceHttpData with the given name (ignore
     *         case)
     * @throws NotEnoughDataDecoderException
     *             need more chunks
     */
    @Override
    public InterfaceHttpData getBodyHttpData(String name) {
        checkDestroyed();

        if (!isLastChunk) {
            throw new NotEnoughDataDecoderException();
        }
        List<InterfaceHttpData> list = bodyMapHttpData.get(name);
        if (list != null) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Initialized the internals from a new chunk
     *
     * @param content
     *            the new received chunk
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    @Override
    public HttpPostMultipartRequestDecoder offer(HttpContent content) {
        checkDestroyed();

        // Maybe we should better not copy here for performance reasons but this will need
        // more care by the caller to release the content in a correct manner later
        // So maybe something to optimize on a later stage
        ByteBuf buf = content.content();
        if (undecodedChunk == null) {
            undecodedChunk = buf.copy();
        } else {
            undecodedChunk.writeBytes(buf);
        }
        if (content instanceof LastHttpContent) {
            isLastChunk = true;
        }
        parseBody();
        if (undecodedChunk != null && undecodedChunk.writerIndex() > discardThreshold) {
            undecodedChunk.discardReadBytes();
        }
        return this;
    }

    /**
     * True if at current getStatus, there is an available decoded
     * InterfaceHttpData from the Body.
     *
     * This getMethod works for chunked and not chunked request.
     *
     * @return True if at current getStatus, there is a decoded InterfaceHttpData
     * @throws EndOfDataDecoderException
     *             No more data will be available
     */
    @Override
    public boolean hasNext() {
        checkDestroyed();

        if (currentStatus == MultiPartStatus.EPILOGUE) {
            // OK except if end of list
            if (bodyListHttpDataRank >= bodyListHttpData.size()) {
                throw new EndOfDataDecoderException();
            }
        }
        return !bodyListHttpData.isEmpty() && bodyListHttpDataRank < bodyListHttpData.size();
    }

    /**
     * Returns the next available InterfaceHttpData or null if, at the time it
     * is called, there is no more available InterfaceHttpData. A subsequent
     * call to offer(httpChunk) could enable more data.
     *
     * Be sure to call {@link InterfaceHttpData#release()} after you are done
     * with processing to make sure to not leak any resources
     *
     * @return the next available InterfaceHttpData or null if none
     * @throws EndOfDataDecoderException
     *             No more data will be available
     */
    @Override
    public InterfaceHttpData next() {
        checkDestroyed();

        if (hasNext()) {
            return bodyListHttpData.get(bodyListHttpDataRank++);
        }
        return null;
    }

    /**
     * This getMethod will parse as much as possible data and fill the list and map
     *
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    private void parseBody() {
        if (currentStatus == MultiPartStatus.PREEPILOGUE || currentStatus == MultiPartStatus.EPILOGUE) {
            if (isLastChunk) {
                currentStatus = MultiPartStatus.EPILOGUE;
            }
            return;
        }
        parseBodyMultipart();
    }

    /**
     * Utility function to add a new decoded data
     */
    protected void addHttpData(InterfaceHttpData data) {
        if (data == null) {
            return;
        }
        List<InterfaceHttpData> datas = bodyMapHttpData.get(data.getName());
        if (datas == null) {
            datas = new ArrayList<InterfaceHttpData>(1);
            bodyMapHttpData.put(data.getName(), datas);
        }
        datas.add(data);
        bodyListHttpData.add(data);
    }

    /**
     * Parse the Body for multipart
     *
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    private void parseBodyMultipart() {
        if (undecodedChunk == null || undecodedChunk.readableBytes() == 0) {
            // nothing to decode
            return;
        }
        InterfaceHttpData data = decodeMultipart(currentStatus);
        while (data != null) {
            addHttpData(data);
            if (currentStatus == MultiPartStatus.PREEPILOGUE || currentStatus == MultiPartStatus.EPILOGUE) {
                break;
            }
            data = decodeMultipart(currentStatus);
        }
    }

    /**
     * Decode a multipart request by pieces<br>
     * <br>
     * NOTSTARTED PREAMBLE (<br>
     * (HEADERDELIMITER DISPOSITION (FIELD | FILEUPLOAD))*<br>
     * (HEADERDELIMITER DISPOSITION MIXEDPREAMBLE<br>
     * (MIXEDDELIMITER MIXEDDISPOSITION MIXEDFILEUPLOAD)+<br>
     * MIXEDCLOSEDELIMITER)*<br>
     * CLOSEDELIMITER)+ EPILOGUE<br>
     *
     * Inspired from HttpMessageDecoder
     *
     * @return the next decoded InterfaceHttpData or null if none until now.
     * @throws ErrorDataDecoderException
     *             if an error occurs
     */
    private InterfaceHttpData decodeMultipart(MultiPartStatus state) {
        switch (state) {
        case NOTSTARTED:
            throw new ErrorDataDecoderException("Should not be called with the current getStatus");
        case PREAMBLE:
            // Content-type: multipart/form-data, boundary=AaB03x
            throw new ErrorDataDecoderException("Should not be called with the current getStatus");
        case HEADERDELIMITER: {
            // --AaB03x or --AaB03x--
            return findMultipartDelimiter(multipartDataBoundary, MultiPartStatus.DISPOSITION,
                    MultiPartStatus.PREEPILOGUE);
        }
        case DISPOSITION: {
            // content-disposition: form-data; name="field1"
            // content-disposition: form-data; name="pics"; filename="file1.txt"
            // and other immediate values like
            // Content-type: image/gif
            // Content-Type: text/plain
            // Content-Type: text/plain; charset=ISO-8859-1
            // Content-Transfer-Encoding: binary
            // The following line implies a change of mode (mixed mode)
            // Content-type: multipart/mixed, boundary=BbC04y
            return findMultipartDisposition();
        }
        case FIELD: {
            // Now get value according to Content-Type and Charset
            Charset localCharset = null;
            Attribute charsetAttribute = currentFieldAttributes.get(HttpHeaders.Values.CHARSET);
            if (charsetAttribute != null) {
                try {
                    localCharset = Charset.forName(charsetAttribute.getValue());
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
            }
            Attribute nameAttribute = currentFieldAttributes.get(HttpPostBodyUtil.NAME);
            if (currentAttribute == null) {
                try {
                    currentAttribute = factory.createAttribute(request,
                            cleanString(nameAttribute.getValue()));
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
                if (localCharset != null) {
                    currentAttribute.setCharset(localCharset);
                }
            }
            // load data
            try {
                loadFieldMultipart(multipartDataBoundary);
            } catch (NotEnoughDataDecoderException e) {
                return null;
            }
            Attribute finalAttribute = currentAttribute;
            currentAttribute = null;
            currentFieldAttributes = null;
            // ready to load the next one
            currentStatus = MultiPartStatus.HEADERDELIMITER;
            return finalAttribute;
        }
        case FILEUPLOAD: {
            // eventually restart from existing FileUpload
            return getFileUpload(multipartDataBoundary);
        }
        case MIXEDDELIMITER: {
            // --AaB03x or --AaB03x--
            // Note that currentFieldAttributes exists
            return findMultipartDelimiter(multipartMixedBoundary, MultiPartStatus.MIXEDDISPOSITION,
                    MultiPartStatus.HEADERDELIMITER);
        }
        case MIXEDDISPOSITION: {
            return findMultipartDisposition();
        }
        case MIXEDFILEUPLOAD: {
            // eventually restart from existing FileUpload
            return getFileUpload(multipartMixedBoundary);
        }
        case PREEPILOGUE:
            return null;
        case EPILOGUE:
            return null;
        default:
            throw new ErrorDataDecoderException("Shouldn't reach here.");
        }
    }

    /**
     * Skip control Characters
     *
     * @throws NotEnoughDataDecoderException
     */
    void skipControlCharacters() {
        SeekAheadOptimize sao;
        try {
            sao = new SeekAheadOptimize(undecodedChunk);
        } catch (SeekAheadNoBackArrayException e) {
            try {
                skipControlCharactersStandard();
            } catch (IndexOutOfBoundsException e1) {
                throw new NotEnoughDataDecoderException(e1);
            }
            return;
        }

        while (sao.pos < sao.limit) {
            char c = (char) (sao.bytes[sao.pos++] & 0xFF);
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                sao.setReadPosition(1);
                return;
            }
        }
        throw new NotEnoughDataDecoderException("Access out of bounds");
    }

    void skipControlCharactersStandard() {
        for (;;) {
            char c = (char) undecodedChunk.readUnsignedByte();
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                break;
            }
        }
    }

    /**
     * Find the next Multipart Delimiter
     *
     * @param delimiter
     *            delimiter to find
     * @param dispositionStatus
     *            the next getStatus if the delimiter is a start
     * @param closeDelimiterStatus
     *            the next getStatus if the delimiter is a close delimiter
     * @return the next InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    private InterfaceHttpData findMultipartDelimiter(String delimiter, MultiPartStatus dispositionStatus,
            MultiPartStatus closeDelimiterStatus) {
        // --AaB03x or --AaB03x--
        int readerIndex = undecodedChunk.readerIndex();
        try {
            skipControlCharacters();
        } catch (NotEnoughDataDecoderException e1) {
            undecodedChunk.readerIndex(readerIndex);
            return null;
        }
        skipOneLine();
        String newline;
        try {
            newline = readDelimiter(delimiter);
        } catch (NotEnoughDataDecoderException e) {
            undecodedChunk.readerIndex(readerIndex);
            return null;
        }
        if (newline.equals(delimiter)) {
            currentStatus = dispositionStatus;
            return decodeMultipart(dispositionStatus);
        }
        if (newline.equals(delimiter + "--")) {
            // CLOSEDELIMITER or MIXED CLOSEDELIMITER found
            currentStatus = closeDelimiterStatus;
            if (currentStatus == MultiPartStatus.HEADERDELIMITER) {
                // MIXEDCLOSEDELIMITER
                // end of the Mixed part
                currentFieldAttributes = null;
                return decodeMultipart(MultiPartStatus.HEADERDELIMITER);
            }
            return null;
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new ErrorDataDecoderException("No Multipart delimiter found");
    }

    /**
     * Find the next Disposition
     *
     * @return the next InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    private InterfaceHttpData findMultipartDisposition() {
        int readerIndex = undecodedChunk.readerIndex();
        if (currentStatus == MultiPartStatus.DISPOSITION) {
            currentFieldAttributes = new TreeMap<String, Attribute>(CaseIgnoringComparator.INSTANCE);
        }
        // read many lines until empty line with newline found! Store all data
        while (!skipOneLine()) {
            String newline;
            try {
                skipControlCharacters();
                newline = readLine();
            } catch (NotEnoughDataDecoderException e) {
                undecodedChunk.readerIndex(readerIndex);
                return null;
            }
            String[] contents = splitMultipartHeader(newline);
            if (contents[0].equalsIgnoreCase(HttpPostBodyUtil.CONTENT_DISPOSITION)) {
                boolean checkSecondArg;
                if (currentStatus == MultiPartStatus.DISPOSITION) {
                    checkSecondArg = contents[1].equalsIgnoreCase(HttpPostBodyUtil.FORM_DATA);
                } else {
                    checkSecondArg = contents[1].equalsIgnoreCase(HttpPostBodyUtil.ATTACHMENT)
                            || contents[1].equalsIgnoreCase(HttpPostBodyUtil.FILE);
                }
                if (checkSecondArg) {
                    // read next values and store them in the map as Attribute
                    for (int i = 2; i < contents.length; i++) {
                        String[] values = StringUtil.split(contents[i], '=');
                        Attribute attribute;
                        try {
                            String name = cleanString(values[0]);
                            String value = values[1];

                            // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
                            if (HttpPostBodyUtil.FILENAME.equals(name)) {
                                // filename value is quoted string so strip them
                                value = value.substring(1, value.length() - 1);
                            } else {
                                // otherwise we need to clean the value
                                value = cleanString(value);
                            }
                            attribute = factory.createAttribute(request, name, value);
                        } catch (NullPointerException e) {
                            throw new ErrorDataDecoderException(e);
                        } catch (IllegalArgumentException e) {
                            throw new ErrorDataDecoderException(e);
                        }
                        currentFieldAttributes.put(attribute.getName(), attribute);
                    }
                }
            } else if (contents[0].equalsIgnoreCase(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING.toString())) {
                Attribute attribute;
                try {
                    attribute = factory.createAttribute(request, HttpHeaders.Names.CONTENT_TRANSFER_ENCODING.toString(),
                            cleanString(contents[1]));
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                }
                currentFieldAttributes.put(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING.toString(), attribute);
            } else if (contents[0].equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH.toString())) {
                Attribute attribute;
                try {
                    attribute = factory.createAttribute(request, HttpHeaders.Names.CONTENT_LENGTH.toString(),
                            cleanString(contents[1]));
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                }
                currentFieldAttributes.put(HttpHeaders.Names.CONTENT_LENGTH.toString(), attribute);
            } else if (contents[0].equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE.toString())) {
                // Take care of possible "multipart/mixed"
                if (contents[1].equalsIgnoreCase(HttpPostBodyUtil.MULTIPART_MIXED)) {
                    if (currentStatus == MultiPartStatus.DISPOSITION) {
                        String[] values = StringUtil.split(contents[2], '=');
                        multipartMixedBoundary = "--" + values[1];
                        currentStatus = MultiPartStatus.MIXEDDELIMITER;
                        return decodeMultipart(MultiPartStatus.MIXEDDELIMITER);
                    } else {
                        throw new ErrorDataDecoderException("Mixed Multipart found in a previous Mixed Multipart");
                    }
                } else {
                    for (int i = 1; i < contents.length; i++) {
                        if (contents[i].toLowerCase().startsWith(HttpHeaders.Values.CHARSET.toString())) {
                            String[] values = StringUtil.split(contents[i], '=');
                            Attribute attribute;
                            try {
                                attribute = factory.createAttribute(request, HttpHeaders.Values.CHARSET.toString(),
                                        cleanString(values[1]));
                            } catch (NullPointerException e) {
                                throw new ErrorDataDecoderException(e);
                            } catch (IllegalArgumentException e) {
                                throw new ErrorDataDecoderException(e);
                            }
                            currentFieldAttributes.put(HttpHeaders.Values.CHARSET.toString(), attribute);
                        } else {
                            Attribute attribute;
                            try {
                                attribute = factory.createAttribute(request,
                                        cleanString(contents[0]), contents[i]);
                            } catch (NullPointerException e) {
                                throw new ErrorDataDecoderException(e);
                            } catch (IllegalArgumentException e) {
                                throw new ErrorDataDecoderException(e);
                            }
                            currentFieldAttributes.put(attribute.getName(), attribute);
                        }
                    }
                }
            } else {
                throw new ErrorDataDecoderException("Unknown Params: " + newline);
            }
        }
        // Is it a FileUpload
        Attribute filenameAttribute = currentFieldAttributes.get(HttpPostBodyUtil.FILENAME);
        if (currentStatus == MultiPartStatus.DISPOSITION) {
            if (filenameAttribute != null) {
                // FileUpload
                currentStatus = MultiPartStatus.FILEUPLOAD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.FILEUPLOAD);
            } else {
                // Field
                currentStatus = MultiPartStatus.FIELD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.FIELD);
            }
        } else {
            if (filenameAttribute != null) {
                // FileUpload
                currentStatus = MultiPartStatus.MIXEDFILEUPLOAD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.MIXEDFILEUPLOAD);
            } else {
                // Field is not supported in MIXED mode
                throw new ErrorDataDecoderException("Filename not found");
            }
        }
    }

    /**
     * Get the FileUpload (new one or current one)
     *
     * @param delimiter
     *            the delimiter to use
     * @return the InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    protected InterfaceHttpData getFileUpload(String delimiter) {
        // eventually restart from existing FileUpload
        // Now get value according to Content-Type and Charset
        Attribute encoding = currentFieldAttributes.get(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING.toString());
        Charset localCharset = charset;
        // Default
        TransferEncodingMechanism mechanism = TransferEncodingMechanism.BIT7;
        if (encoding != null) {
            String code;
            try {
                code = encoding.getValue().toLowerCase();
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
            if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BIT7.value())) {
                localCharset = HttpPostBodyUtil.US_ASCII;
            } else if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BIT8.value())) {
                localCharset = HttpPostBodyUtil.ISO_8859_1;
                mechanism = TransferEncodingMechanism.BIT8;
            } else if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value())) {
                // no real charset, so let the default
                mechanism = TransferEncodingMechanism.BINARY;
            } else {
                throw new ErrorDataDecoderException("TransferEncoding Unknown: " + code);
            }
        }
        Attribute charsetAttribute = currentFieldAttributes.get(HttpHeaders.Values.CHARSET.toString());
        if (charsetAttribute != null) {
            try {
                localCharset = Charset.forName(charsetAttribute.getValue());
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        }
        if (currentFileUpload == null) {
            Attribute filenameAttribute = currentFieldAttributes.get(HttpPostBodyUtil.FILENAME);
            Attribute nameAttribute = currentFieldAttributes.get(HttpPostBodyUtil.NAME);
            Attribute contentTypeAttribute = currentFieldAttributes.get(HttpHeaders.Names.CONTENT_TYPE);
            if (contentTypeAttribute == null) {
                throw new ErrorDataDecoderException("Content-Type is absent but required");
            }
            Attribute lengthAttribute = currentFieldAttributes.get(HttpHeaders.Names.CONTENT_LENGTH);
            long size;
            try {
                size = lengthAttribute != null ? Long.parseLong(lengthAttribute.getValue()) : 0L;
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            } catch (NumberFormatException e) {
                size = 0;
            }
            try {
                currentFileUpload = factory.createFileUpload(request,
                        cleanString(nameAttribute.getValue()), cleanString(filenameAttribute.getValue()),
                        contentTypeAttribute.getValue(), mechanism.value(), localCharset,
                        size);
            } catch (NullPointerException e) {
                throw new ErrorDataDecoderException(e);
            } catch (IllegalArgumentException e) {
                throw new ErrorDataDecoderException(e);
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        }
        // load data as much as possible
        try {
            readFileUploadByteMultipart(delimiter);
        } catch (NotEnoughDataDecoderException e) {
            // do not change the buffer position
            // since some can be already saved into FileUpload
            // So do not change the currentStatus
            return null;
        }
        if (currentFileUpload.isCompleted()) {
            // ready to load the next one
            if (currentStatus == MultiPartStatus.FILEUPLOAD) {
                currentStatus = MultiPartStatus.HEADERDELIMITER;
                currentFieldAttributes = null;
            } else {
                currentStatus = MultiPartStatus.MIXEDDELIMITER;
                cleanMixedAttributes();
            }
            FileUpload fileUpload = currentFileUpload;
            currentFileUpload = null;
            return fileUpload;
        }
        // do not change the buffer position
        // since some can be already saved into FileUpload
        // So do not change the currentStatus
        return null;
    }

    /**
     * Destroy the {@link HttpPostMultipartRequestDecoder} and release all it resources. After this method
     * was called it is not possible to operate on it anymore.
     */
    @Override
    public void destroy() {
        checkDestroyed();
        cleanFiles();
        destroyed = true;

        if (undecodedChunk != null && undecodedChunk.refCnt() > 0) {
            undecodedChunk.release();
            undecodedChunk = null;
        }

        // release all data which was not yet pulled
        for (int i = bodyListHttpDataRank; i < bodyListHttpData.size(); i++) {
            bodyListHttpData.get(i).release();
        }
    }

    /**
     * Clean all HttpDatas (on Disk) for the current request.
     */
    @Override
    public void cleanFiles() {
        checkDestroyed();

        factory.cleanRequestHttpDatas(request);
    }

    /**
     * Remove the given FileUpload from the list of FileUploads to clean
     */
    @Override
    public void removeHttpDataFromClean(InterfaceHttpData data) {
        checkDestroyed();

        factory.removeHttpDataFromClean(request, data);
    }

    /**
     * Remove all Attributes that should be cleaned between two FileUpload in
     * Mixed mode
     */
    private void cleanMixedAttributes() {
        currentFieldAttributes.remove(HttpHeaders.Values.CHARSET);
        currentFieldAttributes.remove(HttpHeaders.Names.CONTENT_LENGTH);
        currentFieldAttributes.remove(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING);
        currentFieldAttributes.remove(HttpHeaders.Names.CONTENT_TYPE);
        currentFieldAttributes.remove(HttpPostBodyUtil.FILENAME);
    }

    /**
     * Read one line up to the CRLF or LF
     *
     * @return the String from one line
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the readerInder to the previous
     *             value
     */
    private String readLineStandard() {
        int readerIndex = undecodedChunk.readerIndex();
        try {
            ByteBuf line = buffer(64);

            while (undecodedChunk.isReadable()) {
                byte nextByte = undecodedChunk.readByte();
                if (nextByte == HttpConstants.CR) {
                    nextByte = undecodedChunk.readByte();
                    if (nextByte == HttpConstants.LF) {
                        return line.toString(charset);
                    }
                } else if (nextByte == HttpConstants.LF) {
                    return line.toString(charset);
                } else {
                    line.writeByte(nextByte);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to the CRLF or LF
     *
     * @return the String from one line
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the readerInder to the previous
     *             value
     */
    private String readLine() {
        SeekAheadOptimize sao;
        try {
            sao = new SeekAheadOptimize(undecodedChunk);
        } catch (SeekAheadNoBackArrayException e1) {
            return readLineStandard();
        }
        int readerIndex = undecodedChunk.readerIndex();
        try {
            ByteBuf line = buffer(64);

            while (sao.pos < sao.limit) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == HttpConstants.CR) {
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == HttpConstants.LF) {
                            sao.setReadPosition(0);
                            return line.toString(charset);
                        }
                    } else {
                        line.writeByte(nextByte);
                    }
                } else if (nextByte == HttpConstants.LF) {
                    sao.setReadPosition(0);
                    return line.toString(charset);
                } else {
                    line.writeByte(nextByte);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to --delimiter or --delimiter-- and if existing the CRLF
     * or LF Read one line up to --delimiter or --delimiter-- and if existing
     * the CRLF or LF. Note that CRLF or LF are mandatory for opening delimiter
     * (--delimiter) but not for closing delimiter (--delimiter--) since some
     * clients does not include CRLF in this case.
     *
     * @param delimiter
     *            of the form --string, such that '--' is already included
     * @return the String from one line as the delimiter searched (opening or
     *         closing)
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the readerInder to the previous
     *             value
     */
    private String readDelimiterStandard(String delimiter) {
        int readerIndex = undecodedChunk.readerIndex();
        try {
            StringBuilder sb = new StringBuilder(64);
            int delimiterPos = 0;
            int len = delimiter.length();
            while (undecodedChunk.isReadable() && delimiterPos < len) {
                byte nextByte = undecodedChunk.readByte();
                if (nextByte == delimiter.charAt(delimiterPos)) {
                    delimiterPos++;
                    sb.append((char) nextByte);
                } else {
                    // delimiter not found so break here !
                    undecodedChunk.readerIndex(readerIndex);
                    throw new NotEnoughDataDecoderException();
                }
            }
            // Now check if either opening delimiter or closing delimiter
            if (undecodedChunk.isReadable()) {
                byte nextByte = undecodedChunk.readByte();
                // first check for opening delimiter
                if (nextByte == HttpConstants.CR) {
                    nextByte = undecodedChunk.readByte();
                    if (nextByte == HttpConstants.LF) {
                        return sb.toString();
                    } else {
                        // error since CR must be followed by LF
                        // delimiter not found so break here !
                        undecodedChunk.readerIndex(readerIndex);
                        throw new NotEnoughDataDecoderException();
                    }
                } else if (nextByte == HttpConstants.LF) {
                    return sb.toString();
                } else if (nextByte == '-') {
                    sb.append('-');
                    // second check for closing delimiter
                    nextByte = undecodedChunk.readByte();
                    if (nextByte == '-') {
                        sb.append('-');
                        // now try to find if CRLF or LF there
                        if (undecodedChunk.isReadable()) {
                            nextByte = undecodedChunk.readByte();
                            if (nextByte == HttpConstants.CR) {
                                nextByte = undecodedChunk.readByte();
                                if (nextByte == HttpConstants.LF) {
                                    return sb.toString();
                                } else {
                                    // error CR without LF
                                    // delimiter not found so break here !
                                    undecodedChunk.readerIndex(readerIndex);
                                    throw new NotEnoughDataDecoderException();
                                }
                            } else if (nextByte == HttpConstants.LF) {
                                return sb.toString();
                            } else {
                                // No CRLF but ok however (Adobe Flash uploader)
                                // minus 1 since we read one char ahead but
                                // should not
                                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                                return sb.toString();
                            }
                        }
                        // FIXME what do we do here?
                        // either considering it is fine, either waiting for
                        // more data to come?
                        // lets try considering it is fine...
                        return sb.toString();
                    }
                    // only one '-' => not enough
                    // whatever now => error since incomplete
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to --delimiter or --delimiter-- and if existing the CRLF
     * or LF. Note that CRLF or LF are mandatory for opening delimiter
     * (--delimiter) but not for closing delimiter (--delimiter--) since some
     * clients does not include CRLF in this case.
     *
     * @param delimiter
     *            of the form --string, such that '--' is already included
     * @return the String from one line as the delimiter searched (opening or
     *         closing)
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the readerInder to the previous
     *             value
     */
    private String readDelimiter(String delimiter) {
        SeekAheadOptimize sao;
        try {
            sao = new SeekAheadOptimize(undecodedChunk);
        } catch (SeekAheadNoBackArrayException e1) {
            return readDelimiterStandard(delimiter);
        }
        int readerIndex = undecodedChunk.readerIndex();
        int delimiterPos = 0;
        int len = delimiter.length();
        try {
            StringBuilder sb = new StringBuilder(64);
            // check conformity with delimiter
            while (sao.pos < sao.limit && delimiterPos < len) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == delimiter.charAt(delimiterPos)) {
                    delimiterPos++;
                    sb.append((char) nextByte);
                } else {
                    // delimiter not found so break here !
                    undecodedChunk.readerIndex(readerIndex);
                    throw new NotEnoughDataDecoderException();
                }
            }
            // Now check if either opening delimiter or closing delimiter
            if (sao.pos < sao.limit) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == HttpConstants.CR) {
                    // first check for opening delimiter
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == HttpConstants.LF) {
                            sao.setReadPosition(0);
                            return sb.toString();
                        }
                    } else {
                        // error since CR must be followed by LF
                        // delimiter not found so break here !
                        undecodedChunk.readerIndex(readerIndex);
                        throw new NotEnoughDataDecoderException();
                    }
                } else if (nextByte == HttpConstants.LF) {
                    // same first check for opening delimiter where LF used with
                    // no CR
                    sao.setReadPosition(0);
                    return sb.toString();
                } else if (nextByte == '-') {
                    sb.append('-');
                    // second check for closing delimiter
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == '-') {
                            sb.append('-');
                            // now try to find if CRLF or LF there
                            if (sao.pos < sao.limit) {
                                nextByte = sao.bytes[sao.pos++];
                                if (nextByte == HttpConstants.CR) {
                                    if (sao.pos < sao.limit) {
                                        nextByte = sao.bytes[sao.pos++];
                                        if (nextByte == HttpConstants.LF) {
                                            sao.setReadPosition(0);
                                            return sb.toString();
                                        }
                                    } else {
                                        // error CR without LF
                                        // delimiter not found so break here !
                                        undecodedChunk.readerIndex(readerIndex);
                                        throw new NotEnoughDataDecoderException();
                                    }
                                } else if (nextByte == HttpConstants.LF) {
                                    sao.setReadPosition(0);
                                    return sb.toString();
                                } else {
                                    // No CRLF but ok however (Adobe Flash
                                    // uploader)
                                    // minus 1 since we read one char ahead but
                                    // should not
                                    sao.setReadPosition(1);
                                    return sb.toString();
                                }
                            }
                            // FIXME what do we do here?
                            // either considering it is fine, either waiting for
                            // more data to come?
                            // lets try considering it is fine...
                            sao.setReadPosition(0);
                            return sb.toString();
                        }
                        // whatever now => error since incomplete
                        // only one '-' => not enough or whatever not enough
                        // element
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read a FileUpload data as Byte (Binary) and add the bytes directly to the
     * FileUpload. If the delimiter is found, the FileUpload is completed.
     *
     * @throws NotEnoughDataDecoderException
     *             Need more chunks but do not reset the readerInder since some
     *             values will be already added to the FileOutput
     * @throws ErrorDataDecoderException
     *             write IO error occurs with the FileUpload
     */
    private void readFileUploadByteMultipartStandard(String delimiter) {
        int readerIndex = undecodedChunk.readerIndex();
        // found the decoder limit
        boolean newLine = true;
        int index = 0;
        int lastPosition = undecodedChunk.readerIndex();
        boolean found = false;
        while (undecodedChunk.isReadable()) {
            byte nextByte = undecodedChunk.readByte();
            if (newLine) {
                // Check the delimiter
                if (nextByte == delimiter.codePointAt(index)) {
                    index++;
                    if (delimiter.length() == index) {
                        found = true;
                        break;
                    }
                    continue;
                } else {
                    newLine = false;
                    index = 0;
                    // continue until end of line
                    if (nextByte == HttpConstants.CR) {
                        if (undecodedChunk.isReadable()) {
                            nextByte = undecodedChunk.readByte();
                            if (nextByte == HttpConstants.LF) {
                                newLine = true;
                                index = 0;
                                lastPosition = undecodedChunk.readerIndex() - 2;
                            } else {
                                // save last valid position
                                lastPosition = undecodedChunk.readerIndex() - 1;

                                // Unread next byte.
                                undecodedChunk.readerIndex(lastPosition);
                            }
                        }
                    } else if (nextByte == HttpConstants.LF) {
                        newLine = true;
                        index = 0;
                        lastPosition = undecodedChunk.readerIndex() - 1;
                    } else {
                        // save last valid position
                        lastPosition = undecodedChunk.readerIndex();
                    }
                }
            } else {
                // continue until end of line
                if (nextByte == HttpConstants.CR) {
                    if (undecodedChunk.isReadable()) {
                        nextByte = undecodedChunk.readByte();
                        if (nextByte == HttpConstants.LF) {
                            newLine = true;
                            index = 0;
                            lastPosition = undecodedChunk.readerIndex() - 2;
                        } else {
                            // save last valid position
                            lastPosition = undecodedChunk.readerIndex() - 1;

                            // Unread next byte.
                            undecodedChunk.readerIndex(lastPosition);
                        }
                    }
                } else if (nextByte == HttpConstants.LF) {
                    newLine = true;
                    index = 0;
                    lastPosition = undecodedChunk.readerIndex() - 1;
                } else {
                    // save last valid position
                    lastPosition = undecodedChunk.readerIndex();
                }
            }
        }
        ByteBuf buffer = undecodedChunk.copy(readerIndex, lastPosition - readerIndex);
        if (found) {
            // found so lastPosition is correct and final
            try {
                currentFileUpload.addContent(buffer, true);
                // just before the CRLF and delimiter
                undecodedChunk.readerIndex(lastPosition);
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        } else {
            // possibly the delimiter is partially found but still the last
            // position is OK
            try {
                currentFileUpload.addContent(buffer, false);
                // last valid char (not CR, not LF, not beginning of delimiter)
                undecodedChunk.readerIndex(lastPosition);
                throw new NotEnoughDataDecoderException();
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        }
    }

    /**
     * Read a FileUpload data as Byte (Binary) and add the bytes directly to the
     * FileUpload. If the delimiter is found, the FileUpload is completed.
     *
     * @throws NotEnoughDataDecoderException
     *             Need more chunks but do not reset the readerInder since some
     *             values will be already added to the FileOutput
     * @throws ErrorDataDecoderException
     *             write IO error occurs with the FileUpload
     */
    private void readFileUploadByteMultipart(String delimiter) {
        SeekAheadOptimize sao;
        try {
            sao = new SeekAheadOptimize(undecodedChunk);
        } catch (SeekAheadNoBackArrayException e1) {
            readFileUploadByteMultipartStandard(delimiter);
            return;
        }
        int readerIndex = undecodedChunk.readerIndex();
        // found the decoder limit
        boolean newLine = true;
        int index = 0;
        int lastrealpos = sao.pos;
        int lastPosition;
        boolean found = false;

        while (sao.pos < sao.limit) {
            byte nextByte = sao.bytes[sao.pos++];
            if (newLine) {
                // Check the delimiter
                if (nextByte == delimiter.codePointAt(index)) {
                    index++;
                    if (delimiter.length() == index) {
                        found = true;
                        break;
                    }
                    continue;
                } else {
                    newLine = false;
                    index = 0;
                    // continue until end of line
                    if (nextByte == HttpConstants.CR) {
                        if (sao.pos < sao.limit) {
                            nextByte = sao.bytes[sao.pos++];
                            if (nextByte == HttpConstants.LF) {
                                newLine = true;
                                index = 0;
                                lastrealpos = sao.pos - 2;
                            } else {
                                // unread next byte
                                sao.pos--;

                                // save last valid position
                                lastrealpos = sao.pos;
                            }
                        }
                    } else if (nextByte == HttpConstants.LF) {
                        newLine = true;
                        index = 0;
                        lastrealpos = sao.pos - 1;
                    } else {
                        // save last valid position
                        lastrealpos = sao.pos;
                    }
                }
            } else {
                // continue until end of line
                if (nextByte == HttpConstants.CR) {
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == HttpConstants.LF) {
                            newLine = true;
                            index = 0;
                            lastrealpos = sao.pos - 2;
                        } else {
                            // unread next byte
                            sao.pos--;

                            // save last valid position
                            lastrealpos = sao.pos;
                        }
                    }
                } else if (nextByte == HttpConstants.LF) {
                    newLine = true;
                    index = 0;
                    lastrealpos = sao.pos - 1;
                } else {
                    // save last valid position
                    lastrealpos = sao.pos;
                }
            }
        }
        lastPosition = sao.getReadPosition(lastrealpos);
        ByteBuf buffer = undecodedChunk.copy(readerIndex, lastPosition - readerIndex);
        if (found) {
            // found so lastPosition is correct and final
            try {
                currentFileUpload.addContent(buffer, true);
                // just before the CRLF and delimiter
                undecodedChunk.readerIndex(lastPosition);
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        } else {
            // possibly the delimiter is partially found but still the last
            // position is OK
            try {
                currentFileUpload.addContent(buffer, false);
                // last valid char (not CR, not LF, not beginning of delimiter)
                undecodedChunk.readerIndex(lastPosition);
                throw new NotEnoughDataDecoderException();
            } catch (IOException e) {
                throw new ErrorDataDecoderException(e);
            }
        }
    }

    /**
     * Load the field value from a Multipart request
     *
     * @throws NotEnoughDataDecoderException
     *             Need more chunks
     * @throws ErrorDataDecoderException
     */
    private void loadFieldMultipartStandard(String delimiter) {
        int readerIndex = undecodedChunk.readerIndex();
        try {
            // found the decoder limit
            boolean newLine = true;
            int index = 0;
            int lastPosition = undecodedChunk.readerIndex();
            boolean found = false;
            while (undecodedChunk.isReadable()) {
                byte nextByte = undecodedChunk.readByte();
                if (newLine) {
                    // Check the delimiter
                    if (nextByte == delimiter.codePointAt(index)) {
                        index++;
                        if (delimiter.length() == index) {
                            found = true;
                            break;
                        }
                        continue;
                    } else {
                        newLine = false;
                        index = 0;
                        // continue until end of line
                        if (nextByte == HttpConstants.CR) {
                            if (undecodedChunk.isReadable()) {
                                nextByte = undecodedChunk.readByte();
                                if (nextByte == HttpConstants.LF) {
                                    newLine = true;
                                    index = 0;
                                    lastPosition = undecodedChunk.readerIndex() - 2;
                                }
                            }
                        } else if (nextByte == HttpConstants.LF) {
                            newLine = true;
                            index = 0;
                            lastPosition = undecodedChunk.readerIndex() - 1;
                        } else {
                            lastPosition = undecodedChunk.readerIndex();
                        }
                    }
                } else {
                    // continue until end of line
                    if (nextByte == HttpConstants.CR) {
                        if (undecodedChunk.isReadable()) {
                            nextByte = undecodedChunk.readByte();
                            if (nextByte == HttpConstants.LF) {
                                newLine = true;
                                index = 0;
                                lastPosition = undecodedChunk.readerIndex() - 2;
                            }
                        }
                    } else if (nextByte == HttpConstants.LF) {
                        newLine = true;
                        index = 0;
                        lastPosition = undecodedChunk.readerIndex() - 1;
                    } else {
                        lastPosition = undecodedChunk.readerIndex();
                    }
                }
            }
            if (found) {
                // found so lastPosition is correct
                // but position is just after the delimiter (either close
                // delimiter or simple one)
                // so go back of delimiter size
                try {
                    currentAttribute.addContent(
                            undecodedChunk.copy(readerIndex, lastPosition - readerIndex), true);
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
                undecodedChunk.readerIndex(lastPosition);
            } else {
                try {
                    currentAttribute.addContent(
                            undecodedChunk.copy(readerIndex, lastPosition - readerIndex), false);
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
                undecodedChunk.readerIndex(lastPosition);
                throw new NotEnoughDataDecoderException();
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
    }

    /**
     * Load the field value from a Multipart request
     *
     * @throws NotEnoughDataDecoderException
     *             Need more chunks
     * @throws ErrorDataDecoderException
     */
    private void loadFieldMultipart(String delimiter) {
        SeekAheadOptimize sao;
        try {
            sao = new SeekAheadOptimize(undecodedChunk);
        } catch (SeekAheadNoBackArrayException e1) {
            loadFieldMultipartStandard(delimiter);
            return;
        }
        int readerIndex = undecodedChunk.readerIndex();
        try {
            // found the decoder limit
            boolean newLine = true;
            int index = 0;
            int lastPosition;
            int lastrealpos = sao.pos;
            boolean found = false;

            while (sao.pos < sao.limit) {
                byte nextByte = sao.bytes[sao.pos++];
                if (newLine) {
                    // Check the delimiter
                    if (nextByte == delimiter.codePointAt(index)) {
                        index++;
                        if (delimiter.length() == index) {
                            found = true;
                            break;
                        }
                        continue;
                    } else {
                        newLine = false;
                        index = 0;
                        // continue until end of line
                        if (nextByte == HttpConstants.CR) {
                            if (sao.pos < sao.limit) {
                                nextByte = sao.bytes[sao.pos++];
                                if (nextByte == HttpConstants.LF) {
                                    newLine = true;
                                    index = 0;
                                    lastrealpos = sao.pos - 2;
                                }
                            }
                        } else if (nextByte == HttpConstants.LF) {
                            newLine = true;
                            index = 0;
                            lastrealpos = sao.pos - 1;
                        } else {
                            lastrealpos = sao.pos;
                        }
                    }
                } else {
                    // continue until end of line
                    if (nextByte == HttpConstants.CR) {
                        if (sao.pos < sao.limit) {
                            nextByte = sao.bytes[sao.pos++];
                            if (nextByte == HttpConstants.LF) {
                                newLine = true;
                                index = 0;
                                lastrealpos = sao.pos - 2;
                            }
                        }
                    } else if (nextByte == HttpConstants.LF) {
                        newLine = true;
                        index = 0;
                        lastrealpos = sao.pos - 1;
                    } else {
                        lastrealpos = sao.pos;
                    }
                }
            }
            lastPosition = sao.getReadPosition(lastrealpos);
            if (found) {
                // found so lastPosition is correct
                // but position is just after the delimiter (either close
                // delimiter or simple one)
                // so go back of delimiter size
                try {
                    currentAttribute.addContent(
                            undecodedChunk.copy(readerIndex, lastPosition - readerIndex), true);
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
                undecodedChunk.readerIndex(lastPosition);
            } else {
                try {
                    currentAttribute.addContent(
                            undecodedChunk.copy(readerIndex, lastPosition - readerIndex), false);
                } catch (IOException e) {
                    throw new ErrorDataDecoderException(e);
                }
                undecodedChunk.readerIndex(lastPosition);
                throw new NotEnoughDataDecoderException();
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
    }

    /**
     * Clean the String from any unallowed character
     *
     * @return the cleaned String
     */
    private static String cleanString(String field) {
        StringBuilder sb = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char nextChar = field.charAt(i);
            if (nextChar == HttpConstants.COLON) {
                sb.append(HttpConstants.SP);
            } else if (nextChar == HttpConstants.COMMA) {
                sb.append(HttpConstants.SP);
            } else if (nextChar == HttpConstants.EQUALS) {
                sb.append(HttpConstants.SP);
            } else if (nextChar == HttpConstants.SEMICOLON) {
                sb.append(HttpConstants.SP);
            } else if (nextChar == HttpConstants.HT) {
                sb.append(HttpConstants.SP);
            } else if (nextChar == HttpConstants.DOUBLE_QUOTE) {
                // nothing added, just removes it
            } else {
                sb.append(nextChar);
            }
        }
        return sb.toString().trim();
    }

    /**
     * Skip one empty line
     *
     * @return True if one empty line was skipped
     */
    private boolean skipOneLine() {
        if (!undecodedChunk.isReadable()) {
            return false;
        }
        byte nextByte = undecodedChunk.readByte();
        if (nextByte == HttpConstants.CR) {
            if (!undecodedChunk.isReadable()) {
                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                return false;
            }
            nextByte = undecodedChunk.readByte();
            if (nextByte == HttpConstants.LF) {
                return true;
            }
            undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 2);
            return false;
        }
        if (nextByte == HttpConstants.LF) {
            return true;
        }
        undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
        return false;
    }

    /**
     * Split one header in Multipart
     *
     * @return an array of String where rank 0 is the name of the header,
     *         follows by several values that were separated by ';' or ','
     */
    private static String[] splitMultipartHeader(String sb) {
        ArrayList<String> headers = new ArrayList<String>(1);
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;
        nameStart = HttpPostBodyUtil.findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < sb.length(); nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }
        for (colonEnd = nameEnd; colonEnd < sb.length(); colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }
        valueStart = HttpPostBodyUtil.findNonWhitespace(sb, colonEnd);
        valueEnd = HttpPostBodyUtil.findEndOfString(sb);
        headers.add(sb.substring(nameStart, nameEnd));
        String svalue = sb.substring(valueStart, valueEnd);
        String[] values;
        if (svalue.indexOf(';') >= 0) {
            values = StringUtil.split(svalue, ';');
        } else {
            values = StringUtil.split(svalue, ',');
        }
        for (String value : values) {
            headers.add(value.trim());
        }
        String[] array = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            array[i] = headers.get(i);
        }
        return array;
    }
}
