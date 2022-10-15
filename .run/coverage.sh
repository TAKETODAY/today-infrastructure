cd ../
#mvn jacoco:report-aggregate coveralls:report -q -Pcoverage -Dcoverage=true -DCI=true \
#mvn test -B jacoco:report-aggregate coveralls:report -q -Pcoverage -Dcoverage=true -DCI=true\
#-DrepoToken=Z9f4z57lrxZMLfSlhD3EEEW7glMZPsddN -DserviceName=github -e

#mvn test -B coveralls:report -q \
#-DrepoToken=Z9f4z57lrxZMLfSlhD3EEEW7glMZPsddN -DserviceName=github -Pcoverage -Dcoverage=true -DCI=true

mvn clean test -DCI=true -Pcoverage -pl integration-tests,today-beans,today-context
