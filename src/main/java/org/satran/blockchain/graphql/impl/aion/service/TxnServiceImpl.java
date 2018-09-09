package org.satran.blockchain.graphql.impl.aion.service;

import org.aion.api.type.*;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.satran.blockchain.graphql.impl.aion.service.dao.AionBlockchainAccessor;
import org.satran.blockchain.graphql.impl.aion.util.ModelConverter;
import org.satran.blockchain.graphql.impl.aion.util.TypeUtil;
import org.satran.blockchain.graphql.model.*;
import org.satran.blockchain.graphql.model.Block;
import org.satran.blockchain.graphql.model.TxDetails;
import org.satran.blockchain.graphql.model.input.TxArgsInput;
import org.satran.blockchain.graphql.service.BlockService;
import org.satran.blockchain.graphql.service.TxnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TxnServiceImpl implements TxnService {

    private static final Logger logger = LoggerFactory.getLogger(TxnServiceImpl.class);

    private BlockService chainService;

    private AionBlockchainAccessor accessor;

    public TxnServiceImpl(BlockService blockService, AionBlockchainAccessor accessor) {
        this.chainService = blockService;
        this.accessor = accessor;
    }

    public List<TxDetails> getTransactions(long fromBlock, long limit) {
        List<TxDetails> transactions = new ArrayList<TxDetails>();

        if (logger.isDebugEnabled())
            logger.debug("Getting transaction -----------");

        if (fromBlock == -1) {
            Block latestBlock = chainService.getLatestBlock();

            if (logger.isDebugEnabled())
                logger.debug("Return block " + latestBlock.getNumber());

            if (latestBlock != null)
                fromBlock = latestBlock.getNumber();

        }

        while (transactions.size() < limit && fromBlock >= 0) {
            Block blockDetails = chainService.getBlock(fromBlock);

            if (blockDetails == null)
                break;


            if (blockDetails.getTxDetails().size() > 0) {
                transactions.addAll(blockDetails.getTxDetails());
            }

            fromBlock--;
        }

        return transactions;

    }

    public TxDetails getTransaction(String txHash) {
        if (logger.isDebugEnabled())
            logger.debug("Getting transaction for {} ", txHash);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getChain().getTransactionByHash(Hash256.wrap(txHash)));
            if (apiMsg.isError()) {
                logger.error("Unable to get the transaction {}", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            if (logger.isDebugEnabled())
                logger.debug("Transaction details" + apiMsg.getObject());

            Transaction transaction = apiMsg.getObject();

            return ModelConverter.convert(transaction);
//            if(blkDetails == null || blkDetails.size() == 0)
//                throw new RuntimeException("No block found with number : " + number);
//
//            BlockDetails block = blkDetails.get(0);
//
//            return block;


        }));

    }

    @Override
    public String call(TxArgsInput args) { //TODO test
        if (logger.isDebugEnabled())
            logger.debug("Invoking call {} ", args);

        return accessor.call(((apiMsg, api) -> {

            TxArgs txArgs = new TxArgs.TxArgsBuilder()
                                .from(Address.wrap(args.getFrom()))
                                .to(Address.wrap(args.getTo()))
                                .value(args.getValue())
                                .nonce(args.getNonce())
                                .data(ByteArrayWrapper.wrap(args.getData().getBytes()))
                                .nrgLimit(args.getNrgLimit())
                                .nrgPrice(args.getNrgPrice())
                                .createTxArgs();

            apiMsg.set(api.getTx().call(txArgs));

            if (apiMsg.isError()) {
                logger.error("Unable to invoke call", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            byte[] bytes = apiMsg.getObject();

            return new String(bytes);
        }));
    }

    public Map<String, CompileResponseBean> compile(String code) {
        if(logger.isDebugEnabled())
            logger.debug("Trying to compile code");

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().compile(code));

            if(apiMsg.isError()) {
                logger.error("Error compiling contract source code : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            Map<String, CompileResponse> response = apiMsg.getObject();

            if(response == null)
                throw new RuntimeException("Error during compilation");

            Map<String, CompileResponseBean> result = new HashMap<>();

            for(Map.Entry<String, CompileResponse> entry: response.entrySet()) {
                result.put(entry.getKey(), ModelConverter.convert(entry.getValue()));
            }

            return result;

        }));

    }

    @Override
    public List<DeployResponseBean> contractDeploy(ContractDeployBean cd) {

        if(logger.isDebugEnabled())
            logger.debug("Invoke contractDeploy()");

        return accessor.call(((apiMsg, api) -> {

            if(logger.isDebugEnabled())
                logger.debug("Let's first compile the contract");

            apiMsg.set(api.getTx().compile(cd.getCode()));

            if(apiMsg.isError()) {
                logger.error("Error compiling contract source code : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            Map<String, CompileResponse> compileResponses = apiMsg.getObject();

            List<DeployResponseBean> result = new ArrayList();

            if(logger.isDebugEnabled())
                logger.debug("Compilation done");

            int counter = 0;
            for(CompileResponse compRes: compileResponses.values()) {

                if(logger.isDebugEnabled())
                    logger.debug("Deploying contract #{}", counter++);

                ContractDeploy.ContractDeployBuilder contractDeployBuilder
                        = new ContractDeploy.ContractDeployBuilder()
                        .compileResponse(compRes)
                        .from(Address.wrap(cd.getFrom()))
                        .nrgLimit(cd.getNrgLimit())
                        .nrgPrice(cd.getNrgPrice())
                        .value(cd.getValue())
                        .constructor(cd.isConstructor());

                if (cd.getData() != null)
                    contractDeployBuilder.data(ByteArrayWrapper.wrap(cd.getData().getBytes()));

                ContractDeploy aionCd = contractDeployBuilder.createContractDeploy();

                apiMsg.set(api.getTx().contractDeploy(aionCd));

                if (apiMsg.isError()) {
                    logger.error("Error deploying contract : {} ", apiMsg.getErrString());
                    throw new RuntimeException(apiMsg.getErrString());
                }

                DeployResponse deployResponse = apiMsg.getObject();
                result.add(ModelConverter.convert(deployResponse));
            }
            return result;

        }));

    }

    @Override
    public long estimateNrg(String code) {
        if (logger.isDebugEnabled())
            logger.debug("Estimate Nrg {} ", code);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().estimateNrg(code));

            if(apiMsg.isError()) {
                logger.error("Error estimating Nrg : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            return apiMsg.getObject();

        }));
    }

    @Override
    public long estimateNrg(TxArgsInput argsInput) {
        if (logger.isDebugEnabled())
            logger.debug("Estimate Nrg {} ", argsInput);

        TxArgs txArgs = ModelConverter.convert(argsInput);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().estimateNrg(txArgs));

            if(apiMsg.isError()) {
                logger.error("Error estimating Nrg : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            return apiMsg.getObject();

        }));
    }

    @Override
    public boolean eventDeregister(List<String> evts, String address) {

        if (logger.isDebugEnabled())
            logger.debug("Deregister event ");

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().eventDeregister(evts, Address.wrap(address)));

            if (apiMsg.isError()) {
                logger.error("Error de-register : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            if (logger.isDebugEnabled())
                logger.debug("eventDeregistered successfully");

            return apiMsg.getObject();
        }));
    }

    @Override
    public boolean eventRegister(List<String> evts, ContractEventFilterBean ef, String address) {
        if (logger.isDebugEnabled())
            logger.debug("event register event ");

        ContractEventFilter cef = ModelConverter.convert(ef);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().eventRegister(evts, cef, Address.wrap(address)));

            if (apiMsg.isError()) {
                logger.error("Error event register : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            if (logger.isDebugEnabled())
                logger.debug("event registered successfully");

            return apiMsg.getObject();
        }));
    }

    @Override
    public boolean fastTxBuild(TxArgsInput args, boolean call) { //TODO


        throw new UnsupportedOperationException("Not supported yet");
        /*if (logger.isDebugEnabled())
            logger.debug("Fast tx build ");

        TxArgs txArgs = ModelConverter.convert(args);

        return accessor.call(((apiMsg, api) -> {
            //apiMsg.set(api.getTx().fastTxbuild(txArgs, call));

            if (apiMsg.isError()) {
                logger.error("Error calling api.getTx().fastTxbuild : {} " + apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            return apiMsg.getObject();
        }));*/
    }

    @Override
    public String getCode(String address) {
        if (logger.isDebugEnabled())
            logger.debug("Getting code at address {} ", address);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getCode(Address.wrap(address)));

            if (apiMsg.isError()) {
                logger.error("Unable to get code : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            byte[] bytes = apiMsg.getObject();

            return TypeUtil.toString(bytes);
        }));
    }

    @Override
    public String getCode(String address, long blockNumber) {
        if (logger.isDebugEnabled())
            logger.debug("Getting code at address {} and blocknumber {} ", address, blockNumber);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getCode(Address.wrap(address), blockNumber));

            if (apiMsg.isError()) {
                logger.error("Unable to get code : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            byte[] bytes = apiMsg.getObject();

            return TypeUtil.toString(bytes);
        }));
    }

    @Override
    public MsgRespBean getMsgStatus(String msgHash) {
        if (logger.isDebugEnabled())
            logger.debug("Getting message status {} ", msgHash);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getMsgStatus(TypeUtil.toByteArrayWrapper(msgHash)));

            if (apiMsg.isError()) {
                logger.error("Unable to get Msg status : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            MsgRsp msgResp = apiMsg.getObject();

            return ModelConverter.convert(msgResp);
        }));
    }

    @Override
    public long getNrgPrice() {
        if (logger.isDebugEnabled())
            logger.debug("Getting Nrg price ");

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getNrgPrice());

            if (apiMsg.isError()) {
                logger.error("Unable to get Nrg price : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            return apiMsg.getObject();
        }));
    }

    @Override
    public String getSolcVersion() {
        if (logger.isDebugEnabled())
            logger.debug("Getting solc version ");

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getSolcVersion());

            if (apiMsg.isError()) {
                logger.error("Unable to solc version : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            return apiMsg.getObject();
        }));
    }

    @Override
    public TxReceiptBean getTxReceipt(String txnHash) {
        if (logger.isDebugEnabled())
            logger.debug("Getting txn receipt {} ", txnHash);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().getTxReceipt(Hash256.wrap(txnHash)));

            if (apiMsg.isError()) {
                logger.error("Unable to get txn receipt : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            TxReceipt txReceipt = apiMsg.getObject();

            return ModelConverter.convert(txReceipt);
        }));
    }

    @Override
    public MsgRespBean sendRawTransaction(String encodedTx) {
        if (logger.isDebugEnabled())
            logger.debug("Sending raw transaction {} ", encodedTx);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().sendRawTransaction(TypeUtil.toByteArrayWrapper(encodedTx)));

            if (apiMsg.isError()) {
                logger.error("Unable to send raw transaction : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            MsgRsp msgRsp = apiMsg.getObject();

            return ModelConverter.convert(msgRsp);
        }));
    }

    @Override
    public MsgRespBean sendSignedTransaction(TxArgsInput txArgsInput, String privateKey) {
        if (logger.isDebugEnabled())
            logger.debug("SendSinged transaction ");

        TxArgs txArgs = ModelConverter.convert(txArgsInput);

        return accessor.call(((apiMsg, api) -> {
            apiMsg.set(api.getTx().sendSignedTransaction(txArgs, TypeUtil.toByteArrayWrapper(privateKey)));

            if (apiMsg.isError()) {
                logger.error("Unable to send signed transaction request : {} ", apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

            MsgRsp msgRsp = apiMsg.getObject();

            return ModelConverter.convert(msgRsp);
        }));
    }

    @Override
    public MsgRespBean sendTransaction(TxArgsInput txArgsInput) {
        if (logger.isDebugEnabled())
            logger.debug("Sending transaction : {} ", txArgsInput);

        return accessor.call(((apiMsg, api) -> {

            TxArgs txArgs = ModelConverter.convert(txArgsInput);

            apiMsg.set(api.getTx().nonBlock().sendTransaction(txArgs));

            if(apiMsg.isError()) {
                logger.error("Error posting transaction : {} " + apiMsg.getErrString());
                throw new RuntimeException(apiMsg.getErrString());
            }

//
            MsgRsp msgRsp = apiMsg.getObject();

            if(logger.isDebugEnabled())
                logger.debug("Posted transaction hash : {}", msgRsp.getTxHash());

            return ModelConverter.convert(msgRsp);
        }));
    }


}
