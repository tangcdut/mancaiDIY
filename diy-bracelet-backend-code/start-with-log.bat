@echo off
echo Starting server with logging...
java -jar diy-server/target/diy-server-1.0-SNAPSHOT.jar 2>&1 | tee -a server-runtime.log
pause
