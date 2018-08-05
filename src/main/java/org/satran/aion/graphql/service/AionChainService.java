package org.satran.aion.graphql.service;

import org.aion.api.IAionAPI;
import org.aion.api.type.ApiMsg;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.Transaction;
import org.aion.base.type.Hash256;
import org.satran.aion.graphql.exception.ConnectionException;
import org.satran.aion.graphql.pool.AionConnection;
import org.satran.aion.graphql.pool.AionRpcConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AionChainService {

    private static final Logger logger = LoggerFactory.getLogger(AionChainService.class);

    @Autowired
    private AionRpcConnectionHelper connectionHelper;

    public AionChainService() {

    }

    public  List<BlockDetails> getBlocks(Long first, long offset) {

        AionConnection connection = connectionHelper.getConnection();

        if(first >= 30)
            throw new RuntimeException("Too many blocks. You can only request upto 30 blocks in a call");

        if(connection == null)
            throw new ConnectionException("Connection could not be established");

        IAionAPI api = connection.getApi();
        ApiMsg apiMsg = connection.getApiMsg();

        //Find the latest block number if the blocknumber is not passed
        if(offset == -1) {
            apiMsg.set(api.getChain().blockNumber());
            if (apiMsg.isError()) {
                logger.error("Get blockNumber isError: " + apiMsg.getErrString());
            }

            long bn = api.getChain().blockNumber().getObject();
        }


        try {

            if(offset == -1)
                apiMsg.set(api.getAdmin().getBlockDetailsByLatest(first));
            else {
                apiMsg.set(api.getAdmin().getBlockDetailsByRange(offset - first, offset));
            }

            if (apiMsg.isError()) {
                logger.error("Error getting block details" + apiMsg.getErrString());
            }

            List<BlockDetails> block = (List<BlockDetails>) apiMsg.getObject();

            if(logger.isDebugEnabled())
                logger.debug("Result: " + block.toString());

            return block;
        } finally {
           connectionHelper.closeConnection(connection);
        }


    }

    public BlockDetails getBlock(long number) {

        if(logger.isDebugEnabled())
            logger.debug("Getting block for " + number);

        AionConnection connection = connectionHelper.getConnection();

        if(connection == null)
            throw new ConnectionException("Connection could not be established");

        IAionAPI api = connection.getApi();
        ApiMsg apiMsg = connection.getApiMsg();

        try {
            apiMsg.set(api.getAdmin().getBlockDetailsByNumber(String.valueOf(number)));
            if (apiMsg.isError()) {
                logger.error("Unable to get the block" + apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            if(logger.isDebugEnabled())
                logger.debug("Block details got" + apiMsg.getObject().getClass());

            List<BlockDetails> blkDetails = ((List<BlockDetails>)apiMsg.getObject());

            if(blkDetails == null || blkDetails.size() == 0)
                throw new RuntimeException("No block found with number : " + number);

            BlockDetails block = blkDetails.get(0);

            return block;

        } finally {
            connectionHelper.closeConnection(connection);
        }

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
