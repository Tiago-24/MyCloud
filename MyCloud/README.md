
# Credenciais

Bob, passkeystore: epocaespecial, pass Utilizador: passwordDoBob
Alice, passkeystore: epocaespecial, pass Utilizador: passwordDaAlice


# Antes de Tudo

javac mycloudutils/*.java mycloud/*.java 

javac mycloudserver/*.java 

javac -cp mycloudserver/CriarUser.java

# Servidor

java -Djavax.net.ssl.keyStore=../keystores/keystore.server \
     -Djavax.net.ssl.keyStorePassword=epocaespecial \
     -Djavax.net.ssl.trustStore=../keystores/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=epocaespecial \
     -cp . mycloudserver.MyCloudServer 23456



# Server


 java -cp . mycloudserver.MyCloudServer 23456

# Testes para casos positivos

java mycloudserver.CriarUser antonio passwordDoAntonio

# Testes para casos positivos

Repetir o código de criar user:
java mycloudserver.CriarUser antonio passwordDoAntonio





# Cliente

# Testes para casos positivos

# cifrar
java mycloud.MyCloud -u alice -p epocaespecial -c planeamento.pdf -t bob

# Decifrar
java -cp . mycloud.MyCloud -u bob -p epocaespecial -d planeamento.pdf.cifrado

# upload
java -cp . mycloud.MyCloud -s localhost:23456 -u alice -p passwordDaAlice -e planeamento.pdf.cifrado planeamento.pdf.chave.bob

# download
java -cp . mycloud.MyCloud -s localhost:23456 -u bob -p passwordDoBob -r planeamento.pdf.cifrado planeamento.pdf.chave.bob

# Assinar
java -cp . mycloud.MyCloud -u bob -p epocaespecial -a planeamento.pdf

# Verificar
java -cp . mycloud.MyCloud -s localhost:23456 -u bob -p passwordDoBob -e planeamento.pdf.assinatura.bob planeamento.pdf

java -cp . mycloud.MyCloud -s localhost:23456 -u alice -p passwordDaAlice -r planeamento.pdf.assinatura.bob

java -cp . mycloud.MyCloud -u alice -p epocaespecial -v planeamento.pdf.assinatura.bob

---------------------
# corrompe sem alterar o tamanho para a assinatura ser inválida

cp ../fromServer/planeamento.pdf.assinatura.bob ../fromServer/planeamento.pdf.assinatura.bob.bak

dd if=/dev/urandom of=../fromServer/planeamento.pdf.assinatura.bob bs=1 count=1 conv=notrunc

java -cp . mycloud.MyCloud -u alice -p epocaespecial -v planeamento.pdf.assinatura.bob

mv ../fromServer/planeamento.pdf.assinatura.bob.bak ../fromServer/planeamento.pdf.assinatura.bob

java -cp . mycloud.MyCloud -u alice -p epocaespecial -v planeamento.pdf.assinatura.bob



# Testes para casos negativos

java -cp . mycloud.MyCloud -u alice -p epocaespecial -c nada.pdf -t bob

