A fazer:

- isCypher no cliente;
- Tudo a ver com assinaturas
- Rever o método dos booleanos no cliente

- Funcionalidades que tocam também depois no servidor, cifrar e enviar por exemplo(se tiver la)


# Antes de Tudo

javac mycloudutils/*.java mycloud/*.java !!!!!e!!!!! javac mycloudserver/*.java 

# Servidor

java -Djavax.net.ssl.keyStore=../keystores/keystore.server \
     -Djavax.net.ssl.keyStorePassword=epocaespecial \
     -Djavax.net.ssl.trustStore=../keystores/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=epocaespecial \
     -cp . mycloudserver.MyCloudServer 23456



# Cliente

# cifrar
java -cp . mycloud.MyCloud -u alice -p passwordDaAlice -c mensagem.txt -t bob

# upload
java -cp . mycloud.MyCloud -s localhost:23456 -u alice -p passwordDaAlice \
     -e ../mensagem.txt.cifrado ../mensagem.txt.chave.bob

# download
java -cp . mycloud.MyCloud -s localhost:23456 -u bob -p passwordDoBob \
     -r ../mensagem.txt.cifrado ../mensagem.txt.chave.bob



