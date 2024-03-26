# Build Server
javac -d out/server/classes -cp IoTServer/src/main/java IoTServer/src/main/java/server/*.java
cp -r IoTServer/src/main/resources/* out/server/classes
jar cvfe out/IoTServer.jar server.IoTServer -C out/server/classes .

# Build Client
javac -d out/client/classes -cp IoTDevice/src/main/java IoTDevice/src/main/java/client/*.java
jar cvfe out/IoTDevice.jar client.IoTDevice -C out/client/classes .