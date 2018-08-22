package org.satran.aion.graphql.resolvers;

import java.util.List;
import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import graphql.GraphQLException;
import graphql.servlet.GraphQLErrorHandler;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.Transaction;
import org.aion.api.type.TxDetails;
import org.aion.base.type.Hash256;
import org.satran.aion.graphql.exception.DataFetchingException;
import org.satran.aion.graphql.service.BlockService;
import org.satran.aion.graphql.service.TxnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Query implements GraphQLQueryResolver {

    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    @Autowired
    private BlockService aionService;

    @Autowired
    private TxnService txnService;


    public List<BlockDetails> blocks(long first, long offset) {
        try {
            return aionService.getBlocks(first, offset);
        } catch (Exception e) {
            logger.error("Error getting blocks --", e);
            throw new DataFetchingException(e.getMessage());
        }

    }

    public BlockDetails block(long number) {
        try {
            return aionService.getBlock(number);
        } catch(Exception e) {
            logger.error("Error getting block ", e);
            throw e;
        }
    }

    public Transaction transaction(Hash256 txHash) {
        try {
            return txnService.getTransaction(txHash);
        } catch (Exception e) {
            logger.error("Error getting transaction", e);
            throw e;
        }
    }

    public List<TxDetails> transactions(long fromBlock, long limit) {
        try {
            return txnService.getTransactions(fromBlock, limit);
        } catch (Exception e) {
            logger.error("Error getting transactions", e);
            throw e;
        }
    }
}