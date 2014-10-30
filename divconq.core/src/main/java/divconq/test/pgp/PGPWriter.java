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
package divconq.test.pgp;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.bcpg.ContainedPacket;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.PBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import divconq.hub.Hub;
import divconq.lang.chars.Utf8Encoder;
import divconq.pgp.PGPUtil;
import divconq.util.HexUtil;

/**
 * For use with simple scenario where you want to fill buffers containing 
 * a single encrypted file.  Give us a key and we'll AES-256 encrypt
 * your content with MDC integrity protection (SHA-1).
 * 
 * Hand us buffers of data or a stream - make sure the stream is small
 * though since everything you give us will go into memory.  
 * 
 * Primary methods are: open, close, writeBuffer and writeStream.
 * 
 * @author Andy White
 */

/*
 * TODO example and Utility call for above - File in to File out
 * 
 * With no compression
 * 
:pubkey enc packet: version 3, algo 1, keyid D983468282E667B0
	data: [2047 bits]

user: "Andy White <awhite@filetransferconsulting.com>"
2048-bit RSA key, ID 82E667B0, created 2014-10-20

:encrypted data packet:
	length: unknown
	mdc_method: 2
	
gpg: encrypted with 2048-bit RSA key, ID 82E667B0, created 2014-10-20
      "Andy White <awhite@filetransferconsulting.com>"
      
:literal data packet:
	mode b (62), created 1414169807, name="story.xml",
	raw data: unknown length
 * 
 * 
 * 
 * with compression
 * 
:pubkey enc packet: version 3, algo 1, keyid D983468282E667B0
	data: [2047 bits]

user: "Andy White <awhite@filetransferconsulting.com>"
2048-bit RSA key, ID 82E667B0, created 2014-10-20

:encrypted data packet:
	length: 47110
	mdc_method: 2
	
gpg: encrypted with 2048-bit RSA key, ID 82E667B0, created 2014-10-20
      "Andy White <awhite@filetransferconsulting.com>"
:compressed packet: algo=1

:literal data packet:
	mode b (62), created 1413810619, name="story.xml",
	raw data: unknown length
	 
 * 
 */

public class PGPWriter {
    static public final int BUF_SIZE_POWER = 16; // 2^16 size buffer on long files
    
    protected String fileName = "temp.bin";
    protected long modificationTime = System.currentTimeMillis();
    
    protected SecureRandom rand = new SecureRandom();
    protected int algorithm = 0;
    protected byte[] key = null;
    protected Cipher cipher = null;
    
    protected ByteBuf out = null;
    
    protected List<ByteBuf> readyBuffers = new ArrayList<>();
    
    /*
     * Create a stream representing a general packet.
     * 
     * @param out buffer to write to
     * @param tag type of packet
     * @param length expected content length
     * @throws IOException
     */
    public void startGeneralPacket(int tag, long length) throws IOException {
        int hdr = 0x80;
        hdr |= 0x40 | tag;
        
        out.writeByte(hdr);
        
        this.writeNewPacketLength(length);
    }
    
    /*
     * Create a new style partial input stream buffered into chunks.
     * 
     * @param out buffer to write to
     * @param tag packet tag.
     * @param buffer size of chunks making up the packet.
     * @throws IOException
     */
    public void startPartialPacket(int tag) throws IOException {
        int hdr = 0xC0 | tag;
        
        // packet tag
        out.writeByte(hdr);
        
        System.out.println("Started " + tag + " which is " + hdr + " hex: " + HexUtil.charToHex(hdr));
        
        // unknown length
        //this.out.writeZero(1);
        
        /*
        this.partialBuffer = new byte[64 * 1024];		// TODO configure or something, use bytebuf
        
        int length = this.partialBuffer.length;
        
        for (this.partialPower = 0; length != 1; this.partialPower++)
            length >>>= 1;
        
        if (this.partialPower > 30)
            throw new IOException("Buffer cannot be greater than 2^30 in length.");
        
        this.partialBufferLength = 1 << this.partialPower;
        this.partialOffset = 0;
        */
    }
    
    public void writeNewPacketLength(long bodyLen) throws IOException {
        if (bodyLen < 192) {
            out.writeByte((int) bodyLen);
        }
        else if (bodyLen <= 8383) {
            bodyLen -= 192;
                    
            out.writeByte((int)(((bodyLen >> 8) & 0xff) + 192));
            out.writeByte((int)bodyLen);
        }
        else {
            out.writeByte(0xff);
            out.writeByte((int)(bodyLen >> 24));
            out.writeByte((int)(bodyLen >> 16));
            out.writeByte((int)(bodyLen >> 8));
            out.writeByte((int)bodyLen);
        }
    }
    
    /*  NOT NEEDED
    public void writePacket(int tag, byte[] body) throws IOException {
        this.writeHeader(tag, false, body.length);
        out.writeBytes(body);
    }
    */
    
    /* REVIEW
    public void finishPartial() throws IOException {
        if (this.partialBuffer != null) {
            this.partialFlush(true);
            this.partialBuffer = null;
        }
    }
    
    public void partialFlush(boolean isLast) throws IOException {
        if (isLast) {
            this.writeNewPacketLength(this.partialOffset);
            out.writeBytes(this.partialBuffer, 0, this.partialOffset);
        }
        else {
            out.writeByte(0xE0 | this.partialPower);
            out.writeBytes(this.partialBuffer, 0, this.partialBufferLength);
        }
        
        this.partialOffset = 0;
    }
    
    public void writePartial(byte b) throws IOException {
        if (this.partialOffset == this.partialBufferLength)
            this.partialFlush(false);
        
        this.partialBuffer[partialOffset++] = b;
    }
    
    public void writePartial(byte[] buf, int off, int len) throws IOException {
        if (this.partialOffset == this.partialBufferLength)
            this.partialFlush(false);
        
        if (len <= (this.partialBufferLength - this.partialOffset)) {
            System.arraycopy(buf, off, this.partialBuffer, this.partialOffset, len);
            this.partialOffset += len;
        }
        else {
            System.arraycopy(buf, off, this.partialBuffer, this.partialOffset, this.partialBufferLength - this.partialOffset);
            
            off += this.partialBufferLength - this.partialOffset;
            len -= this.partialBufferLength - this.partialOffset;
            
            this.partialFlush(false);
            
            while (len > this.partialBufferLength) {
                System.arraycopy(buf, off, this.partialBuffer, 0, this.partialBufferLength);
                
                off += this.partialBufferLength;
                len -= this.partialBufferLength;
                
                this.partialFlush(false);
            }

            System.arraycopy(buf, off, this.partialBuffer, 0, len);
            
            this.partialOffset += len;
        }
    }
    
    public void write(int b) throws IOException {
        if (this.partialBuffer != null)
            this.writePartial((byte)b);
        else
            out.writeByte(b);
    }
    
    public void write(byte[] bytes, int off, int len) throws IOException {
        if (this.partialBuffer != null)
            this.writePartial(bytes, off, len);
        else
            out.writeBytes(bytes, off, len);
    }
    */
    
    public void writeStream(InputStream in) throws IOException {
    	while (true) {
    		/*  TODO
    		byte[]
    		
            byte[] any = this.cipher.update(inLineIv);
            
            if (any != null)
            	this.out.writeBytes(any);		// we may include this in digest, TODO review
    		*/
    		
	    	int avail = this.out.writableBytes();
	    	int actual = this.out.writeBytes(in, avail);
	    	
	    	if (this.out.writableBytes() == 0)
	    		this.allocNextBuffer();
	    	
	    	if (actual < avail)
	    		break;
    	}
    	
        in.close();
    }
    
    /* consider this in some future improvement
    protected void writeObject(BCPGObject o) throws IOException {
        out.writeBytes(o.getEncoded());		// TODO this is inefficient, fix someday.
    }
    */
    
    /*
     * this is for non-literal data sections 
     */
    protected void writePacket(ContainedPacket p) throws IOException {
        byte[] encoded = p.getEncoded();		// TODO this is inefficient, fix someday.
        
        this.ensureBuffer(encoded.length);
        
        this.out.writeBytes(encoded);
    }
    
    public void ensureBuffer(int size) {
    	if ((this.out == null) || (this.out.writableBytes() < size)) 
    		this.allocNextBuffer();
    }
    
    public void allocNextBuffer() {
    	if (this.out != null)
    		this.readyBuffers.add(this.out);
        
    	// buffer must be no larger than 1 GB and should probably be at least 4 KB so we can fit initial sections (packets) 
    	// at top of file
    	
		this.out = Hub.instance.getBufferAllocator().heapBuffer(256 * 1024);	// TODO config / smaller!!!
    }

    /**
     * Start here with an open for a given recipient (public) key.
     * 
     * @param key encrypt to this key
     * @throws IOException if there are problems encoding the packets
     * @throws PGPException problems with key or crypto
     */
	public void open(PGPPublicKey key) throws IOException, PGPException {
    	this.open(PGPEncryptedData.AES_256, new JcePublicKeyKeyEncryptionMethodGenerator(key));
	}

	public void open(int algorithm, PGPKeyEncryptionMethodGenerator... methods) 
	   		throws IOException, PGPException, IllegalStateException
	{
		if (methods.length == 0)
		    throw new IllegalStateException("no encryption methods specified");
		
		this.algorithm = algorithm;
		
		// *******************************************************************
		// public key packet
		// *******************************************************************
		
		// TODO this condition untested and perhaps not helpful, review (PBE - password based encryption)
		if ((methods.length == 1) && (methods[0] instanceof PBEKeyEncryptionMethodGenerator)) {
		    PBEKeyEncryptionMethodGenerator m = (PBEKeyEncryptionMethodGenerator)methods[0];
		    
		    this.key = m.getKey(algorithm);
		    
		    this.writePacket(m.generate(algorithm, null));
		}
		else  {
			this.key = org.bouncycastle.openpgp.PGPUtil.makeRandomKey(algorithm, rand);
		    
		    byte[] sessionInfo = this.createSessionInfo();
		
		    for (int i = 0; i < methods.length; i++) {
		        PGPKeyEncryptionMethodGenerator m = (PGPKeyEncryptionMethodGenerator)methods[i];
		        
		        this.writePacket(m.generate(algorithm, sessionInfo));
		    }
		}
		
		int packet1 = this.out.writerIndex();
		
		System.out.println("packet 1: " + packet1 + " final bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet1 - 5, 5));
		
		// *******************************************************************
		// encrypt packet, add IV to 
		// *******************************************************************
		
        try
        {
            String cName = PGPUtil.getSymmetricCipherName(algorithm) + "/CFB/NoPadding";

        	DefaultJcaJceHelper helper = new DefaultJcaJceHelper();
        	
            this.cipher = helper.createCipher(cName);
            
            byte[] iv = new byte[this.cipher.getBlockSize()];

        	this.cipher.init(Cipher.ENCRYPT_MODE, PGPUtil.makeSymmetricKey(algorithm, this.key), new IvParameterSpec(iv));
    		
            //
            // we have to add block size + 2 for the generated IV and + 1 + 22 if integrity protected
            //
        	
        	this.ensureBuffer(this.cipher.getBlockSize() + 2 + 1 + 22);
        	
        	this.startPartialPacket(PacketTags.SYM_ENC_INTEGRITY_PRO);   //, this.cipher.getBlockSize() + 2 + 1 + 22);
        	
        	this.out.writeByte(1);        // version number
            
            byte[] inLineIv = new byte[this.cipher.getBlockSize() + 2];
            
            this.rand.nextBytes(inLineIv);
            
            inLineIv[inLineIv.length - 1] = inLineIv[inLineIv.length - 3];
            inLineIv[inLineIv.length - 2] = inLineIv[inLineIv.length - 4];

            // TODO
            byte[] any = this.cipher.update(inLineIv);
            
            if (any != null)
            	this.out.writeBytes(any);		// we may include this in digest, TODO review
        }
        catch (InvalidKeyException e)
        {
            throw new PGPException("invalid key: " + e.getMessage(), e);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new PGPException("imvalid algorithm parameter: " + e.getMessage(), e);
        }
        catch (GeneralSecurityException e)
        {
            throw new PGPException("cannot create cipher: " + e.getMessage(), e);
        }
		
		int packet2 = this.out.writerIndex();
		
		System.out.println("packet 2: first bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet1, 25));
		System.out.println("packet 2: " + packet2 + " final bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet2 - 5, 5));
        
		// *******************************************************************
        // TODO compress packet, if any
		// *******************************************************************
		
		int packet3 = this.out.writerIndex();
		
		//System.out.println("packet 3: first bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet2, 25));
		//System.out.println("packet 3: " + packet3 + " final bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet3 - 5, 5));
        
		// *******************************************************************
        // literal packet start
		// *******************************************************************
	    
    	this.startPartialPacket(PacketTags.LITERAL_DATA);
    	
        byte[] encName = Utf8Encoder.encode(this.fileName);

        // TODO don't hard code
        int len = 1 + 1 + encName.length + 4 + 99;		// type + name length + name + mod time + file content
        
        //out.writeByte(len);
        
        this.writeNewPacketLength(len);
        
        out.writeByte(PGPLiteralData.BINARY);
        
        out.writeByte((byte)encName.length);

        out.writeBytes(encName);

        long modDate = this.modificationTime / 1000;

        out.writeByte((byte)(modDate >> 24));
        out.writeByte((byte)(modDate >> 16));
        out.writeByte((byte)(modDate >> 8));
        out.writeByte((byte)(modDate));
		
		int packet4 = this.out.writerIndex();
		
		System.out.println("packet 4: first bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet3, 25));
		System.out.println("packet 4: " + packet4 + " final bytes: " + HexUtil.bufferToHex(this.out.array(), this.out.arrayOffset() + packet4 - 5, 5));
		
        // TODO new SHA1PGPDigestCalculator();
        //if (digestCalc != null)
        //   genOut = new TeeOutputStream(digestCalc.getOutputStream(), genOut);
	}
	
    protected byte[] createSessionInfo() {
        byte[] sessionInfo = new byte[this.key.length + 3];

        // add algorithm
        sessionInfo[0] = (byte) this.algorithm;
        
        // add key
        System.arraycopy(this.key, 0, sessionInfo, 1, this.key.length);
        
        // add checksum 
        int check = 0;

        for (int i = 1; i != sessionInfo.length - 2; i++)
            check += sessionInfo[i] & 0xff;

        sessionInfo[sessionInfo.length - 2] = (byte)(check >> 8);
        sessionInfo[sessionInfo.length - 1] = (byte)(check);

        return sessionInfo;
    }
    
    /*
     * Finish writing out the current packet without closing the underlying stream.
     */
    public void close() throws IOException {
    	// TODO review this.finishPartial();
    	
    	this.ensureBuffer(22);
    	
		this.startGeneralPacket(PacketTags.MOD_DETECTION_CODE, 20);
		
		/*
        byte[] dig = this.dc.getDigest();
		 */
		
        this.out.writeBytes(new byte[20]);	// TODO
        
        this.readyBuffers.add(this.out);
        this.out = null;
    }
    
	public ByteBuf nextReadyBuffer() {
		if (this.readyBuffers.size() > 0)
			return this.readyBuffers.remove(0);
		
		return null;
	}
}
