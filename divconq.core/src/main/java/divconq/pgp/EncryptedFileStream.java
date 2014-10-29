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
package divconq.pgp;

import io.netty.buffer.ByteBuf;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.bcpg.ContainedPacket;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.PBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import divconq.hub.Hub;
import divconq.lang.chars.Utf8Encoder;
import divconq.pgp.PGPUtil;

public class EncryptedFileStream {
	public static final int MAX_PACKET_SIZE = 32 * 1024;
	public static final int MAX_PARTIAL_LEN = 0xEF;
	
    protected String fileName = "temp.bin";
    protected long modificationTime = System.currentTimeMillis();
    //protected PGPPublicKey pubKey = null;
    protected int algorithm = PGPEncryptedData.AES_256;
    protected List<PGPKeyEncryptionMethodGenerator> methods = new ArrayList<>();
    
    protected boolean writeFirst = false;
    protected boolean isClosed = false;
    protected SecureRandom rand = new SecureRandom();
    protected byte[] key = null;
    protected Cipher cipher = null;
    protected MessageDigest digest = null;
    
    protected int packetsize = 0;
    protected int packetpos = 0;
    protected ByteBuf packetbuf = null;
    
    protected ByteBuf out = null;    
    protected List<ByteBuf> readyBuffers = new ArrayList<>();

    public void setFileName(String fileName) {
		this.fileName = fileName;
	}
    
    public String getFileName() {
		return this.fileName;
	}
    
    public boolean isClosed() {
    	return this.isClosed;
    }
    
    public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}
    
    public long getModificationTime() {
		return this.modificationTime;
	}
    
    public void setAlgorithm(int algorithm) {
		this.algorithm = algorithm;
	}
    
    public int getAlgorithm() {
		return this.algorithm;
	}
    
    public void addPublicKey(PGPPublicKey pubKey) {
    	this.methods.add(new JcePublicKeyKeyEncryptionMethodGenerator(pubKey));
	}
    
    public void addMethod(PGPKeyEncryptionMethodGenerator v) {
    	this.methods.add(v);
	}
    
    public void ensureBuffer(int size) {
    	if ((this.out == null) || (this.out.writableBytes() < size)) 
    		this.allocNextBuffer(size);
    }
    
    public void allocNextBuffer() {
    	this.allocNextBuffer(1);		// use default
    }
    
    public void allocNextBuffer(int size) {
    	if (this.out != null)
    		this.readyBuffers.add(this.out);
        
    	// buffer must be no larger than 1 GB and should probably be at least 4 KB so we can fit initial sections (packets) 
    	// at top of file
    	
    	// create buffers that are at least 32KB in size
    	
		this.out = Hub.instance.getBufferAllocator().heapBuffer(Math.max(size, 32 * 1024));		// TODO config 
    }
    
	public ByteBuf nextReadyBuffer() {
		if (this.readyBuffers.size() > 0)
			return this.readyBuffers.remove(0);
		
		return null;
	}
    
    public void loadPublicKey(Path keyring) throws IOException, PGPException {
    	// TODO move some of this to dcPGPUtil
        PGPPublicKey pubKey = null;
        
        InputStream keyIn = new BufferedInputStream(new FileInputStream(keyring.toFile()));
        
    	PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(org.bouncycastle.openpgp.PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //

        @SuppressWarnings("rawtypes")
		Iterator keyRingIter = pgpPub.getKeyRings();
        
        while (keyRingIter.hasNext() && (pubKey == null)) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing)keyRingIter.next();

            @SuppressWarnings("rawtypes")
			Iterator keyIter = keyRing.getPublicKeys();
            
            while (keyIter.hasNext() && (pubKey == null)) {
                PGPPublicKey key = (PGPPublicKey)keyIter.next();

                if (key.isEncryptionKey())
                	pubKey = key;
            }
        }

        if (pubKey == null)
        	throw new IllegalArgumentException("Can't find encryption key in key ring.");
    	
		this.methods.add(new JcePublicKeyKeyEncryptionMethodGenerator(pubKey));
	}
    
    // means writing to section that is ciphered only and never compressed, use writeCompressed 
    // most of the time even if not using compression
    public void writeData(byte[] bytes, int offset, int len) {
    	// the first time this is called we need to write headers - those headers
    	// call into this method so clear flag immediately
    	if (!this.writeFirst) {
    		this.writeFirst = true;
    		this.writeFirstLiteral(len);
    	}
    	
    	int remaining = len;
    	int avail = this.packetsize - this.packetpos;
    	
    	// packetbuf may have data that has not yet been processed, so if we are doing any writes
    	// we need to write the packet buffer first
    	ByteBuf pbb = this.packetbuf;
    	
    	if (pbb != null) {
	        int bbremaining = pbb.readableBytes();
	        
	        // only write if there is space available in current packet or if we have a total
	        // amount of data larger than max packet size
    		while ((bbremaining > 0) && ((avail > 0) || (bbremaining + remaining) >= MAX_PACKET_SIZE)) {
            	// out of current packet space? create more packets
            	if (avail == 0) {
	            	this.packetsize = MAX_PACKET_SIZE;
	            	this.packetpos = 0;
	    	        
	    	        this.writeDataInternal((byte) MAX_PARTIAL_LEN);		// partial packet length
	        		
	        		avail = this.packetsize;
            	}
            	
            	// figure out how much we can write to the current packet, write it, update indexes
        		int alen = Math.min(avail, bbremaining);
        		
        		this.writeDataInternal(pbb.array(), pbb.arrayOffset() + pbb.readerIndex(), alen);
        		
        		pbb.skipBytes(alen);
        		bbremaining = pbb.readableBytes();        		
        		this.packetpos += alen;        		
        		avail = this.packetsize - this.packetpos;
        		
    			// our formula always assumes that packetbuf starts at zero offset, anytime
    			// we write out part of the packetbuf we either need to write it all and clear it
        		// or we need to start with a new buffer with data starting at offset 0
        		if (bbremaining == 0) {
        			pbb.clear();
        		}
        		else {
        			ByteBuf npb = Hub.instance.getBufferAllocator().heapBuffer(MAX_PACKET_SIZE);
        			npb.writeBytes(pbb, bbremaining);
        			this.packetbuf = npb; 
        			
        			pbb.release();
        			pbb = npb;
        		}
    		}
    	}
    	
        // only write if there is space available in current packet or if we have a total
        // amount of data larger than max packet size
    	while ((remaining > 0) && ((avail > 0) || (remaining >= MAX_PACKET_SIZE))) {
        	// out of current packet space? create more packets
        	if (avail == 0) {
            	this.packetsize = MAX_PACKET_SIZE;
            	this.packetpos = 0;
    	        
    	        this.writeDataInternal((byte) MAX_PARTIAL_LEN);		// partial packet length
        		
        		avail = this.packetsize;
        	}

        	// figure out how much we can write to the current packet, write it, update indexes
    		int alen = Math.min(avail, remaining);
    		
    		this.writeDataInternal(bytes, offset, alen);
    		
    		remaining -= alen;
    		offset += alen;
    		this.packetpos += alen;    		
    		avail = this.packetsize - this.packetpos;
    	}
    	
    	// buffer remaining to build larger packet later
    	if (remaining > 0) {
    		if (this.packetbuf == null)
    			this.packetbuf = Hub.instance.getBufferAllocator().heapBuffer(MAX_PACKET_SIZE);
    		
    		// add to new buffer or add to existing buffer, either way it should be less than max here
    		this.packetbuf.writeBytes(bytes, offset, remaining);
    	}
    }
    
    public void writeData(byte val) {
    	byte[] bytes = new byte[1];
    	bytes[0] = val;
    	
    	this.writeData(bytes, 0, 1);
    }
    
    public void writeData(ByteBuf buf) {
    	this.writeData(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
    }
    
    // writes without checking if we need to add a new packet length, useful for headers and tags 
    protected void writeDataInternal(byte[] bytes, int offset, int len) {
		if (this.algorithm != SymmetricKeyAlgorithmTags.NULL) {
	    	// hash the unprocessed bytes
	        this.digest.update(bytes, offset, len);
	        
	        // TODO add debugging support by capturing bytes before compression
	        
	    	// TODO add compression support
	        // if compression flag is on, then send data through compressor
	        
	        // encrypt the data
	    	byte[] cout = this.cipher.update(bytes, offset, len);
	    	
	    	if (cout == null)
	    		return;
	    	
	    	int cpos = 0; 
	    	
	    	// fill present buffer as much as possible
	    	if (this.out.writableBytes() > 0) { 
	    		int clen = Math.min(cout.length, this.out.writableBytes());
	    		this.out.writeBytes(cout, cpos, clen);
	    		cpos += clen;
	    	}    	
	    	
	    	int remaining = cout.length - cpos;
	    	
	    	if (remaining == 0)  
	    		return;
	    	
	    	this.ensureBuffer(remaining);
			this.out.writeBytes(cout, cpos, remaining);
		}
		else {
	        
	        // TODO add debugging support by capturing bytes before compression
	        
	    	// TODO add compression support
	        // if compression flag is on, then send data through compressor
	        
	    	// fill present buffer as much as possible
	    	if (this.out.writableBytes() > 0) { 
	    		int clen = Math.min(len, this.out.writableBytes());
	    		this.out.writeBytes(bytes, offset, clen);
	    		offset += clen;
	    		len -= clen;
	    	}    	
	    	
	    	if (len == 0)  
	    		return;
	    	
	    	this.ensureBuffer(len);
			this.out.writeBytes(bytes, offset, len);
		}
    }
    
    protected void writeDataInternal(byte val) {
    	byte[] bytes = new byte[1];
    	bytes[0] = val;
    	
    	this.writeDataInternal(bytes, 0, 1);
    }

    // call before putting any file data in buffer
	public void init() throws PGPException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (this.algorithm != SymmetricKeyAlgorithmTags.NULL) {
			// *******************************************************************
			// public key packet(s)
			// *******************************************************************
			
			if ((methods.size() == 1) && (methods.get(0) instanceof PBEKeyEncryptionMethodGenerator)) {
			    PBEKeyEncryptionMethodGenerator method = (PBEKeyEncryptionMethodGenerator)methods.get(0);
			    
			    this.key = method.getKey(algorithm);
			    
		        ContainedPacket packet1 = method.generate(algorithm, null);
		        
		        byte[] encoded1 = packet1.getEncoded();
				
		        this.ensureBuffer(encoded1.length);
		        
		        this.out.writeBytes(encoded1);
			}
			else  {
				this.key = org.bouncycastle.openpgp.PGPUtil.makeRandomKey(algorithm, rand);
			    
		        byte[] sessionInfo = new byte[key.length + 3];
	
		        // add algorithm
		        sessionInfo[0] = (byte) algorithm;
		        
		        // add key
		        System.arraycopy(key, 0, sessionInfo, 1, key.length);
		        
		        // add checksum 
		        int check = 0;
	
		        for (int i = 1; i != sessionInfo.length - 2; i++)
		            check += sessionInfo[i] & 0xff;
	
		        sessionInfo[sessionInfo.length - 2] = (byte)(check >> 8);
		        sessionInfo[sessionInfo.length - 1] = (byte)(check);
			
			    for (PGPKeyEncryptionMethodGenerator method : methods) {
			        ContainedPacket packet1 = method.generate(algorithm, sessionInfo);
			        
			        byte[] encoded1 = packet1.getEncoded();
					
			        this.ensureBuffer(encoded1.length);
			        
			        this.out.writeBytes(encoded1);
			    }
			}
			
			// *******************************************************************
			// encrypt packet, add IV to encryption though
			// *******************************************************************
	        
	        this.ensureBuffer(3);
	        
	        this.out.writeByte(0xC0 | PacketTags.SYM_ENC_INTEGRITY_PRO);
	        this.out.writeByte(0);  // unknown size
	    	this.out.writeByte(1);        // version number
			
	        // ******************** start encryption **********************
			
	        String cName = PGPUtil.getSymmetricCipherName(algorithm) + "/CFB/NoPadding";
	
	    	DefaultJcaJceHelper helper = new DefaultJcaJceHelper();
	    	
	        this.cipher = helper.createCipher(cName);
	        
	        byte[] iv = new byte[this.cipher.getBlockSize()];
	
	        this.cipher.init(Cipher.ENCRYPT_MODE, PGPUtil.makeSymmetricKey(algorithm, key), new IvParameterSpec(iv));
	        
	        this.digest = MessageDigest.getInstance("SHA-1");
	        
	        // --- encrypt checksum for encrypt packet, part of the encrypted output --- 
	        
	        byte[] inLineIv = new byte[this.cipher.getBlockSize() + 2];
	        
	        rand.nextBytes(inLineIv);
	        
	        inLineIv[inLineIv.length - 1] = inLineIv[inLineIv.length - 3];
	        inLineIv[inLineIv.length - 2] = inLineIv[inLineIv.length - 4];
	        
	        this.writeDataInternal(inLineIv, 0, inLineIv.length);
		}
		
        // ******************* Optionally add Compression **************************
        
        // TODO set compressor 
        
        // ******************** Literal data packet ***********************
        
		this.ensureBuffer(1);
        this.writeDataInternal((byte) (0xC0 | PacketTags.LITERAL_DATA));
	}
    
	public void writeFirstLiteral(int dataLength) {
        // --- data packet ---
        byte[] encName = Utf8Encoder.encode(this.fileName);
        
        int headerlen =  1        // format
				+ 1 		// name length
				+ encName.length	// file name
				+ 4; 		// time

        // if less than 512 assume this is all there will be, because we cannot stream with numbers smaller than 512 for initial
        if (dataLength < (512 - headerlen)) {
        	this.packetsize = dataLength + headerlen;
	        
	    	this.writeDataPacketLength(this.packetsize);
        }
        else {
	        int length = dataLength + headerlen;
	        int power = 0;
	        
	        for (power = 0; (length != 1) && (power < 16); power++)
	            length >>>= 1;
	        
        	this.packetsize = 1 << power;
	        
	        this.writeDataInternal((byte) (0xE0 | power));		// partial packet length
        }
        
        byte[] hdr = new byte[headerlen];
        
        hdr[0] = (byte)PGPLiteralData.BINARY;		// data format
        hdr[1] = (byte)encName.length;			// file name
        
        for (int i = 0; i < encName.length; i++)
        	hdr[2 + i] = encName[i];
        
        hdr[headerlen - 4] = (byte)(this.modificationTime >> 24);
        hdr[headerlen - 3] = (byte)(this.modificationTime >> 16);
        hdr[headerlen - 2] = (byte)(this.modificationTime >> 8);
        hdr[headerlen - 1] = (byte)this.modificationTime;       
        
        this.writeDataInternal(hdr, 0, headerlen);
        
        this.packetpos = headerlen;
	}
	
    /*
     * Finish writing out the current packet and add protection packet
     */
    public void close() throws PGPException {
    	if (this.isClosed)
    		return;
    	
    	this.isClosed = true;
    	
		if (this.packetbuf != null) {
			// flush data if any packet space is available
	    	this.writeData(new byte[0], 0, 0);
	    	
        	this.packetsize = this.packetbuf.readableBytes();
	        this.packetpos = 0;
	        
	        // even if zero, this is fine, we need a final packet
	    	this.writeDataPacketLength(this.packetsize);		
	    	
	    	if (this.packetsize > 0)
	    		this.writeData(new byte[0], 0, 0);
	    	
			this.packetbuf.release();
			this.packetbuf = null;
		}
		
		if (this.algorithm != SymmetricKeyAlgorithmTags.NULL) {
	    	this.ensureBuffer(22);
	    	
			this.writeDataInternal((byte) (0xC0 | PacketTags.MOD_DETECTION_CODE));
			
			this.writeDataInternal((byte) 20);	// length of SHA-1 is always 20 bytes
			
	        this.writeDataInternal(this.digest.digest(), 0, 20);
	        
	        // TODO final compression, pass into doFinal below
	        
	        byte[] fcipher;
	        
			try {
				fcipher = this.cipher.doFinal();
			} 
			catch (Exception x) {
				throw new PGPException("Problem with PGP cipher", x);
			}
	        
	        this.ensureBuffer(fcipher.length);
	        this.out.writeBytes(fcipher);		// write raw
		}
		else {
	        // TODO final compression, if any
		}
		
        this.readyBuffers.add(this.out);
        this.out = null;
    }
    
    public void writeDataPacketLength(int bodyLen) {
        if (bodyLen < 192) {
            this.writeDataInternal((byte) bodyLen);
        }
        else if (bodyLen <= 8383) {
            bodyLen -= 192;
                    
            int oct1 = ((bodyLen >> 8) & 0xff) + 192;
            
            this.writeDataInternal((byte) oct1);
            this.writeDataInternal((byte) bodyLen);
        }
        else {
        	this.writeDataInternal((byte) 0xff);
        	this.writeDataInternal((byte) (bodyLen >> 24));
        	this.writeDataInternal((byte) (bodyLen >> 16));
        	this.writeDataInternal((byte) (bodyLen >> 8));
        	this.writeDataInternal((byte) bodyLen);
        }
    }
    
    /*
     * Reverse length:
     * 
        if (newPacket)
        {
            tag = hdr & 0x3f;

            int    l = this.read();

            if (l < 192)
            {
                bodyLen = l;
            }
            else if (l <= 223)
            {
                int b = in.read();

                bodyLen = ((l - 192) << 8) + (b) + 192;
            }
            else if (l == 255)
            {
                bodyLen = (in.read() << 24) | (in.read() << 16) |  (in.read() << 8)  | in.read();
            }
            else
            {
                partial = true;
                bodyLen = 1 << (l & 0x1f);
            }
        }
        else
        {
            int lengthType = hdr & 0x3;

            tag = (hdr & 0x3f) >> 2;

            switch (lengthType)
            {
            case 0:
                bodyLen = this.read();
                break;
            case 1:
                bodyLen = (this.read() << 8) | this.read();
                break;
            case 2:
                bodyLen = (this.read() << 24) | (this.read() << 16) | (this.read() << 8) | this.read();
                break;
            case 3:
                partial = true;
                break;
            default:
                throw new IOException("unknown length type encountered");
            }
        }
     * 
     */
}
