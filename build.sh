# Build Server
javac -d IoTServer/classes -cp IoTServer/src/main/java IoTServer/src/main/java/server/*.java
cp -r IoTServer/src/main/resources/* IoTServer/classes
jar cvfe IoTServer.jar server.IoTServer -C IoTServer/classes .

# Build Client
javac -d IoTDevice/classes -cp IoTDevice/src/main/java IoTDevice/src/main/java/client/*.java
jar cvfe IoTDevice.jar client.IoTDevice -C IoTDevice/classes .


