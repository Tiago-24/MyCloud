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
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
    }

    //Ficheiro encriptado a partir da chave AES
    public static void encryptFile(File inputFile, File outputFile, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        processFile(cipher, inputFile, outputFile);
    }

    public static File decryptFile(File pasta,File fileencriptado, File chave, Key secretKey) throws Exception {
    	
    	FileInputStream keyFile = new FileInputStream(chave);
        byte[] encryptedSecretKey = keyFile.readAllBytes();
        keyFile.close();
        
        
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, secretKey);
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
