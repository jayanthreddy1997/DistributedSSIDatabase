package com.nyu.db.transactionmanager;

import com.nyu.db.model.*;
import com.nyu.db.utils.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SerializationGraph {

    private static final Logger logger = LoggerFactory.getLogger(SerializationGraph.class);

    private Map<Transaction, Set<Transaction>> graph;  // Graph represented as adjacency list

    public SerializationGraph() {
        this.graph = new HashMap<>();
    }

    public boolean addTransactionAndRunChecks(Transaction transaction) {
        this.addTransaction(transaction);

        boolean consistent = this.checkCycle();
        if (!consistent) {
            logger.info("Detected cycle with 2 consecutive RW edges upon adding T"+transaction.getTransactionId()+"to serialization graph.");
            this.removeTransaction(transaction);
        }
        return consistent;
    }

    private void addTransaction(Transaction t1) {
        this.graph.put(t1, new HashSet<>());

        for (Transaction t2: this.graph.keySet()) {
            for (SymbolOperation o2: t2.getOperations()) {
                for (SymbolOperation o1: t1.getOperations()) {
                    if (o1.getVariableId()==o2.getVariableId()) {
                        if (o1 instanceof WriteOperation && o2 instanceof WriteOperation) {
                            // WW edge from t2 to t1
                            this.graph.get(t2).add(t1);
                        } else if (o1 instanceof WriteOperation && o2 instanceof ReadOperation) {
                            if (t2.getStartTimestamp() <= TimeManager.getTime()) {
                                // RW edge from t2 to t1
                                this.graph.get(t2).add(t1);
                            } else {
                                // WR edge from t1 to t2
                                this.graph.get(t1).add(t2);
                            }
                        } else if (o1 instanceof ReadOperation && o2 instanceof WriteOperation) {
                            if (t2.getCommitTimestamp() <= t1.getStartTimestamp()) {
                                // WR edge from t2 to t1
                                this.graph.get(t2).add(t1);
                            } else {
                                // RW edge from t1 to t2
                                this.graph.get(t1).add(t2);
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeTransaction(Transaction transaction) {
        this.graph.forEach((t, m) -> m.remove(transaction));
        this.graph.remove(transaction);
    }

    /**
     * Sufficient to check any cycle. Since, if cycle exists it will contain two RW edges in a row.
     * @return true if a cycle is found
     */
    private boolean checkCycle() {


        return true;
    }
}