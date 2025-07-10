package mycloud;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.*;

public class MyCloud {
    public static void main(String[] args) {
    
    String serverHost = null; //Ip servidor
    int    serverPort = -1; //Porto 
    String user       = null;
    String pass       = null;
    boolean isUpload  = false;
    boolean isDownload= false;
    List<String> files= new ArrayList<>();

    // Loop pelos args[]
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
                // tudo o resto até ao fim são ficheiros
                for (int j = i+1; j < args.length; j++) {
                    files.add(args[j]);
                }
                i = args.length;  // sai do for principal
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

    // Validação mínima após o parsing
    if (serverHost == null || serverPort < 0
        || user == null || pass == null
        || (isUpload == isDownload)      // tem de ser um ou outro, não ambos nem nenhum
        || files.isEmpty())
    {
        System.err.println("Uso válido:");
        System.err.println("  -s host:port -u user -p pass -e ficheiro1 [ficheiro2...]");
        System.err.println("  -s host:port -u user -p pass -r ficheiro1 [ficheiro2...]");
        System.exit(1);
    }

    // A partir daqui, tens:
    //   serverHost, serverPort, user, pass,
    //   isUpload==true ⟹ faz upload de 'files'
    //   isDownload==true ⟹ faz download de 'files'
    
}

}
