package org.satran.aion.graphql.service;

import org.aion.api.IAionAPI;
import org.aion.api.type.*;
import org.aion.base.type.Hash256;
import org.satran.aion.graphql.exception.ConnectionException;
import org.satran.aion.graphql.pool.AionConnection;
import org.satran.aion.graphql.pool.AionRpcConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TxnService {

    private static final Logger logger = LoggerFactory.getLogger(TxnService.class);

    @Autowired
    private BlockService chainService;

    @Autowired
    private AionRpcConnectionHelper connectionHelper;

    public TxnService() {

    }

    public List<TxDetails> getTransactions(long fromBlock, long limit) {
        List<TxDetails> transactions = new ArrayList<TxDetails>();

        if(logger.isDebugEnabled())
         logger.debug("Getting transaction -----------");

        if(fromBlock == -1) {
            Block latestBlock = chainService.getLatestBlock();

            if(logger.isDebugEnabled())
                logger.debug("Return block " + latestBlock.getNumber());

            if(latestBlock != null)
                fromBlock = latestBlock.getNumber();

        }

        while (transactions.size() < limit) {
            BlockDetails blockDetails = chainService.getBlock(fromBlock);

            if (blockDetails == null)
                break;


            if (blockDetails.getTxDetails().size() > 0) {
                transactions.addAll(blockDetails.getTxDetails());
            }

            fromBlock--;
        }

        return transactions;

    }

    public Transaction getTransaction(Hash256 txHash) {
        if(logger.isDebugEnabled())
            logger.debug("Getting transaction for " + txHash);

        AionConnection connection = connectionHelper.getConnection();

        if(connection == null)
            throw new ConnectionException("Connection could not be established");

        IAionAPI api = connection.getApi();
        ApiMsg apiMsg = connection.getApiMsg();

        try {
            apiMsg.set(api.getChain().getTransactionByHash(txHash));
            if (apiMsg.isError()) {
                logger.error("Unable to get the transaction" + apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            if(logger.isDebugEnabled())
                logger.debug("Transaction details" + apiMsg.getObject());

            Transaction transaction = apiMsg.getObject();


//            if(blkDetails == null || blkDetails.size() == 0)
//                throw new RuntimeException("No block found with number : " + number);
//
//            BlockDetails block = blkDetails.get(0);
//
//            return block;

            return transaction;

        } finally {
            connectionHelper.closeConnection(connection);
        }
    }
}
