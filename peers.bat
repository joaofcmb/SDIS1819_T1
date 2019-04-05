set CLASS-PATH=".\build\production\SDIS1819_T1"
set PEER-MAIN="peer.Peer"
set RMI-NAME="accesspoint"

set PEER-NUM=5

for /l %%i in (1, 1, %PEER-NUM%) do (
    start "peer%%i" java --class-path %CLASS-PATH% %PEER-MAIN% "1.0" %%i "%RMI-NAME%%%i" "224.0.0.2" "4441" "224.0.0.3" "442" "224.0.0.4" "4443"
)