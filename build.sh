# Compile Client
javac -d out/classes/IoTDevice -cp src/main/java src/main/java/common/*.java
javac -d out/classes/IoTDevice -cp src/main/java src/main/java/client/*.java

# Compile Server
javac -d out/classes/IoTServer -cp src/main/java src/main/java/common/*.java
javac -d out/classes/IoTServer -cp src/main/java src/main/java/server/*.java

# Build Client and Server
jar cvfe out/IoTServer.jar server.IoTServer -C out/classes/IoTServer .
jar cvfe out/IoTDevice.jar client.IoTDevice -C out/classes/IoTDevice .

# Store a client reference copy
mkdir -p out/client-copy
cp out/IoTDevice.jar -d out/client-copy

# Copy stores and certificates
cp stores/* -d out/
cp certificates/* -d out/