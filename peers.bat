@echo off

set CLASS-PATH=".\build\production\SDIS1819_T1"
set PEER-MAIN="peer.Peer"
set RMI-NAME="accesspoint"

set PEER-NUM=%1
set VERSION=%2

for /l %%i in (1, 1, %PEER-NUM%) do (
    start "peer%%i" java --class-path %CLASS-PATH% %PEER-MAIN% %VERSION% %%i "%RMI-NAME%%%i" "224.0.0.2" "4441" "224.0.0.3" "442" "224.0.0.4" "4443"
)