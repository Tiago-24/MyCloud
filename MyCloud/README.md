Server:

java -cp .   -Djavax.net.ssl.keyStore="../keystores/keystore.server"   -Djavax.net.ssl.keyStorePassword=epocaespecial   mycloudserver.MyCloudServer 23456

java -cp .   -Djavax.net.ssl.keyStore="../keystores/keystore.server"   -Djavax.net.ssl.keyStorePassword=epocaespecial   mycloudserver.MyCloudServer 23456



Cliente:

javac --release 21 mycloud/MyCloud.java

java -cp .   -Djavax.net.ssl.trustStore="../keystores/truststore.jks"   -Djavax.net.ssl.trustStorePassword=epocaespecial   mycloud.MyCloud     -s localhost:23456     -u alice     -p senhaAlice     -e ../exemplo.txt

java -cp .   -Djavax.net.ssl.trustStore="../keystores/truststore.jks"   -Djavax.net.ssl.trustStorePassword=epocaespecial   mycloud.MyCloud     -s localhost:23456     -u alice     -p senhaAlice     -r exemplo.txt