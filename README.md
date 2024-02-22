# DistributedSSIDatabase

Distributed database, complete with serializable snapshot isolation, replication, and failure recovery

## SSI
Serializable Snapshot Isolation is a form of Multi-version Read Consistency protocol. This means that a Read-only
transaction will read committed data as of the time of the start of the transaction.

In Snapshot Isolation, for Read-Write transactions, any writes follow the first committer wins rule at commit time, 
i.e., a transaction will only commit if no data items it wrote to were committed to by some other concurrent 
transactions.

To ensure a serializable execution, we maintain a Serialization graph to detect cycles. Edges in the graph are labelled 
as r-w, w-w, or w-r edges. Cycle detection looks for a cycle with 2 r-w edges in a row; if such a cycle is found at 
commit stage, we abort the transaction being committed (since it is inducing a cycle).

## Available Copies
We implement a partially replicated database, with some variables replicated across multiple sites. Our implementation 
extends easily to any form of replication to any number of variables.

The available copies algorithm we have implemented does the following:
1. At commit time, for a read-write transaction, only if none of the sites accessed by the transaction went down during the transaction do we commit. Else, we abort. Note: for read-only transactions, they return data of the latest version as of the transaction begin time, so this rule does not hold, i.e., read-only transactions are as if they commit at transaction start time.
## Instructions to run
- run `mvn clean package` to compile and generate the jar file (located at target/DistributedSSI-1.0-SNAPSHOT.jar)
- run the simulation for an input file `input1.txt` using command `java -jar DistributedSSI-1.0-SNAPSHOT.jar input1.txt`
from the appropriate folder (Reference `src\resources\inputs\test*.txt` for input format)
