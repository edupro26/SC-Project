# Compile Client
javac -d out/classes/IoTDevice -cp src/main/java src/main/java/common/*.java
javac -d out/classes/IoTDevice -cp src/main/java src/main/java/client/*.java

# Compile Server
javac -d out/classes/IoTServer -cp src/main/java src/main/java/common/*.java
javac -d out/classes/IoTServer -cp src/main/java src/main/java/server/*.java

# Copy resources
cp -r src/main/resources/* out/classes/IoTServer

# Build Client and Server
jar cvfe out/IoTServer.jar server.IoTServer -C out/classes/IoTServer .
jar cvfe out/IoTDevice.jar client.IoTDevice -C out/classes/IoTDevice .

# Store a client reference copy
mkdir -p out/classes/IoTServer/client-copy
cp out/IoTDevice.jar -d out/classes/IoTServer/client-copy