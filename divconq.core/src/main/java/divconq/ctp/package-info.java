package divconq.ctp;

/*
What is left to do?

- divconq.service.simple.FileServerService take the service found starting at lines 100 and move
  the start up/engage to divconq.ctp.net.CtpServices and take the file handling and make more generic
  try to share the ICommandHandler between client and server
  
  Engage will ask CtpServices for a service, FileServerService needs to implement a service that 
  accepts a "Ctp Claim Token" and then grabs the Adapter from Ctp Services (via Hub) and set its own
  command handler and command mapper
  
- run CtpFClient for tests

- make the uploads and downloads actually work

- add Delete and Move support to server

- fix up how net.CtpHandler and CtpAdapter work together.  try to always have some buffer available for
  next read, and make it some how possible for a Task to request the next stream message without having to 
  leave the main task loop
  
  right now the Task has to "resume" over and over, be nice if we could get more packets per run before sharing the
  thread
  
- SFTP support
  
- network layer TLS and Compression support

- Bus/Gateway support

- UDP/UDT support.  Thinking use UDP except we need TLS and Compression...consider anyway sending around 1MB
  bursts at a time with sequence numbers then ACK process occurs maybe every 50 packets we negotiate ACK and settle missing
  sequence numbers? But STAT happens every second so other side knows where in the sequence they are
  
  there would be "flush" option for saying settle up the ACK now - useful for small commands
  


*/