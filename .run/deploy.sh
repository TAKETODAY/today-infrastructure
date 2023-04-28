cd ../
mvn deploy -DskipTests=true -P release -Dfile.encoding=UTF-8 \
-pl '!integration-tests'

