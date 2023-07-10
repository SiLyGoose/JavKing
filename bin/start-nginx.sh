nginx -c "$(pwd)/nginx.conf.erb" -p "$(pwd)/";

java -Dserver.port=$PORT $JAVA_OPTS -jar out/artifacts/JavKing-1.1.jar