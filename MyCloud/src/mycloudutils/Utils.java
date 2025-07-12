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
    

    //AES aleatória
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }


    public static PublicKey loadPublicKey(String userDest) throws Exception {
		FileInputStream kfile2 = new FileInputStream("../keystores/keystore."+userDest);
		KeyStore kstore = KeyStore.getInstance("PKCS12");
		kstore.load(kfile2, "epocaespecial".toCharArray()); 
		Certificate cert = kstore.getCertificate(userDest);  // cert
        PublicKey publicKey = cert.getPublicKey();   // pubk
        kfile2.close();
        if (publicKey == null) {
            System.out.println("Erro: Não foi possível ler a chave pública!");
            return null;
        }
		
		return publicKey;
		
	}

    //Ficheiro encriptado a partir da chave AES
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

    public static File decryptFile(File pasta,File fileencriptado, File chave, PrivateKey pk) throws Exception {
    	
    	FileInputStream keyFile = new FileInputStream(chave);
        byte[] encryptedSecretKey = keyFile.readAllBytes();
        keyFile.close();
        
        
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] secretKeyEncoded = rsaCipher.doFinal(encryptedSecretKey);
    	
        SecretKeySpec secretKey1 = new SecretKeySpec(secretKeyEncoded, "AES");
        
        
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey1);
        
        String nomeficheirodecript;
        
        if(fileencriptado.getName().contains(".secure")) {
        	
        	nomeficheirodecript = fileencriptado.getName().replace(".secure", "");
        }
        else {
        	nomeficheirodecript = fileencriptado.getName().replace(".encrypted", "");
        }
        
        
        //nomeficheirodecript.replace(".secure", "");
        File ficheirodecript = new File(pasta,nomeficheirodecript);
        
        
        
        
        try (FileInputStream fis = new FileInputStream(fileencriptado);
             CipherInputStream cis = new CipherInputStream(fis, aesCipher);
             FileOutputStream fos = new FileOutputStream(ficheirodecript)) {

            byte[] buffer = new byte[16];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        System.out.println("Ficheiro decifrado com sucesso: "+fileencriptado.getName() + " para o ficheiro: "+ ficheirodecript.getName());
        
        return ficheirodecript;
         
    }

}
