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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.bcpg.ContainedPacket;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import divconq.hub.Hub;
import divconq.lang.chars.Utf8Encoder;
import divconq.pgp.PGPUtil;
import divconq.util.HexUtil;

public class PGPWriter2 {
	@SuppressWarnings("resource")
	public void test2(String srcpath, String destpath, String keyring) throws Exception {
		Path src = Paths.get(srcpath);
		
		// file data 
		byte[] fileData = Files.readAllBytes(src);

		// dest
        OutputStream dest = new BufferedOutputStream(new FileOutputStream(destpath));
        
        // encryption key
        PGPPublicKey pubKey = null;
        
        InputStream keyIn = new BufferedInputStream(new FileInputStream(keyring));
        
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
		
	    String fileName = src.getFileName().toString();
        byte[] encName = Utf8Encoder.encode(fileName);
	    long modificationTime = System.currentTimeMillis();
	    
	    SecureRandom rand = new SecureRandom();
	    int algorithm = PGPEncryptedData.AES_256;
	    
	    Cipher cipher = null;
	    
	    ByteBuf leadingbuf = Hub.instance.getBufferAllocator().heapBuffer(1024 * 1024);	// 1 mb
	    ByteBuf encbuf = Hub.instance.getBufferAllocator().heapBuffer(1024 * 1024);		// 1 mb
		
		// *******************************************************************
		// public key packet
		// *******************************************************************
		
		PGPKeyEncryptionMethodGenerator method = new JcePublicKeyKeyEncryptionMethodGenerator(pubKey);
		
		byte[] key = org.bouncycastle.openpgp.PGPUtil.makeRandomKey(algorithm, rand);
	    
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
	    
        ContainedPacket packet1 = method.generate(algorithm, sessionInfo);
        
        byte[] encoded1 = packet1.getEncoded();
		
        leadingbuf.writeBytes(encoded1);
		
		// *******************************************************************
		// encrypt packet, add IV to encryption though
		// *******************************************************************
        
    	leadingbuf.writeByte(0xC0 | PacketTags.SYM_ENC_INTEGRITY_PRO);

    	this.writePacketLength(leadingbuf, 0);		// 0 = we don't know
    	
    	leadingbuf.writeByte(1);        // version number
		
        String cName = PGPUtil.getSymmetricCipherName(algorithm) + "/CFB/NoPadding";

    	DefaultJcaJceHelper helper = new DefaultJcaJceHelper();
    	
        cipher = helper.createCipher(cName);
        
        byte[] iv = new byte[cipher.getBlockSize()];

    	cipher.init(Cipher.ENCRYPT_MODE, PGPUtil.makeSymmetricKey(algorithm, key), new IvParameterSpec(iv));
		
        // ******************** start encryption **********************
        
        // --- encrypt checksum for encrypt packet, part of the encrypted output --- 
        
        byte[] inLineIv = new byte[cipher.getBlockSize() + 2];
        
        rand.nextBytes(inLineIv);
        
        inLineIv[inLineIv.length - 1] = inLineIv[inLineIv.length - 3];
        inLineIv[inLineIv.length - 2] = inLineIv[inLineIv.length - 4];
        
        encbuf.writeBytes(inLineIv);
        
        System.out.println("bytes written a: " + encbuf.readableBytes());
        
        // --- data packet ---
        
        int chunkpos = 0;

        int headerlen =  1        // format
				+ 1 		// name length
				+ encName.length	// file name
				+ 4; 		// time
        
        encbuf.writeByte(0xC0 | PacketTags.LITERAL_DATA);
    	
        int packetsize = 512 - headerlen;
        
        if (fileData.length - chunkpos < packetsize) {
        	packetsize = fileData.length - chunkpos;
	        
	    	this.writePacketLength(encbuf, headerlen + packetsize);
        }
        else {
	        encbuf.writeByte(0xE9);		// 512 packet length
        }
        
        System.out.println("bytes written b: " + encbuf.readableBytes());
        
    	encbuf.writeByte(PGPLiteralData.BINARY);		// data format
        
    	encbuf.writeByte((byte)encName.length);			// file name

    	encbuf.writeBytes(encName);

        encbuf.writeInt((int) (modificationTime / 1000));		// mod time
        
        System.out.println("bytes written c: " + encbuf.readableBytes());
        
        encbuf.writeBytes(fileData, chunkpos, packetsize);
        
        System.out.println("bytes written d: " + encbuf.readableBytes());
        
        chunkpos += packetsize;
        
        // write one or more literal packets
        while (chunkpos < fileData.length) {
	        packetsize = 512;
	        
	        // check if this is the final packet
	        if (fileData.length - chunkpos <= packetsize) {
	        	packetsize = fileData.length - chunkpos;
		        
		    	this.writePacketLength(encbuf, packetsize);
	        }
	        else {
		        encbuf.writeByte(0xE9);		// full 512 packet length
	        }
	        
	        encbuf.writeBytes(fileData, chunkpos, packetsize);
	        
	        chunkpos += packetsize;
		}
	
        // protection packet
	    encbuf.writeByte(0xC0 | PacketTags.MOD_DETECTION_CODE);
        encbuf.writeByte(20);	// packet length
	
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
        
        byte[] rv = md.digest();
        
        encbuf.writeBytes(rv);
        
    	System.out.println("Pre-Encrypted Hex");
    	
    	this.hexDump(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
    	
    	System.out.println();
    	System.out.println();
    	
        // ***** encryption data ready *********
        
        byte[] encdata = cipher.doFinal(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
		
    	// add encrypted data to main buffer
    	leadingbuf.writeBytes(encdata);
        
    	System.out.println("Final Hex");
    	
    	this.hexDump(leadingbuf.array(), leadingbuf.arrayOffset(), leadingbuf.writerIndex());
    	
    	System.out.println();
    	System.out.println();
    	
        // write to file
    	dest.write(leadingbuf.array(), leadingbuf.arrayOffset(), leadingbuf.writerIndex());
    	
    	dest.flush();
    	dest.close();
	}
	
	
	@SuppressWarnings("resource")
	public void test1(String srcpath, String destpath, String keyring) throws Exception {
		Path src = Paths.get(srcpath);
		
		// file data 
		byte[] story = Files.readAllBytes(src);

		// dest
        OutputStream dest = new BufferedOutputStream(new FileOutputStream(destpath));
        
        // encryption key
        PGPPublicKey pubKey = null;
        
        InputStream keyIn = new BufferedInputStream(new FileInputStream(keyring));
        
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
		
	    String fileName = src.getFileName().toString();
        byte[] encName = Utf8Encoder.encode(fileName);
	    long modificationTime = System.currentTimeMillis();
	    
	    SecureRandom rand = new SecureRandom();
	    int algorithm = PGPEncryptedData.AES_256;
	    
	    Cipher cipher = null;
	    
	    ByteBuf leadingbuf = Hub.instance.getBufferAllocator().heapBuffer(1024 * 1024);	// 1 mb
	    ByteBuf encbuf = Hub.instance.getBufferAllocator().heapBuffer(1024 * 1024);		// 1 mb
		
		// *******************************************************************
		// public key packet
		// *******************************************************************
		
		PGPKeyEncryptionMethodGenerator method = new JcePublicKeyKeyEncryptionMethodGenerator(pubKey);
		
		byte[] key = org.bouncycastle.openpgp.PGPUtil.makeRandomKey(algorithm, rand);
	    
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
	    
        ContainedPacket packet1 = method.generate(algorithm, sessionInfo);
        
        byte[] encoded1 = packet1.getEncoded();
		
        leadingbuf.writeBytes(encoded1);
		
		// *******************************************************************
		// encrypt packet, add IV to 
		// *******************************************************************
		
        String cName = PGPUtil.getSymmetricCipherName(algorithm) + "/CFB/NoPadding";

    	DefaultJcaJceHelper helper = new DefaultJcaJceHelper();
    	
        cipher = helper.createCipher(cName);
        
        byte[] iv = new byte[cipher.getBlockSize()];

    	cipher.init(Cipher.ENCRYPT_MODE, PGPUtil.makeSymmetricKey(algorithm, key), new IvParameterSpec(iv));
		
        // ******************** start encryption **********************
        
        // --- encrypt checksum for encrypt packet, part of the encrypted output --- 
        
        byte[] inLineIv = new byte[cipher.getBlockSize() + 2];
        
        rand.nextBytes(inLineIv);
        
        inLineIv[inLineIv.length - 1] = inLineIv[inLineIv.length - 3];
        inLineIv[inLineIv.length - 2] = inLineIv[inLineIv.length - 4];
        
        encbuf.writeBytes(inLineIv);
        
        // --- data packet ---
        
        encbuf.writeByte(0xC0 | PacketTags.LITERAL_DATA);
    	
    	this.writePacketLength(encbuf, 
    			1        // format
    			+ 1 		// name length
    			+ encName.length	// file name
    			+ 4 		// time
    			+ story.length 		// data
    		);
        
    	encbuf.writeByte(PGPLiteralData.BINARY);
        
    	encbuf.writeByte((byte)encName.length);

    	encbuf.writeBytes(encName);

        encbuf.writeInt((int) (modificationTime / 1000));
        
        encbuf.writeBytes(story);
        
        // protection packet
	    encbuf.writeByte(0xC0 | PacketTags.MOD_DETECTION_CODE);
        encbuf.writeByte(20);	// packet length
	
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
        
        byte[] rv = md.digest();
        
        encbuf.writeBytes(rv);
        
    	System.out.println("Encrypted Hex");
    	
    	this.hexDump(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
    	
    	System.out.println();
    	System.out.println();
    	
        // ***** encryption data ready *********
        
        byte[] encdata = cipher.doFinal(encbuf.array(), encbuf.arrayOffset(), encbuf.writerIndex());
		
    	leadingbuf.writeByte(0xC0 | PacketTags.SYM_ENC_INTEGRITY_PRO);

    	/*
    	this.writePacketLength(leadingbuf, 
    			1		// version 
    			+ encdata.length 		// encrypted data
    		);
    		*/
    	
    	this.writePacketLength(leadingbuf, 0);		// 0 = we don't know
    	
    	leadingbuf.writeByte(1);        // version number

    	// add encrypted data to main buffer
    	leadingbuf.writeBytes(encdata);
        
    	System.out.println("Final Hex");
    	
    	this.hexDump(leadingbuf.array(), leadingbuf.arrayOffset(), leadingbuf.writerIndex());
    	
    	System.out.println();
    	System.out.println();
    	
        // write to file
    	dest.write(leadingbuf.array(), leadingbuf.arrayOffset(), leadingbuf.writerIndex());
    	
    	dest.flush();
    	dest.close();
	}
	
	public void hexDump(byte[] array, int offset, int length) {
		int d = 0;
		
		for (int i = 0; i < length; i++) {
			System.out.print(HexUtil.charToHex(array[offset + i]) + " ");
			
			d++;
			
			if (d == 32) {
				System.out.println();
				d = 0;
			}
		}
	}
	
    public void writePacketLength(ByteBuf out, int bodyLen) throws IOException {
        if (bodyLen < 192) {
            out.writeByte(bodyLen);
        }
        else if (bodyLen <= 8383) {
            bodyLen -= 192;
                    
            int oct1 = ((bodyLen >> 8) & 0xff) + 192;
            
            System.out.print("packet length: " +  HexUtil.charToHex(oct1) 
            		+ " " + HexUtil.charToHex(bodyLen));
            System.out.println();
            
            out.writeByte(oct1);
            out.writeByte(bodyLen);
        }
        else {
            out.writeByte(0xff);
            out.writeByte(bodyLen >> 24);
            out.writeByte(bodyLen >> 16);
            out.writeByte(bodyLen >> 8);
            out.writeByte(bodyLen);
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
