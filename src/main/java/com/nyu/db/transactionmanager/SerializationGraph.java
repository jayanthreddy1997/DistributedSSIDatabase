package com.nyu.db.transactionmanager;

import com.nyu.db.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SerializationGraph {
    private enum EdgeType {
        RW,
        WR,
        WW
    }

    private static final Logger logger = LoggerFactory.getLogger(SerializationGraph.class);

    private Map<Long, Map<Long, EdgeType>> graph;  // Graph represented as adjacency list

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

    private void addTransaction(Transaction transaction) {
        if (this.graph.isEmpty()) {
            this.graph.put(transaction.getTransactionId(), new HashMap<>());
        } else {
            // Reads are at transaction start time
            // writes are at commit time
            for (long transactionId: graph.keySet()) {
//                for
            }
        }
    }

    private void removeTransaction(Transaction transaction) {
        // TODO: remove transaction from graph
    }

    /**
     * Looks for a cycle with two RW edges in a row
     * @return true if such a cycle is found
     */
    private boolean checkCycle() {
        // TODO

        // If cycle exists, it will contain 2 RW edges in a row, double-checking here

        return true;
    }
}