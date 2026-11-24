#!/bin/sh
# Render tells the app which port to use through $PORT; Tomcat defaults to 8080.
sed -i "s/port=\"8080\"/port=\"${PORT:-8080}\"/" /usr/local/tomcat/conf/server.xml
exec catalina.sh run
