package mycloudserver;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CriarUser {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Uso: java mycloudserver.CriarUser <username> <password>");
            System.exit(1);
        }
        String username = args[0];
        String password = args[1];

        // 1) Ler password do MAC
        Console console = System.console();
        String macPassword;
        if (console != null) {
            char[] macChars = console.readPassword("Password MAC: ");
            macPassword = new String(macChars);
        } else {
            System.out.print("Password MAC: ");
            macPassword = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        File usersFile = new File("users");
        File macFile   = new File("users.mac");

        // 2) Verificar integridade do ficheiro users
        if (usersFile.exists()) {
            if (!macFile.exists()) {
                System.err.println("Erro: users.mac não encontrado");
                System.exit(1);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(macPassword.getBytes("UTF-8"), "HmacSHA256"));
            byte[] expected = mac.doFinal(Files.readAllBytes(usersFile.toPath()));
            String expectedB64 = Base64.getEncoder().encodeToString(expected);

            String actualB64 = new String(Files.readAllBytes(macFile.toPath()), "UTF-8").trim();
            if (!expectedB64.equals(actualB64)) {
                System.err.println("Erro: ficheiro users corrompido (MAC inválido)");
                System.exit(1);
            }
        }

        // 3) Carregar linhas existentes e verificar se já existe user
        List<String> lines = new ArrayList<>();
        if (usersFile.exists()) {
            lines = Files.readAllLines(usersFile.toPath());
            for (String line : lines) {
                if (line.startsWith(username + ":")) {
                    System.err.println("Erro: utilizador já existe");
                    System.exit(1);
                }
            }
        }

        // 4) Gerar salt e hash
        byte[] saltBytes = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(saltBytes);
        String saltB64 = Base64.getEncoder().encodeToString(saltBytes);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(saltB64.getBytes("UTF-8"));
        md.update(password.getBytes("UTF-8"));
        String hashB64 = Base64.getEncoder().encodeToString(md.digest());

        // 5) Adicionar nova linha user:salt:hash
        lines.add(username + ":" + saltB64 + ":" + hashB64);
        Files.write(usersFile.toPath(), lines);

        // 6) Recalcular e gravar novo MAC
        Mac mac2 = Mac.getInstance("HmacSHA256");
        mac2.init(new SecretKeySpec(macPassword.getBytes("UTF-8"), "HmacSHA256"));
        byte[] macBytes = mac2.doFinal(Files.readAllBytes(usersFile.toPath()));
        String macB64 = Base64.getEncoder().encodeToString(macBytes);
        Files.write(macFile.toPath(), macB64.getBytes("UTF-8"));

        System.out.println("Utilizador '" + username + "' criado com sucesso.");
    }
}
