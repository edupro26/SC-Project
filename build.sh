# Build Server
javac -d server-app/classes -cp src/main/java src/main/java/common/*.java
javac -d server-app/classes -cp src/main/java src/main/java/server/*.java
cp -r src/main/resources/* server-app/classes
jar cvfe server-app/IoTServer.jar server.IoTServer -C server-app/classes .

# Build Client
javac -d client-app/classes -cp src/main/java src/main/java/common/*.java
javac -d client-app/classes -cp src/main/java src/main/java/client/*.java
jar cvfe client-app/IoTDevice.jar client.IoTDevice -C client-app/classes .

# Store a client reference copy
mkdir server-app/classes/client-copy
cp client-app/IoTDevice.jar -d server-app/classes/client-copy