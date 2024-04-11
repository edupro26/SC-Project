### SERVER ###

# Setup server key store with the server key pair
keytool -genkeypair -alias ServerKeyPair -keyalg RSA -keysize 2048 -keystore keystore.server -storepass alphatango -dname "CN=server, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the server certificate
keytool -exportcert -alias ServerKeyPair -file certServer.cer -keystore keystore.server -storepass alphatango

# --------------------------------------------

### CLIENT 1 ###

# Setup Client 1 keystore with the client 1 key pair
keytool -genkeypair -alias user1 -keyalg RSA -keysize 2048 -keystore keystore.user1 -storepass alphatango -dname "CN=user1, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Setup Client 1 truststore with the server certificate
keytool -importcert -alias ServerKeyPair -file certServer.cer -keystore truststore.user -storepass alphatango -noprompt

#Export the client 1 certificate
keytool -exportcert -alias user1 -file certClient1.cer -keystore keystore.user1 -storepass alphatango

# --------------------------------------------

### CLIENT 2 ###

# Setup Client 2 keystore
keytool -genkeypair -alias user2 -keyalg RSA -keysize 2048 -keystore keystore.user2 -storepass alphatango -dname "CN=user2, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Setup Client 2 truststore
keytool -importcert -alias ServerKeyPair -file certServer.cer -keystore truststore.user -storepass alphatango -noprompt

# Export the client 2 certificate
keytool -exportcert -alias user2 -file certClient2.cer -keystore keystore.user2 -storepass alphatango

### IMPORT CLIENT CERTIFICATES ###

# Import client 1 certificate into  truststore
keytool -importcert -alias user1 -file certClient1.cer -keystore truststore.user -storepass alphatango -noprompt

# Import client 2 certificate into truststore
keytool -importcert -alias user2 -file certClient2.cer -keystore truststore.user -storepass alphatango -noprompt

