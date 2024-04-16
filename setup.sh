# Cleaning any existing building files
chmod +x clean.sh
./clean.sh

# Build server and client applications
chmod +x build.sh
./build.sh

# --------------------------------------------
### SERVER ###

# Setup server key store with the server key pair
keytool -genkeypair -alias ServerKeyPair -keyalg RSA -keysize 2048 -keystore keystore.server -storepass alphatango -dname "CN=server, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the server certificate
keytool -exportcert -alias ServerKeyPair -file certServer.cer -keystore keystore.server -storepass alphatango

# --------------------------------------------
### CLIENT 1 ###

# Setup client1 keystore with the client1 key pair
keytool -genkeypair -alias user1 -keyalg RSA -keysize 2048 -keystore keystore.user1 -storepass alphatango -dname "CN=user1, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the client1 certificate
keytool -exportcert -alias user1 -file certClient1.cer -keystore keystore.user1 -storepass alphatango

# --------------------------------------------
### CLIENT 2 ###

# Setup client2 keystore with the client2 key pair
keytool -genkeypair -alias user2 -keyalg RSA -keysize 2048 -keystore keystore.user2 -storepass alphatango -dname "CN=user2, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the client2 certificate
keytool -exportcert -alias user2 -file certClient2.cer -keystore keystore.user2 -storepass alphatango

# --------------------------------------------
### IMPORT CLIENT CERTIFICATES ###

# Import server certificate into truststore
keytool -importcert -alias ServerKeyPair -file certServer.cer -keystore truststore.user -storepass alphatango -noprompt

# Import client1 certificate into truststore
keytool -importcert -alias user1 -file certClient1.cer -keystore truststore.user -storepass alphatango -noprompt

# Import client2 certificate into truststore
keytool -importcert -alias user2 -file certClient2.cer -keystore truststore.user -storepass alphatango -noprompt
