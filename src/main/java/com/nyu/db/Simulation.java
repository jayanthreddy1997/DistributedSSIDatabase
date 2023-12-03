package com.nyu.db;
import com.nyu.db.datamanager.DataManager;
import com.nyu.db.datamanager.impl.DataManagerImpl;
import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.Transaction;
import com.nyu.db.model.WriteOperation;
import com.nyu.db.transactionmanager.TransactionManager;
import com.nyu.db.transactionmanager.impl.TransactionManagerImpl;

import com.nyu.db.utils.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class Simulation {

    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);
    private static final int NUM_VARIABLES = 20;
    private static final int NUM_SITES = 10;
    private TransactionManager transactionManager;

    private void initializeDatabaseManagers() {
        List<DataManager> dataManagers = new ArrayList<>();
        for (int siteId=1; siteId<=NUM_SITES; siteId++) {
            dataManagers.add(new DataManagerImpl(siteId));
        }
        for (int variableId=1; variableId<=NUM_VARIABLES; variableId+=1) {
            if (variableId % 2 == 1) {
                // Available only at one site
                int siteId = 1 + variableId % NUM_SITES;
                dataManagers.get(siteId-1).registerVariable(variableId, variableId*10);
            } else {
                // Available at all sites
                for (DataManager dm: dataManagers) {
                    dm.registerVariable(variableId, variableId*10);
                }
            }
        }
        this.transactionManager = new TransactionManagerImpl(dataManagers);
    }

    public void run(String inputFilePath) {
        logger.info("Starting simulation with input from "+inputFilePath);
        this.initializeDatabaseManagers();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String token;
            while ((token = br.readLine()) != null) {
                if (token.isEmpty() || token.startsWith("//"))
                    continue;
                if (token.contains("//"))
                    token = token.substring(0, token.indexOf("//")).trim();
                String[] params = token.substring(token.indexOf("(") + 1, token.indexOf(")")).split(",");

                if (token.startsWith("begin")) {
                    String transactionName = params[0].trim();
                    if (!transactionName.startsWith("T")) {
                        String errMsg = "Transaction names are required to start with T followed by an integer";
                        logger.error(errMsg);
                        throw new RuntimeException(errMsg);
                    }
                    this.transactionManager.createTransaction(Long.parseLong(transactionName.substring(1)));
                } else if (token.startsWith("W") || token.startsWith("R") || token.startsWith("end")) {
                    long transactionId = Long.parseLong(params[0].trim().substring(1));
                    Transaction transaction = this.transactionManager.getTransaction(transactionId);
                    if (token.startsWith("W")) {
                        int variableId = Integer.parseInt(params[1].trim().substring(1));
                        int value = Integer.parseInt(params[2].trim());
                        WriteOperation op = new WriteOperation(transaction, variableId, value, TimeManager.getTime());
                        transaction.getOperations().add(op);
                        boolean writeStatus = this.transactionManager.write(op);
                        if (!writeStatus) {
                            logger.info(String.format("W(T%d, x%d, %d) put on wait since site is down", transactionId, variableId, value));
                        }
                    } else if (token.startsWith("R")) {
                        int variableId = Integer.parseInt(params[1].trim().substring(1));
                        ReadOperation op = new ReadOperation(transaction, variableId, TimeManager.getTime());
                        transaction.getOperations().add(op);
                        Optional<Integer> val = this.transactionManager.read(op);
                        val.ifPresentOrElse(
                                integer -> logger.info(String.format("x%d: %d (T%d)", variableId, integer, transactionId)),
                                () -> logger.info(String.format("R(T%d, x%d) put on wait since site is down", transactionId, variableId))
                        );
                    } else if (token.startsWith("end")) {
                        CommitOperation op = new CommitOperation(transaction, TimeManager.getTime());
                        boolean status = this.transactionManager.commitTransaction(op);
                        logger.info("T"+transactionId+(status?" commits":" aborts"));
                    }
                } else if (token.equals("dump()")) {
                    this.transactionManager.printCommittedState();
                } else if (token.startsWith("fail")) {
                    int siteId = Integer.parseInt(params[0].trim());
                    this.transactionManager.fail(siteId);
                } else if (token.startsWith("recover")) {
                    int siteId = Integer.parseInt(params[0].trim());
                    this.transactionManager.recover(siteId);
                } else {
                    String errMsg = "Unknown token received " + token;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }

                TimeManager.incrementTime();
            }
        } catch (FileNotFoundException e) {
            logger.error("File not found: "+inputFilePath);
            System.exit(1);
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        logger.info("Simulation complete!");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Argument expected with path to input file");
            System.exit(1);
        }
        Simulation sim = new Simulation();
        sim.run(args[0]);
    }
}