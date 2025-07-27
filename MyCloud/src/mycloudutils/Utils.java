package mycloudutils;


import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class Utils {

    private static final String AES_ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 128;
    

    
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }


    public static PublicKey loadPublicKey(String user, String pass, String userDest) throws Exception {
		FileInputStream kfile2 = new FileInputStream("../keystores/keystore."+user);
		KeyStore kstore = KeyStore.getInstance("PKCS12");
		kstore.load(kfile2, pass.toCharArray()); 
		Certificate cert = kstore.getCertificate(userDest);  
        PublicKey publicKey = cert.getPublicKey();   
        kfile2.close();
        if (publicKey == null) {
            System.out.println("Erro: Não foi possível ler a chave pública!");
            return null;
        }
		
		return publicKey;
		
	}


    public static PrivateKey loadPrivateKey(String user, String pass) throws Exception{
        try (FileInputStream fis = new FileInputStream("../keystores/keystore." + user)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, pass.toCharArray());

            Key key = ks.getKey(user, pass.toCharArray());
            if (!(key instanceof PrivateKey)) {
                throw new KeyStoreException(
                    "O alias " + user + " não tem uma PrivateKey");
            }
            return (PrivateKey) key;
        }

    }

    
    public static void encryptFile(File inputFile, File outputFile, SecretKey secretKey, PublicKey pubk, String userDest, String filename) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        processFile(cipher, inputFile, outputFile);

        Cipher cipher2 = Cipher.getInstance("RSA");
        cipher2.init(Cipher.WRAP_MODE, pubk);
        byte[] newWrappedKey = cipher2.wrap(secretKey);

        try (FileOutputStream kos = new FileOutputStream("../" + filename + ".chave." + userDest)) {
            kos.write(newWrappedKey);
            
        }

    }

    
    private static void processFile(Cipher cipher, File inputFile, File outputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            cos.close();
        }
        
        
    }

    public static File decryptFile(File pasta, File fileencriptado, File chave, PrivateKey pk) throws Exception {
    	
    	byte[] encryptedSecretKey;
        try (FileInputStream keyFis = new FileInputStream(chave)) {
            encryptedSecretKey = keyFis.readAllBytes();
        }
        
        
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] secretKeyEncoded = rsaCipher.doFinal(encryptedSecretKey);
        
    	
        SecretKeySpec secretKey = new SecretKeySpec(secretKeyEncoded, "AES");
        
        
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        String nomeOriginal = fileencriptado.getName().replaceFirst("\\.cifrado$", "");
        File outputFile = new File(pasta, nomeOriginal);
           
        try (FileInputStream  fis = new FileInputStream(fileencriptado);
         CipherInputStream cis = new CipherInputStream(fis, aesCipher);
         FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }   
        
        System.out.println("Ficheiro decifrado com sucesso: "+ fileencriptado.getName()+ " para " + outputFile.getName());
        
        return outputFile;
         
    }

    public static byte[] signFile(File file, Key myPrivateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign((PrivateKey) myPrivateKey);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }
        }
        return signature.sign();
    }

   
    public static boolean verifySignature(File file, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }
        }
        return signature.verify(signatureBytes);
    }

}
