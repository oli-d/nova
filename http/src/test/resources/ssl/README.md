This is how key and truststore were created:

1. Create the certificate and serverside keystore
keytool -genkey -noprompt -trustcacerts -keyalg RSA -alias testkey -keystore keystore.jks -storepass storepass -ext SAN=DNS:localhost,IP:127.0.0.1 -validity 9999

2. Export server certificate
keytool -export -alias testkey -storepass storepass -file server.cer -keystore keystore.jks

3. Import server certificate into new truststore to use on the client side
keytool -import -v -trustcacerts -alias testkey -file server.cer -keystore truststore.jks


