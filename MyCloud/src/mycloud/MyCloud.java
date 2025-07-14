package mycloud;

import javax.net.ssl.SSLSocketFactory;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.*;

import mycloudutils.Utils;

public class MyCloud {
    public static void main(String[] args) throws Exception {
        String serverHost = null;
        int    serverPort = -1;
        String user       = null;
        String pass       = null;

        //Enviar e Receber
        boolean isUpload  = false;
        boolean isDownload= false;

        //Assinar e Decifrar
        boolean isCifrar  = false;
        boolean isDecifrar = false;
        String nomeficheiro = null;
        String userDestinatario = null;
        String ficheiroEncriptado = null;

        //Assinaturas
        boolean isAssinar  = false;
        boolean isVerificar = false;
        String ficheiroAssinar   = null;
        String ficheiroVerificar = null;

        List<String> files= new ArrayList<>();



        // 1) Parsing dos argumentos
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                    if (i+1 >= args.length) {
                        System.err.println("Falta host:port após -s");
                        System.exit(1);
                    }
                    String[] hp = args[++i].split(":");
                    if (hp.length != 2) {
                        System.err.println("Formato inválido em -s, use host:port");
                        System.exit(1);
                    }
                    serverHost = hp[0];
                    try {
                        serverPort = Integer.parseInt(hp[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("Porta inválida: " + hp[1]);
                        System.exit(1);
                    }
                    break;

                case "-u":
                    if (i+1 >= args.length) {
                        System.err.println("Falta usuário após -u");
                        System.exit(1);
                    }
                    user = args[++i];
                    break;

                case "-p":
                    if (i+1 >= args.length) {
                        System.err.println("Falta password após -p");
                        System.exit(1);
                    }
                    pass = args[++i];
                    break;

                case "-e":
                    isUpload = true;
                    for (int j = i+1; j < args.length; j++) {
                        files.add(args[j]);
                    }
                    i = args.length;  
                    break;

                case "-r":
                    isDownload = true;
                    for (int j = i+1; j < args.length; j++) {
                        files.add(args[j]);
                    }
                    i = args.length;
                    break;

                case "-c":
                    if (i + 1 >= args.length) {
                        System.err.println("Falta ficheiro após -c");
                        System.exit(1);
                    }
                    nomeficheiro = args[++i];
                    isCifrar = true;
                    break;
                                
                case "-t":
                    if (i+1 >= args.length) {
                        System.err.println("Falta usuário após -t");
                        System.exit(1);
                    }
                    userDestinatario = args[++i];
                    break;

                case "-d":
                    if (i + 1 >= args.length) {
                        System.err.println("Falta ficheiro após -d");
                        System.exit(1);
                    }
                    ficheiroEncriptado = args[++i];
                    isDecifrar = true;
                    break;

                case "-a":
                    if (i+1 >= args.length) {
                        System.err.println("Falta ficheiro após -a");
                        System.exit(1);
                    }
                    ficheiroAssinar = args[++i];
                    isAssinar = true;
                    break;

                case "-v":
                    if (i+1 >= args.length) {
                        System.err.println("Falta ficheiro após -v");
                        System.exit(1);
                    }
                    ficheiroVerificar = args[++i];
                    isVerificar = true;
                    break;

                default:
                    System.err.println("Argumento desconhecido: " + args[i]);
                    System.exit(1);
            }
        }

        
        
        if (isCifrar) {
            if (nomeficheiro == null || userDestinatario == null) {
                System.err.println("Uso para cifrar: -u username -p password -c nome_de_ficheiro -t destinatário");
                System.exit(1);
            }
            doCifrar(nomeficheiro, user, pass, userDestinatario);
            return;  
        }


        if (isDecifrar) {
            if (ficheiroEncriptado == null) {
                System.err.println("Uso para decifrar: -u username -p password -d nome_de_ficheiro");
                System.exit(1);
            }
            doDecifrar(ficheiroEncriptado, user, pass);
            return;  
        }

        if (isAssinar) {
            if (user == null || pass == null || ficheiroAssinar == null) {
                System.err.println("Uso para assinar: myCloud -u user -p pass -a nome_de_ficheiro");
                System.exit(1);
            }
            doAssinar(user, pass, ficheiroAssinar);
            return;
        }

        if (isVerificar) {
            if (user == null || pass == null || ficheiroVerificar == null) {
                System.err.println("Uso para assinar: myCloud -u user -p pass -a nome_de_ficheiro");
                System.exit(1);
            }
            doVerificar(user, pass, ficheiroVerificar);
            return;
        }

        //Download e Upload
        if (serverHost == null || serverPort < 0
            || user == null || pass == null
            || files.isEmpty())
        {
            System.err.println("Uso válido:");
            System.err.println("  -s host:port -u user -p pass -e ficheiro1 [ficheiro2...]");
            System.err.println("  -s host:port -u user -p pass -r ficheiro1 [ficheiro2...]");
            System.exit(1);
        }

        // TrustStore 
        String tsRelPath = "../keystores/truststore.jks";
        String tsAbPath  = new File(tsRelPath).getAbsolutePath();
        System.setProperty("javax.net.ssl.trustStore",   tsAbPath);
        System.setProperty("javax.net.ssl.trustStorePassword", "epocaespecial");
        System.setProperty("javax.net.ssl.trustStoreType",     "JKS");

        //SSLSocket e handshake
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) sf.createSocket(serverHost, serverPort)) {
            socket.startHandshake();

            
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());

            //Autenticação
            out.writeObject(user);
            out.writeObject(pass);
            out.flush();
            String authResp = in.readObject().toString();
            if (!authResp.startsWith("OK")) {
                System.err.println("Autenticação falhou: " + authResp);
                return;
            }
            System.out.println("Autenticado com sucesso.");

           
            if (isUpload) {
                doUpload(files, out, in);
            } else if(isDownload){
                doDownload(files, out, in);
            }
        } 
    } 

    private static void doUpload(List<String> files,
                                 ObjectOutputStream out,
                                 ObjectInputStream in) throws Exception {
        out.writeObject("-e");
        for (String f : files) out.writeObject(new File(f).getName());
        out.writeObject("Terminou");
        out.flush();

        String resp = in.readObject().toString();
        if (!resp.startsWith("OK")) {
            System.err.println(resp);
            return;
        }

        for (String path : files) {
            File file = new File("../", path);
            out.writeObject(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }
        }
        out.flush();

        for (int i = 0; i < files.size(); i++) {
            System.out.println(in.readObject().toString());
        }
    }

    private static void doDownload(List<String> files,
                                   ObjectOutputStream out,
                                   ObjectInputStream in) throws Exception {
        out.writeObject("-r");
        for (String name : files) out.writeObject(name);
        out.writeObject("Terminou");
        out.flush();

        File outDir = new File("../fromServer");
        if (!outDir.exists()) outDir.mkdirs();

        for (String ignored : files) {
            String res = in.readObject().toString();
            if (res.startsWith("ERRO")) {
                System.err.println(res);
                continue;
            }
            String filename = res;
            long size = (Long) in.readObject();

            File target = new File(outDir, filename);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                byte[] buf = new byte[4096];
                long received = 0;
                while (received < size) {
                    int r = in.read(buf, 0, (int)Math.min(buf.length, size - received));
                    fos.write(buf, 0, r);
                    received += r;
                }
            }
            System.out.println("Download tá nice: " + filename);
        }
    }


    private static void doCifrar(String nomeficheiro, String user, String pass, String userDest) throws Exception{
        
        SecretKey aes = Utils.generateAESKey();
        PublicKey pubk = Utils.loadPublicKey(user, pass, userDest); 

        File inputFile  = new File("../", nomeficheiro);
        File outputFile = new File("../", nomeficheiro + ".cifrado");
        Utils.encryptFile(inputFile, outputFile, aes, pubk, userDest, nomeficheiro);

    }

    private static void doDecifrar(String nomeficheiro, String user, String pass) throws Exception{

        String baseName = nomeficheiro.replaceFirst("\\.cifrado$", "");

        String keyFileName = baseName + ".chave." + user;
        File enc = new File("../fromServer/" + nomeficheiro);
        File key = new File("../fromServer/" + keyFileName); 

        PrivateKey sk = Utils.loadPrivateKey(user, pass);

        File outDir = new File("../decifrado");
        outDir.mkdirs();

        Utils.decryptFile(outDir, enc, key, sk);
    }

    private static void doAssinar(String user, String pass, String nomeficheiro) throws Exception{

        PrivateKey sk = Utils.loadPrivateKey(user, pass);

        File input = new File("../", nomeficheiro);
        if (!input.exists()) {
            System.err.println("Erro: ficheiro não encontrado: " + input.getPath());
            return;
        }

        byte[] sigBytes = Utils.signFile(input, sk);

        String sigName = nomeficheiro + ".assinatura." + user;
        File outSig = new File("../", sigName);
        try (FileOutputStream fos = new FileOutputStream(outSig)) {
            fos.write(sigBytes);
        }

        System.out.println("Ficheiro assinado com sucesso: " + sigName);
        
    }

    private static void doVerificar(String user,String pass, String nomeficheiro) throws Exception{

        String baseName;
        String marker = ".assinatura.";
        int idx = nomeficheiro.lastIndexOf(marker);
        if (idx >= 0) {
            baseName = nomeficheiro.substring(0, idx);
        } else {
            baseName = nomeficheiro;
        }

        String aliasAssinatura = nomeficheiro.substring(idx + marker.length());


        File base = new File("../fromServer", baseName);
        File fileAssinado = new File("../fromServer", nomeficheiro);


        if (!base.exists()) {
            System.err.println("Original não encontrado: " + base.getPath());
            return;
        }

        if (!fileAssinado.exists()) {
            System.err.println("Assinatura não encontrada: " + fileAssinado.getPath());
            return;
        }

        PublicKey pubk = Utils.loadPublicKey(user, pass, aliasAssinatura);

        byte[] sigBytes;
        try (FileInputStream fis = new FileInputStream(fileAssinado)) {
            sigBytes = fis.readAllBytes();
        }

        boolean validado = Utils.verifySignature(base, sigBytes, pubk);


        String resultado;
        if (validado) {
            resultado = "Assinatura válida: " + baseName + " (assinada por " + aliasAssinatura + ")";
        } else {
            resultado = "Assinatura inválida: " + baseName + " (assinada por " + aliasAssinatura + ")";
        }
        System.out.println(resultado);

        
    }


}
