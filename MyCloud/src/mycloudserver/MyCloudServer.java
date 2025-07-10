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
    // Paths relativos ao diretório 'server' (local onde este .class é executado)
    private static final File USERS_FILE = new File("../config/users");
    private static final File USERS_HMAC = new File("../config/users.mac");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java mycloudserver.MyCloudServer <porta_TCP>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);

        // 1) Validar integridade de users via HMAC
        System.out.print("Validando ficheiro de utilizadores… ");
        String macPassword = promptHmacPassword();
        if (!HMAC.check(USERS_FILE, USERS_HMAC, macPassword)) {
            System.err.println("Falha na integridade de 'users'");
            System.exit(1);
        }
        System.out.println("OK");

        // 2) Configurar TLS: usar JKS em keystores/keystore.server
        System.setProperty("javax.net.ssl.keyStore",
            System.getProperty("keystore.path", "../keystores/keystore.server"));
        System.setProperty("javax.net.ssl.keyStorePassword",
            System.getProperty("keystore.pass", "epocaespecial"));

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        try (ServerSocket serverSock = ssf.createServerSocket(port)) {
            System.out.println("Servidor TLS a ouvir na porta " + port);
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
        public ClientHandler(Socket s) { this.socket = s; }

        @Override
        public void run() {
            try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());
            ) {
                // Autenticação dinâmico, recarrega users a cada acesso
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
                              ObjectOutputStream out) throws Exception {
            // Diretório de armazenamento relativo: server/server_storage/<user>
            File userDir = new File("server_storage", user);
            if (!userDir.exists() && !userDir.mkdirs()) {
                throw new IOException("Não foi possível criar diretório de usuário: " + userDir.getPath());
            }

            switch (cmd) {
                case "-e": // upload atomizado
                    handleUpload(userDir, in, out);
                    break;
                case "-r": // download simples
                    handleDownload(userDir, in, out);
                    break;
                default:
                    out.writeObject("ERRO: comando desconhecido " + cmd);
            }
        }

        private void handleUpload(File userDir,
                                  ObjectInputStream in,
                                  ObjectOutputStream out) throws Exception {
            // 1) Receber lista de nomes até 'Terminou'
            List<String> names = new ArrayList<>();
            while (true) {
                String name = in.readObject().toString();
                if ("Terminou".equals(name)) break;
                names.add(name);
            }
            // 2) Verificar existência prévia
            for (String name : names) {
                if (new File(userDir, name).exists()) {
                    out.writeObject("ERRO: já existe " + name + ", abortando upload");
                    return;
                }
            }
            out.writeObject("OK: pode enviar");

            // 3) Receber ficheiros para temporários
            Map<String,Path> tempFiles = new LinkedHashMap<>();
            for (String name : names) {
                long size = (Long) in.readObject();
                Path temp = Files.createTempFile("upload-", null);
                try (OutputStream fos = Files.newOutputStream(temp)) {
                    byte[] buf = new byte[4096];
                    long read = 0;
                    while (read < size) {
                        int r = in.read(buf, 0, (int)Math.min(buf.length, size - read));
                        fos.write(buf, 0, r);
                        read += r;
                    }
                }
                tempFiles.put(name, temp);
            }

            // 4) Mover para diretório final e confirmar
            for (Map.Entry<String,Path> e : tempFiles.entrySet()) {
                Path dst = userDir.toPath().resolve(e.getKey());
                Files.move(e.getValue(), dst, StandardCopyOption.ATOMIC_MOVE);
                out.writeObject("UPLOAD OK: " + e.getKey());
            }
        }

        private void handleDownload(File userDir,
                                    ObjectInputStream in,
                                    ObjectOutputStream out) throws Exception {
            while (true) {
                String name = in.readObject().toString();
                if ("Terminou".equals(name)) break;
                File f = new File(userDir, name);
                if (!f.exists()) {
                    out.writeObject("ERRO: não existe " + name);
                    continue;
                }
                out.writeObject(f.getName());
                out.writeObject(f.length());
                try (InputStream fis = new FileInputStream(f)) {
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

    /** Autenticação dinâmica, recarrega users a cada chamada **/
    static class Auth {
        public static boolean check(String u, String p) {
            Map<String,String[]> users = loadUsers();
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

        private static Map<String,String[]> loadUsers() {
            try {
                List<String> lines = Files.readAllLines(USERS_FILE.toPath(), StandardCharsets.UTF_8);
                Map<String,String[]> map = new HashMap<>();
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

    /** Validação HMAC-SHA256 de um ficheiro **/
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
