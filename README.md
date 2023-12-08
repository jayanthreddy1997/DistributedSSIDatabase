# DistributedSSIDatabase
Distributed database, complete with serializable snapshot isolation, replication, and failure recovery

Instructions to run a simulation:
- run `mvn clean package` to compile and generate the jar file (located at target/DistributedSSI-1.0-SNAPSHOT.jar)
- run the simulation for an input file `input1.txt` using command `java -jar DistributedSSI-1.0-SNAPSHOT.jar input1.txt` from the appropriate folder

Disclaimer: Transactions are required to be named in the format Ti (where i=1,2,3...). 
Variables need to be named xj (where j=1,2,3...)
