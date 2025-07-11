package mycloudserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

public class MyCloudServer {
    private static final File STORAGE_DIR = new File("server_storage");
    private static final File USERS_FILE   = new File("../config/users");
    private static final File USERS_HMAC   = new File("../config/users.mac");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java mycloudserver.MyCloudServer <porta_TCP>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);

        // 1) Validar integridade de users via HMAC
        System.out.print("Validando ficheiro de utilizadores… ");
        String macPwd = promptHmacPassword();
        if (!HMAC.check(USERS_FILE, USERS_HMAC, macPwd)) {
            System.err.println("Falha na integridade de 'users'");
            System.exit(1);
        }
        System.out.println("OK");

        // 2) Configurar TLS
        System.setProperty("javax.net.ssl.keyStore",
            System.getProperty("keystore.path", "../keystores/keystore.server"));
        System.setProperty("javax.net.ssl.keyStorePassword",
            System.getProperty("keystore.pass", "epocaespecial"));

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        try (ServerSocket serverSock = ssf.createServerSocket(port)) {
            System.out.println("Servidor TLS a ouvir na porta " + port);
            while (true) {
                new ClientHandler(serverSock.accept()).start();
            }
        }
    }

    private static String promptHmacPassword() throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword("Password MAC: ");
            String s = new String(pwd);
            Arrays.fill(pwd, ' ');
            return s;
        } else {
            System.out.print("Password MAC: ");
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        ClientHandler(Socket s) { this.socket = s; }

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
                    dispatch(cmd, in, out);
                }
            } catch (EOFException eof) {
                // cliente desconectou
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void dispatch(String cmd,
                              ObjectInputStream in,
                              ObjectOutputStream out) throws Exception {
            // Garante que a pasta de armazenamento existe
            if (!STORAGE_DIR.exists() && !STORAGE_DIR.mkdirs()) {
                throw new IOException("Não foi possível criar diretório de armazenamento");
            }

            switch (cmd) {
                case "-e": // upload
                    System.out.println("Upload de ficheiros");
                    handleUpload(in, out);
                    break;
                case "-r": // download
                    System.out.println("Download de ficheiros");
                    handleDownload(in, out);
                    break;
                default:
                    out.writeObject("ERRO: comando desconhecido " + cmd);
            }
        }

        private void handleUpload(ObjectInputStream in,
                                  ObjectOutputStream out) throws Exception {
            // 1) Receber lista de nomes
            List<String> names = new ArrayList<>();
            while (true) {
                String raw = in.readObject().toString();
                if ("Terminou".equals(raw)) break;
                names.add(Paths.get(raw).getFileName().toString());
            }
            // 2) Verificar existência prévia
            for (String name : names) {
                if (new File(STORAGE_DIR, name).exists()) {
                    out.writeObject("ERRO: já existe " + name);
                    System.out.println("ERRO: já existe " + name);
                    return;
                }
            }
            // 3) Permitir envio
            out.writeObject("OK: pode enviar");

            // 4) Receber ficheiros e gravar
            for (String name : names) {
                long size = (Long) in.readObject();
                File dst = new File(STORAGE_DIR, name);
                try (OutputStream os = new FileOutputStream(dst)) {
                    byte[] buf = new byte[4096];
                    long read = 0;
                    while (read < size) {
                        int r = in.read(buf, 0,
                            (int) Math.min(buf.length, size - read));
                        os.write(buf, 0, r);
                        read += r;
                    }
                }
                out.writeObject("UPLOAD OK: " + name);
                System.out.println("Ficheiro recebido: " + name + " (" + size + " bytes)");
            }
        }

        private void handleDownload(ObjectInputStream in,
                                    ObjectOutputStream out) throws Exception {
            // 1) Receber lista de nomes
            List<String> names = new ArrayList<>();
            while (true) {
                String raw = in.readObject().toString();
                if ("Terminou".equals(raw)) break;
                names.add(Paths.get(raw).getFileName().toString());
            }
            // 2) Enviar cada ficheiro
            for (String name : names) {
                File f = new File(STORAGE_DIR, name);
                if (!f.exists()) {
                    out.writeObject("ERRO: não existe " + name);
                    System.out.println("ERRO: não existe " + name);
                    continue;
                }
                out.writeObject(name);
                out.writeObject(f.length());
                System.out.println("A enviar ficheiro: " + name + " (" + f.length() + " bytes)");
                try (InputStream is = new FileInputStream(f)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                }
                out.flush();
            }
        }
    }

    static class Auth {
        public static boolean check(String u, String p) {
            Map<String, String[]> users = loadUsers();
            String[] parts = users.get(u);
            if (parts == null) return false;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(parts[0].getBytes(StandardCharsets.UTF_8));
                md.update(p.getBytes(StandardCharsets.UTF_8));
                String computed = Base64.getEncoder().encodeToString(md.digest());
                return computed.equals(parts[1]);
            } catch (Exception e) {
                return false;
            }
        }

        private static Map<String, String[]> loadUsers() {
            try {
                List<String> lines = Files.readAllLines(USERS_FILE.toPath(), StandardCharsets.UTF_8);
                Map<String, String[]> map = new HashMap<>();
                for (String l : lines) {
                    String[] a = l.split(":", 3);
                    if (a.length == 3) map.put(a[0], new String[]{a[1], a[2]});
                }
                return map;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static class HMAC {
        public static boolean check(File dataFile, File macFile, String pwd) throws Exception {
            byte[] data = Files.readAllBytes(dataFile.toPath());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pwd.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(data);
            String macB64 = new String(Files.readAllBytes(macFile.toPath()), StandardCharsets.UTF_8).trim();
            byte[] actual = Base64.getDecoder().decode(macB64);
            return MessageDigest.isEqual(expected, actual);
        }
    }
}
