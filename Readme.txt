
This is a distributed file sharing and management system, all commands given in the assignment work properly. 
Multiple clients can log in at the same time and operate, hence multithreaded server has been implemented.

To incorporate the "share_msg" functionality, and delivering msgs in real time, a multithreaded client has been implemented. Where one thread of the client is always listening for any new msgs from the server.

The ports and addresses are hardcoded, hence might need modification if not run on the same machine.

The command share msg has the following syntax

share_msg [groupname] [msg]

To run the client- java client [port] [ip]
To run the server- java server
