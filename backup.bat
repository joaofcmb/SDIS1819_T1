set CLASS-PATH=".\build\production\SDIS1819_T1"
set TESTAPP-MAIN="client.TestApp"

set PEER-ID=1
set FILE-PATH="C:\Users\Joao\Downloads\a154ffbcec538a4161a406abf62f5b76-original.pdf"
set REPLICATION=1

java --class-path %CLASS-PATH% %TESTAPP-MAIN% "accesspoint%PEER-ID%" "BACKUP" %FILE-PATH% %REPLICATION%

pause