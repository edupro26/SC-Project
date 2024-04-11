rm -rf out/

# Remove server files
rm -rf server-files/*

# Remove server keystore
rm -f keystore.server

# Remove client 1 keystore
rm -f keystore.user1

# Remove client 1 truststore
rm -f truststore.user1

# Remove clients truststore
rm -f truststore.user

# Remove client 2 keystore
rm -f keystore.user2

# Remove client 2 truststore
rm -f truststore.user2

# Remove server certificate
rm -f certServer.cer

# Remove client 1 certificate
rm -f certClient1.cer

# Remove client 2 certificate
rm -f certClient2.cer