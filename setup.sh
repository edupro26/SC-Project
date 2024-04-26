# Create folders to keep stores and certificates
mkdir -p stores
mkdir -p certificates

# --------------------------------------------
### SERVER ###

# Setup server key store with the server key pair
keytool -genkeypair -alias ServerKeyPair -keyalg RSA -keysize 2048 -keystore stores/keystore.server -storepass alphatango -dname "CN=server, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the server certificate
keytool -exportcert -alias ServerKeyPair -file certificates/certServer.cer -keystore stores/keystore.server -storepass alphatango

# --------------------------------------------
### CLIENT 1 ###

# Setup client1 keystore with the client1 key pair
keytool -genkeypair -alias tiago.miguel.tiago@gmail.com -keyalg RSA -keysize 2048 -keystore stores/keystore.user1 -storepass alphatango -dname "CN=user1, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the client1 certificate
keytool -exportcert -alias tiago.miguel.tiago@gmail.com -file certificates/certClient1.cer -keystore stores/keystore.user1 -storepass alphatango

# --------------------------------------------
### CLIENT 2 ###

# Setup client2 keystore with the client2 key pair
keytool -genkeypair -alias edupro26@gmail.com -keyalg RSA -keysize 2048 -keystore stores/keystore.user2 -storepass alphatango -dname "CN=user2, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the client2 certificate
keytool -exportcert -alias edupro26@gmail.com -file certificates/certClient2.cer -keystore stores/keystore.user2 -storepass alphatango

# --------------------------------------------
### CLIENT 3 ###

# Setup client3 keystore with the client2 key pair
keytool -genkeypair -alias fc57551@alunos.fc.ul.pt -keyalg RSA -keysize 2048 -keystore stores/keystore.user3 -storepass alphatango -dname "CN=user3, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=PT" -validity 365

# Export the client3 certificate
keytool -exportcert -alias fc57551@alunos.fc.ul.pt -file certificates/certClient3.cer -keystore stores/keystore.user3 -storepass alphatango

# --------------------------------------------
### IMPORT CLIENT CERTIFICATES ###

# Import server certificate into truststore
keytool -importcert -alias ServerKeyPair -file certificates/certServer.cer -keystore stores/truststore.user -storepass alphatango -noprompt

# Import client1 certificate into truststore
keytool -importcert -alias tiago.miguel.tiago@gmail.com -file certificates/certClient1.cer -keystore stores/truststore.user -storepass alphatango -noprompt

# Import client2 certificate into truststore
keytool -importcert -alias edupro26@gmail.com -file certificates/certClient2.cer -keystore stores/truststore.user -storepass alphatango -noprompt

# Import client3 certificate into truststore
keytool -importcert -alias fc57551@alunos.fc.ul.pt -file certificates/certClient3.cer -keystore stores/truststore.user -storepass alphatango -noprompt
