package mycloudserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

public class MyCloudServer {
    private static final int PORT = 23456;
    private static final File USERS_FILE = new File("users");
    private static final File USERS_HMAC = new File("users.mac");

    public static void main(String[] args) throws Exception {
        // 1) Validar integridade de users via HMAC
        System.out.print("Validando ficheiro de utilizadores… ");
        String macPassword = promptHmacPassword();
        if (!HMAC.check(USERS_FILE, USERS_HMAC, macPassword)) {
            System.err.println("Falha na integridade de 'users'");
            System.exit(1);
        }
        System.out.println("OK");

        // 2) Configurar TLS
        System.setProperty("javax.net.ssl.keyStore", "keystore.server");
        System.setProperty("javax.net.ssl.keyStorePassword", "epocaespecial");

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        try (ServerSocket serverSock = ssf.createServerSocket(PORT)) {
            System.out.println("Servidor TLS a ouvir na porta " + PORT);
            while (true) {
                Socket client = serverSock.accept();
                new ClientHandler(client).start();
            }
        }
    }

    private static String promptHmacPassword() throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword("Password MAC: ");
            return new String(pwd);
        } else {
            System.out.print("Password MAC: ");
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        public ClientHandler(Socket s) { this.socket = s; }

        @Override
        public void run() {
            try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());
            ) {
                // Autenticação
                String user = in.readObject().toString();
                String pass = in.readObject().toString();
                if (!Auth.check(user, pass)) {
                    out.writeObject("ERRO: credenciais inválidas");
                    return;
                }
                out.writeObject("OK: autenticado");

                // Loop de comandos
                while (true) {
                    String cmd = in.readObject().toString();
                    if ("Terminou".equals(cmd)) {
                        out.writeObject("Até breve!");
                        break;
                    }
                    dispatch(cmd, user, in, out);
                }

            } catch (EOFException eof) {
                // cliente desconectou
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void dispatch(String cmd, String user,
                              ObjectInputStream in,
                              ObjectOutputStream out) throws Exception
        {
            // Garante que o storage do user existe
            File userDir = new File("server_storage", user);
            userDir.mkdirs();

            switch (cmd) {
                case "-e": // upload
                    handleUpload(userDir, in, out);
                    break;
                case "-r": // download
                    handleDownload(userDir, in, out);
                    break;
                default:
                    out.writeObject("ERRO: comando desconhecido " + cmd);
            }
        }

        private void handleUpload(File userDir,
                                  ObjectInputStream in,
                                  ObjectOutputStream out) throws Exception
        {
            // Recebe múltiplos ficheiros até "Terminou"
            while (true) {
                String name = in.readObject().toString();
                if ("Terminou".equals(name)) break;
                long size = (Long) in.readObject();

                File dst = new File(userDir, name);
                try (FileOutputStream fos = new FileOutputStream(dst)) {
                    byte[] buf = new byte[4096];
                    long read = 0;
                    while (read < size) {
                        int r = in.read(buf, 0,
                            (int)Math.min(buf.length, size - read));
                        fos.write(buf, 0, r);
                        read += r;
                    }
                }
                out.writeObject("UPLOAD OK: " + name);
            }
        }

        private void handleDownload(File userDir,
                                    ObjectInputStream in,
                                    ObjectOutputStream out) throws Exception
        {
            // Envia múltiplos ficheiros até "Terminou"
            while (true) {
                String name = in.readObject().toString();
                if ("Terminou".equals(name)) break;
                File f = new File(userDir, name);
                if (!f.exists()) {
                    out.writeObject("ERRO: não existe " + name);
                    continue;
                }
                // Header: nome e tamanho
                out.writeObject(f.getName());
                out.writeObject(f.length());

                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                }
                out.flush();
            }
        }
    }

    /** Lê users:salt:hash e autentica passwords salgadas **/
    static class Auth {
        private static final Map<String,String[]> users = loadUsers();

        public static boolean check(String u, String p) {
            String[] parts = users.get(u);
            if (parts == null) return false;
            try {
                String salt = parts[0];
                String storedHash = parts[1];
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt.getBytes("UTF-8"));
                md.update(p.getBytes("UTF-8"));
                byte[] h = md.digest();
                String computed = Base64.getEncoder().encodeToString(h);
                return storedHash.equals(computed);
            } catch (Exception e) {
                return false;
            }
        }

        private static Map<String,String[]> loadUsers() {
            try {
                return Files.readAllLines(USERS_FILE.toPath()).stream()
                    .map(l -> l.split(":", 3))
                    .collect(Collectors.toMap(a -> a[0],
                                              a -> new String[]{a[1], a[2]}));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Validação HMAC-SHA256 de um ficheiro **/
    static class HMAC {
        public static boolean check(File dataFile, File macFile, String pwd) throws Exception {
            // 1) lê todo o ficheiro de dados
            byte[] data = Files.readAllBytes(dataFile.toPath());
            // 2) calcula o HMAC-SHA256 binário
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pwd.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(data);

            // 3) lê o Base64 guardado em users.mac e decodifica para bytes
            String macB64 = new String(Files.readAllBytes(macFile.toPath()), StandardCharsets.UTF_8).trim();
            byte[] actual = Base64.getDecoder().decode(macB64);

            // 4) compara binário ↔ binário
            return MessageDigest.isEqual(expected, actual);
        }
    }
}
