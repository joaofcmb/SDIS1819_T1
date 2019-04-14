## Distributed Backup Service

The project was developed using InteliJ IDEA. The project can be viewed and compiled using this IDE.
However, the project is already precompiled and, if necessary for java compatibility reasons, can be
compiled by running the `compile.sh` script.

---

To run the project, follow these steps (All following scripts are in .bat or .sh:

1. Start the rmi registry using the rmiregistry script (on a seperate terminal).
2. Start the peers using the peers script `./peers PEER_NUM PROTOCOL_VERSION`. The peers' id's range from 1 to PEER_NUM
3. Run whichever commands from the TestApp using the following scripts:
    - backup script 	`./backup PEER_ID FILE_PATH REPLICATION_DEGREE`
    - restore script 	`./restore PEER_ID FILE_PATH`
    - delete script 	`./delete PEER_ID FILE_PATH`
    - reclaim script 	`./delete PEER_ID MAX_STORAGE`
    - state script 		`./state PEER_ID`

There are two test files included, if necessary (`8K.jpg` and `pente.mp4`).
