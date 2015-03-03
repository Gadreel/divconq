package divconq.web.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.IDN;
import java.util.List;
import java.util.Locale;

import divconq.net.ssl.SslHandler;
import divconq.web.WebSiteManager;

/**
 * <p>Enables <a href="https://tools.ietf.org/html/rfc3546#section-3.1">SNI
 * (Server Name Indication)</a> extension for server side SSL. For clients
 * support SNI, the server could have multiple host name bound on a single IP.
 * The client will send host name in the handshake data so server could decide
 * which certificate to choose for the host name. </p>
 */
public class SniHandler extends ByteToMessageDecoder {
    public static final int SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC = 20;
    public static final int SSL_CONTENT_TYPE_ALERT = 21;
    public static final int SSL_CONTENT_TYPE_HANDSHAKE = 22;
    public static final int SSL_CONTENT_TYPE_APPLICATION_DATA = 23;

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(SniHandler.class);
    protected boolean handshaken = false;
    protected SslContextFactory selectedContext = null;
    protected WebSiteManager man = null;
    
    public SniHandler(WebSiteManager man) {
    	this.man = man;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!handshaken && in.readableBytes() >= 5) {
            String hostname = sniHostNameFromHandshakeInfo(in);
            
            if (hostname != null) 
                hostname = IDN.toASCII(hostname, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.US);

            // the mapping will return default context when this.hostname is null
            this.selectedContext = this.man.findSslContextFactory(hostname);
        }

        if (handshaken) {
            SslHandler sslHandler = new SslHandler(this.selectedContext.getServerEngine()); 
            ctx.pipeline().replace("ssl", "ssl", sslHandler);
        }
    }

    private String sniHostNameFromHandshakeInfo(ByteBuf in) {
        int readerIndex = in.readerIndex();
        try {
            int command = in.getUnsignedByte(readerIndex);

            // tls, but not handshake command
            switch (command) {
                case SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC:
                case SSL_CONTENT_TYPE_ALERT:
                case SSL_CONTENT_TYPE_APPLICATION_DATA:
                    return null;
                case SSL_CONTENT_TYPE_HANDSHAKE:
                    break;
                default:
                    //not tls or sslv3, do not try sni
                    handshaken = true;
                    return null;
            }

            int majorVersion = in.getUnsignedByte(readerIndex + 1);

            // SSLv3 or TLS
            if (majorVersion == 3) {

                int packetLength = in.getUnsignedShort(readerIndex + 3) + 5;

                if (in.readableBytes() >= packetLength) {
                    // decode the ssl client hello packet
                    // we have to skip some var-length fields
                    int offset = readerIndex + 43;

                    int sessionIdLength = in.getUnsignedByte(offset);
                    offset += sessionIdLength + 1;

                    int cipherSuitesLength = in.getUnsignedShort(offset);
                    offset += cipherSuitesLength + 2;

                    int compressionMethodLength = in.getUnsignedByte(offset);
                    offset += compressionMethodLength + 1;

                    int extensionsLength = in.getUnsignedShort(offset);
                    offset += 2;
                    int extensionsLimit = offset + extensionsLength;

                    while (offset < extensionsLimit) {
                        int extensionType = in.getUnsignedShort(offset);
                        offset += 2;

                        int extensionLength = in.getUnsignedShort(offset);
                        offset += 2;

                        // SNI
                        if (extensionType == 0) {
                            handshaken = true;
                            int serverNameType = in.getUnsignedByte(offset + 2);
                            if (serverNameType == 0) {
                                int serverNameLength = in.getUnsignedShort(offset + 3);
                                return in.toString(offset + 5, serverNameLength,
                                        CharsetUtil.UTF_8);
                            } else {
                                // invalid enum value
                                return null;
                            }
                        }

                        offset += extensionLength;
                    }

                    handshaken = true;
                    return null;
                } else {
                    // client hello incomplete
                    return null;
                }
            } else {
                handshaken = true;
                return null;
            }
        } catch (Throwable e) {
            // unexpected encoding, ignore sni and use default
            if (logger.isDebugEnabled()) {
                logger.debug("Unexpected client hello packet: " + ByteBufUtil.hexDump(in), e);
            }
            handshaken = true;
            return null;
        }
    }
}