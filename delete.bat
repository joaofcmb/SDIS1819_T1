@echo off

set CLASS-PATH=".\build\production\SDIS1819_T1"
set TESTAPP-MAIN="client.TestApp"

set PEER-ID=%1
set FILE-PATH=%2

java --class-path %CLASS-PATH% %TESTAPP-MAIN% "accesspoint%PEER-ID%" "DELETE" %FILE-PATH%