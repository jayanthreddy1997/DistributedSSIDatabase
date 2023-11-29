package com.nyu.db;
import com.nyu.db.datamanager.DataManager;
import com.nyu.db.datamanager.impl.DataManagerImpl;
import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;
import com.nyu.db.transactionmanager.TransactionManager;
import com.nyu.db.transactionmanager.impl.TransactionManagerImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
                dataManagers.get(siteId-1).registerVariable(variableId);
            } else {
                // Available at all sites
                for (DataManager dm: dataManagers) {
                    dm.registerVariable(variableId);
                }
            }
        }
        this.transactionManager = new TransactionManagerImpl(dataManagers);
    }

    public void run(String inputFilePath) {
        logger.info("Starting simulation with input from "+inputFilePath);
        long timeStamp = 0;
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
                    // TODO: Do I need to store active transactions, probably yes
                    this.transactionManager.createTransaction(Long.parseLong(transactionName.substring(1)), timeStamp);
                } else if (token.startsWith("W")) {
                    long transactionId = Long.parseLong(params[0].trim().substring(1));
                    int variableId = Integer.parseInt(params[1].trim().substring(1));
                    int value = Integer.parseInt(params[2].trim());
                    WriteOperation op = new WriteOperation(transactionId, variableId, value, timeStamp);
                    this.transactionManager.write(op);
                    // TODO: Print sites affected by write
                } else if (token.startsWith("R")) {
                    long transactionId = Long.parseLong(params[0].trim().substring(1));
                    int variableId = Integer.parseInt(params[1].trim().substring(1));
                    ReadOperation op = new ReadOperation(transactionId, variableId, timeStamp);
                    int val = this.transactionManager.read(op);
                    logger.info(String.format("x%d: %d (T%d)", variableId, val, transactionId));
                } else if (token.startsWith("end")) {
                    long transactionId = Long.parseLong(params[0].trim().substring(1));
                    CommitOperation op = new CommitOperation(transactionId, timeStamp);
                    boolean status = this.transactionManager.commitTransaction(op);
                    logger.info("T"+transactionId+(status?" commits":" aborts"));
                } else if (token.equals("dump()")) {
                    this.transactionManager.dumpVariableValues();
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

                timeStamp += 1;
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