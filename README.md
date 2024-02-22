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
1. At commit time, for a read-write transaction, only if none of the sites accessed by the transaction went down during
the transaction do we commit. Else, we abort. Note: for read-only transactions, they return data of the latest version 
as of the transaction begin time, so this rule does not hold, i.e., read-only transactions are as if they commit at 
transaction start time.
2. Upon recovery of a site s, all non-replicated variables are available for reads and writes. 
3. After recovering, the site makes the replicated variables available for writing immediately. 
However, a read from a transaction that begins after the recovery of site s for a replicated variable x will not be 
allowed at s until a committed write to x takes place on s.

## Instructions to run
- run `mvn clean package` to compile and generate the jar file (located at target/DistributedSSI-1.0-SNAPSHOT.jar)
- run the simulation for an input file `input1.txt` using command `java -jar DistributedSSI-1.0-SNAPSHOT.jar input1.txt`
from the appropriate folder (Reference `src\resources\inputs\test*.txt` for input format)

## Design details
The system can be divided into three major modules: 
1. **Transaction Manager**  
This is the central manager for transactions that delegates parts of transactions to relevant data managers. 
The transaction manager also checks commit conditions such as first committer wins (calls Data Manager for this check),
no cycle with 2 consecutive RW edges in the serialization graph, and that a transaction aborts if it writes an item at 
a site and the site then fails(Available copies condition). Transaction manager then orders the data managers to commit 
their uncommitted writes (if the commit is valid).
2. **Data Manager**  
The data manager handles all operations in the data node. It holds a workspace for each transaction to hold uncommitted 
values in-memory(gets lost on failure). It also has access to the committed snapshots of variables. The main read, 
write, and commit operations on the data are performed here. It also runs read consistency checks like rejecting a read
if the site went down between the last commit and the beginning of current transaction(unless there was a local write in
the same transaction after the site came up). It also ensures that if a site goes down after the transaction began, we 
don't respond to reads until we see a write.
3. **Simulation**  
This component is solely for testing the system in a simulated environment using inputs from test files defined in 
`src\test\resources\inputs`. The simulation interacts with the database through the Transaction Manager.
