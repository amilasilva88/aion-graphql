package org.satran.blockchain.graphql.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import org.aion.api.type.Transaction;
import org.aion.base.type.Hash256;
import org.satran.blockchain.graphql.model.Account;
import org.satran.blockchain.graphql.model.Block;
import org.satran.blockchain.graphql.model.TxDetails;
import org.satran.blockchain.graphql.exception.DataFetchingException;
import org.satran.blockchain.graphql.service.AccountService;
import org.satran.blockchain.graphql.service.BlockService;
import org.satran.blockchain.graphql.service.TxnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Query implements GraphQLQueryResolver {

    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    @Autowired
    private BlockService blockService;

    @Autowired
    private TxnService txnService;

    @Autowired
    private AccountService accountService;


    public List<Block> blocks(long first, long offset) {
        try {
            return blockService.getBlocks(first, offset);
        } catch (Exception e) {
            logger.error("Error getting blocks", e);
            throw new DataFetchingException(e.getMessage());
        }

    }

    public Block block(long number) {
        try {
            return blockService.getBlock(number);
        } catch(Exception e) {
            logger.error("Error getting block ", e);
            throw new DataFetchingException(e.getMessage());
        }
    }

    public Transaction transaction(Hash256 txHash) {
        try {
            return txnService.getTransaction(txHash);
        } catch (Exception e) {
            logger.error("Error getting transaction", e);
            throw new DataFetchingException(e.getMessage());
        }
    }

    public List<TxDetails> transactions(long fromBlock, long limit) {
        try {
            return txnService.getTransactions(fromBlock, limit);
        } catch (Exception e) {
            logger.error("Error getting transactions", e);
            throw new DataFetchingException(e.getMessage());
        }
    }

    public Account account(String publicKey, long blockNumber) {
        try {
            List<String> fields = new ArrayList<>();
            fields.add("balance");

            return accountService.getAccount(publicKey, fields, blockNumber);
        } catch (Exception e) {
            logger.error("Error getting transaction", e);
            throw new DataFetchingException(e.getMessage());
        }
    }

}