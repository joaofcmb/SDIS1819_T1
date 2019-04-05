set CLASS-PATH=".\build\production\SDIS1819_T1"
set TESTAPP-MAIN="client.TestApp"

set PEER-ID=%1
set FILE-PATH=%2
set REPLICATION=%3

java --class-path %CLASS-PATH% %TESTAPP-MAIN% "accesspoint%PEER-ID%" "BACKUP" %FILE-PATH% %REPLICATION%