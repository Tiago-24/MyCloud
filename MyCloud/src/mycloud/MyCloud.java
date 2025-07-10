package mycloud;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.*;

public class MyCloud {
    public static void main(String[] args) throws Exception {
        String serverHost = null; // IP ou hostname do servidor
        int    serverPort = -1;   // Porta TCP
        String user       = null;
        String pass       = null;
        boolean isUpload  = false;
        boolean isDownload= false;
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

                default:
                    System.err.println("Argumento desconhecido: " + args[i]);
                    System.exit(1);
            }
        }

        // 2) Validação mínima
        if (serverHost == null || serverPort < 0
            || user == null || pass == null
            || (isUpload == isDownload)  // deve ser um ou outro
            || files.isEmpty())
        {
            System.err.println("Uso válido:");
            System.err.println("  -s host:port -u user -p pass -e ficheiro1 [ficheiro2...]");
            System.err.println("  -s host:port -u user -p pass -r ficheiro1 [ficheiro2...]");
            System.exit(1);
        }

        // 3) Configurar TrustStore para TLS
        String tsRelPath = "../keystores/truststore.jks";
        String tsAbPath  = new File(tsRelPath).getAbsolutePath();
        System.setProperty("javax.net.ssl.trustStore",   tsAbPath);
        System.setProperty("javax.net.ssl.trustStorePassword", "epocaespecial");
        System.setProperty("javax.net.ssl.trustStoreType",     "JKS");

        // 4) Abrir SSLSocket e forçar handshake
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) sf.createSocket(serverHost, serverPort)) {
            socket.startHandshake();

            // 5) Abrir streams de objeto
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());

            // 6) Autenticação
            out.writeObject(user);
            out.writeObject(pass);
            out.flush();
            String authResp = in.readObject().toString();
            if (!authResp.startsWith("OK")) {
                System.err.println("Autenticação falhou: " + authResp);
                return;
            }
            System.out.println("Autenticado com sucesso.");

            // Próximos passos: chamar doUpload(files, out, in) se isUpload
            // ou doDownload(files, out, in) se isDownload
        }
    }

    // Métodos doUpload(...) e doDownload(...) virão aqui
}
