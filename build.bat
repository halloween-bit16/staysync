@echo off
echo Building StaySync...
call .\mvnw.cmd package -q
echo Done! Run: java -jar staysync.jar
