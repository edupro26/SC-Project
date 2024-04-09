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
keytool -importcert -alias ServerKeyPair -file certServer.cer -keystore truststore.user1 -storepass alphatango -noprompt

# --------------------------------------------

### CLIENT 2 ###

# Setup Client 2 keystore
keytool -genkeypair -alias user2 -keyalg RSA -keysize 2048 -keystore keystore.user2 -storepass alphatango -dname "CN=user2, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Setup Client 2 truststore
keytool -importcert -alias ServerKeyPair -file certServer.cer -keystore truststore.user2 -storepass alphatango -noprompt

