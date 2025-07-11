package mycloud;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.*;

public class MyCloud {
    public static void main(String[] args) throws Exception {
        String serverHost = null;
        int    serverPort = -1;
        String user       = null;
        String pass       = null;
        boolean isUpload  = false;
        boolean isDownload= false;
        boolean isCifrar  = false;
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
                    
                    cipherFile = args[++i];
                    
                    if (i + 1 < args.length) {
                        System.err.println("Só 1 ficheiro");
                        System.exit(1);
                    }
                    isCipher = true;
                    break;

                default:
                    System.err.println("Argumento desconhecido: " + args[i]);
                    System.exit(1);
            }
        }

        
        if (serverHost == null || serverPort < 0
            || user == null || pass == null
            || (isUpload == isDownload)  
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
            } else {
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
            File file = new File(path);
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

        File outDir = new File("fromServer");
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
}
