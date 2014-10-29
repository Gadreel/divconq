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

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;

/**
 * Basic utility class, borrowed from Bouncy Castle since it was not
 * public access.
 *  
 */
public class PGPUtil
{
    public static String getDigestName(
        int        hashAlgorithm)
        throws PGPException
    {
        switch (hashAlgorithm)
        {
        case HashAlgorithmTags.SHA1:
            return "SHA1";
        case HashAlgorithmTags.MD2:
            return "MD2";
        case HashAlgorithmTags.MD5:
            return "MD5";
        case HashAlgorithmTags.RIPEMD160:
            return "RIPEMD160";
        case HashAlgorithmTags.SHA256:
            return "SHA256";
        case HashAlgorithmTags.SHA384:
            return "SHA384";
        case HashAlgorithmTags.SHA512:
            return "SHA512";
        case HashAlgorithmTags.SHA224:
            return "SHA224";
        case HashAlgorithmTags.TIGER_192:
            return "TIGER";
        default:
            throw new PGPException("unknown hash algorithm tag in getDigestName: " + hashAlgorithm);
        }
    }
    
    public static String getSignatureName(
        int        keyAlgorithm,
        int        hashAlgorithm)
        throws PGPException
    {
        String     encAlg;
                
        switch (keyAlgorithm)
        {
        case PublicKeyAlgorithmTags.RSA_GENERAL:
        case PublicKeyAlgorithmTags.RSA_SIGN:
            encAlg = "RSA";
            break;
        case PublicKeyAlgorithmTags.DSA:
            encAlg = "DSA";
            break;
        case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT: // in some malformed cases.
        case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
            encAlg = "ElGamal";
            break;
        default:
            throw new PGPException("unknown algorithm tag in signature:" + keyAlgorithm);
        }

        return getDigestName(hashAlgorithm) + "with" + encAlg;
    }
    
    public static String getSymmetricCipherName(
        int    algorithm)
    {
        switch (algorithm)
        {
        case SymmetricKeyAlgorithmTags.NULL:
            return null;
        case SymmetricKeyAlgorithmTags.TRIPLE_DES:
            return "DESEDE";
        case SymmetricKeyAlgorithmTags.IDEA:
            return "IDEA";
        case SymmetricKeyAlgorithmTags.CAST5:
            return "CAST5";
        case SymmetricKeyAlgorithmTags.BLOWFISH:
            return "Blowfish";
        case SymmetricKeyAlgorithmTags.SAFER:
            return "SAFER";
        case SymmetricKeyAlgorithmTags.DES:
            return "DES";
        case SymmetricKeyAlgorithmTags.AES_128:
            return "AES";
        case SymmetricKeyAlgorithmTags.AES_192:
            return "AES";
        case SymmetricKeyAlgorithmTags.AES_256:
            return "AES";
        case SymmetricKeyAlgorithmTags.CAMELLIA_128:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.CAMELLIA_192:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.CAMELLIA_256:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.TWOFISH:
            return "Twofish";
        default:
            throw new IllegalArgumentException("unknown symmetric algorithm: " + algorithm);
        }
    }
    
    static public SecretKey makeSymmetricKey(
        int             algorithm,
        byte[]          keyBytes)
        throws PGPException
    {
        String    algName = getSymmetricCipherName(algorithm);

        if (algName == null)
        {
            throw new PGPException("unknown symmetric algorithm: " + algorithm);
        }

        return new SecretKeySpec(keyBytes, algName);
    }
    
    static public void encryptFile(String outputFileName, String inputFileName, String encKeyFileName)
        throws IOException, NoSuchProviderException, PGPException
    {
    	Path fileIn = Paths.get(inputFileName);
    	Path fileOut = Paths.get(outputFileName);
    	Path fileKey = Paths.get(encKeyFileName);
    	
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOut.toFile()));

        try {    
        	EncryptedFileStream pw = new EncryptedFileStream(); 
        	
        	//pw.setAlgorithm(SymmetricKeyAlgorithmTags.NULL);
        	pw.setFileName(fileIn.getFileName().toString());
        	pw.loadPublicKey(fileKey);
        	
        	pw.init();
        	
            FileInputStream in = new FileInputStream(fileIn.toFile());
            byte[] ibuf = new byte[31 * 1024];

            int len;
            
            while ((len = in.read(ibuf)) > 0) {
                pw.writeData(ibuf, 0, len);
                
                ByteBuf buf = pw.nextReadyBuffer();
                
                while (buf != null) {
                	out.write(buf.array(), buf.arrayOffset(), buf.readableBytes()); 
                
                	buf.release();
                	
                	buf = pw.nextReadyBuffer();
                }
            }
            
            in.close();
            pw.close();
            
            ByteBuf buf = pw.nextReadyBuffer();
            
            while (buf != null) {
            	out.write(buf.array(), buf.arrayOffset(), buf.readableBytes()); 
            
            	buf.release();
            	
            	buf = pw.nextReadyBuffer();
            }
            
            out.close();
        }
        catch (Exception e) {
            System.err.println(e);
            
            e.printStackTrace();
        }
    }
    
    /*
     * 
     * TODO develop a packet reader/lister
				
				case 110: {
					byte[] in = Files.readAllBytes(Paths.get("/Work/Temp/Dest/story2.xml.gpg"));
					
					int idx = 0;
					
					while (idx < in.length) {
						int hdr = in[idx] & 0xFF;
				        
				        if ((hdr & 0x80) == 0) {
				            System.out.println("invalid header encountered");
				            break;
				        }
						
				        boolean newPacket = ((hdr & 0x40) != 0);
				        int tag = 0;
				        int bodyLen = 0;
				        int hdrLen = 1;
				        boolean partial = false;
						
				        if (newPacket) {
				            tag = hdr & 0x3f;

							System.out.println("New Packet: " + HexUtil.charToHex(hdr) + " tag: " + tag);
				        	
				            int l = in[idx + 1] & 0xFF;

				            if (l < 192)
				            {
				                bodyLen = l;
				                hdrLen = 2;
				            }
				            else if (l <= 223)
				            {
				                int b = in[idx + 2] & 0xFF;

				                bodyLen = ((l - 192) << 8) + (b) + 192;
				                hdrLen = 3;
				            }
				            else if (l == 255)
				            {
				                //bodyLen = (in[idx + 2] << 24) | (in[idx + 3] << 16) | (in[idx + 4] << 8)  | in[idx + 5];
				                bodyLen = ((in[idx + 2] & 0xFF) << 24) | ((in[idx + 3] & 0xFF) << 16) |  ((in[idx + 4] & 0xFF) << 8)  | (in[idx + 5] & 0xFF);
				                hdrLen = 6;
				            }
				            else
				            {
				                partial = true;
				                bodyLen = 1 << (l & 0x1f);
				                hdrLen = 2;
				            }
				        }
				        else {
				            tag = (hdr & 0x3f) >> 2;

							System.out.println("Old Packet: " + HexUtil.charToHex(hdr) + " tag: " + tag);
				        	
				            int lengthType = hdr & 0x3;

				            switch (lengthType)
				            {
				            case 0:
				                hdrLen = 2;
				                bodyLen = in[idx + 1] & 0xFF;
				                break;
				            case 1:
				                hdrLen = 3;
				                //bodyLen = (in[idx + 1] << 8) | in[idx + 2];
				                bodyLen = ((in[idx + 1] & 0xFF) << 8) | (in[idx + 2] & 0xFF);
				                break;
				            case 2:
				                hdrLen = 5;
				                //bodyLen = (in[idx + 1] << 24) | (in[idx + 2] << 16) | (in[idx + 3] << 8) | in[idx + 4];
				                bodyLen = ((in[idx + 1] & 0xFF) << 24) | ((in[idx + 2] & 0xFF) << 16) | ((in[idx + 3] & 0xFF) << 8) | (in[idx + 4] & 0xFF);
				                break;
				            case 3:
				                partial = true;
				                break;
				            default:
				                throw new IOException("unknown length type encountered");
				            }
				        }	
				        
				        System.out.println("- Length: " + bodyLen + " partial: " + partial + " hdr len: " + hdrLen);
				        
				        idx += bodyLen + hdrLen;
					}
					
					System.out.println("end of file");
					
					break;
				}
     * 
     */
}
