package org.satran.blockchain.graphql.impl.aion.util;

import com.google.gson.Gson;
import org.aion.api.type.*;
import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
import org.satran.blockchain.graphql.model.*;
import org.satran.blockchain.graphql.model.Block;
import org.satran.blockchain.graphql.model.TxDetails;
import org.satran.blockchain.graphql.model.input.TxArgsInput;
import org.springframework.beans.BeanUtils;

import java.util.stream.Collectors;

public class ModelConverter {

    public static Block convert(BlockDetails blockDetails) {
        Block b = new Block();

        BeanUtils.copyProperties(blockDetails, b);

        b.setBloom(String.valueOf(blockDetails.getBloom()));
        b.setExtraData(String.valueOf(blockDetails.getExtraData()));
        b.setSolution(String.valueOf(blockDetails.getSolution()));
        b.setHash(String.valueOf(blockDetails.getHash()));
        b.setParentHash(String.valueOf(blockDetails.getParentHash()));
        b.setMinerAddress(String.valueOf(blockDetails.getMinerAddress()));
        b.setStateRoot(String.valueOf(blockDetails.getStateRoot()));
        b.setTxTrieRoot(String.valueOf(blockDetails.getTxTrieRoot()));
        b.setSize(blockDetails.getSize());

        b.setTxDetails(blockDetails.getTxDetails().stream()
                .map(txDetails -> {
                   TxDetails txD =  ModelConverter.convert(txDetails);
                   txD.setBlockHash(b.getHash());
                   txD.setBlockNumber(b.getNumber());

                   return txD;
                }).collect(Collectors.toList()));

        return b;
    }

    public static Block convert(org.aion.api.type.Block aionBlock) {
        Block block = new Block();

        BeanUtils.copyProperties(aionBlock, block);

        block.setBloom(String.valueOf(aionBlock.getBloom()));
        block.setExtraData(String.valueOf(aionBlock.getExtraData()));
        block.setSolution(String.valueOf(aionBlock.getSolution()));

        block.setHash(String.valueOf(aionBlock.getHash()));
        block.setParentHash(String.valueOf(aionBlock.getParentHash()));
        block.setMinerAddress(String.valueOf(aionBlock.getMinerAddress()));
        block.setStateRoot(String.valueOf(aionBlock.getStateRoot()));
        block.setTxTrieRoot(String.valueOf(aionBlock.getTxTrieRoot()));
        block.setSize(aionBlock.getSize());

        return block;
    }

    public static TxDetails convert(org.aion.api.type.TxDetails aionTxDetails) {
        TxDetails tx = new TxDetails();

        BeanUtils.copyProperties(aionTxDetails, tx);

        tx.setTo(String.valueOf(aionTxDetails.getTo()));
        tx.setFrom(String.valueOf(aionTxDetails.getFrom()));
        tx.setContract(String.valueOf(aionTxDetails.getContract()));

        tx.setTxHash(String.valueOf(aionTxDetails.getTxHash()));
        tx.setData(String.valueOf(aionTxDetails.getData()));

        return  tx;
    }

    public static TxDetails convert(org.aion.api.type.Transaction aionTx) {
        TxDetails tx = new TxDetails();

        BeanUtils.copyProperties(aionTx, tx);

        tx.setTo(String.valueOf(aionTx.getTo()));
        tx.setFrom(String.valueOf(aionTx.getFrom()));
        tx.setTxHash(String.valueOf(aionTx.getTxHash()));
        tx.setData(String.valueOf(aionTx.getData()));

        tx.setBlockNumber(aionTx.getBlockNumber());
        tx.setBlockHash(String.valueOf(aionTx.getBlockHash()));

        return  tx;
    }

    public static ProtocolInfo convert(Protocol aionProtocol) {
        ProtocolInfo protocolInfo = new ProtocolInfo();

        BeanUtils.copyProperties(aionProtocol, protocolInfo);

        return protocolInfo;
    }

    public static MsgRespBean convert(MsgRsp aMsgResp) {
        MsgRespBean resp = new MsgRespBean();

        resp.setError(aMsgResp.getError());
        resp.setMsgHash(String.valueOf(aMsgResp.getMsgHash()));
        resp.setStatus(String.valueOf(aMsgResp.getStatus()));
        resp.setTxDeploy(String.valueOf(aMsgResp.getTxDeploy()));
        resp.setTxHash(String.valueOf(aMsgResp.getTxHash()));
        resp.setTxResult(String.valueOf(aMsgResp.getTxResult()));

        return resp;
    }

    public static CompileResponseBean convert(CompileResponse aionResponse) {

            Gson gson = new Gson();
            String resJson = gson.toJson(aionResponse);

            CompileResponseBean bean = gson.fromJson(resJson, CompileResponseBean.class);

            return bean;
    }

    public static DeployResponseBean convert(DeployResponse deployResponse) {
        DeployResponseBean drb = new DeployResponseBean();

        BeanUtils.copyProperties(deployResponse, drb);

        return drb;
    }

    public static TxArgs convert(TxArgsInput gqlInput) {
        TxArgs.TxArgsBuilder builder = new TxArgs.TxArgsBuilder();

        if(gqlInput.getData() != null)
            builder.data(ByteArrayWrapper.wrap(gqlInput.getData().getBytes()));

        builder.nrgPrice(gqlInput.getNrgPrice());
        builder.nrgLimit(gqlInput.getNrgLimit());
        builder.nonce(gqlInput.getNonce());
        builder.value(gqlInput.getValue());

        if(gqlInput.getFrom() != null)
            builder.from(Address.wrap(gqlInput.getFrom()));

        if(gqlInput.getTo() != null)
            builder.to(Address.wrap(gqlInput.getTo()));

        return builder.createTxArgs();
    }

    public static ContractEventFilter convert(ContractEventFilterBean gqlBean) {

        ContractEventFilter.ContractEventFilterBuilder builder = new ContractEventFilter.ContractEventFilterBuilder();

        builder.expireTime(gqlBean.getExpireTime());
        builder.fromBlock(gqlBean.getFromBlock());
        builder.toBlock(gqlBean.getToBlock());
        builder.topics(gqlBean.getTopics());

        if(gqlBean.getAddresses() != null) {
            builder.addresses(gqlBean.getAddresses()
                                    .stream()
                                    .map(as -> Address.wrap(as))
                                    .collect(Collectors.toList()));
        }

        return builder.createContractEventFilter();
    }

    public static TxReceiptBean convert(TxReceipt aionTxnRecipt) {
        TxReceiptBean bean = new TxReceiptBean();

        if(aionTxnRecipt == null)
            return null;

        bean.setBlockHash(aionTxnRecipt.getBlockHash().toString());
        bean.setBlockNumber(aionTxnRecipt.getBlockNumber());

        if(aionTxnRecipt.getContractAddress() !=  null)
            bean.setContractAddress(aionTxnRecipt.getContractAddress().toString());

        bean.setCumulativeNrgUsed(aionTxnRecipt.getCumulativeNrgUsed());
        bean.setNrgConsumed(aionTxnRecipt.getNrgConsumed());

        if(aionTxnRecipt.getFrom() != null)
            bean.setFrom(aionTxnRecipt.getFrom().toString());
        if(aionTxnRecipt.getTo() != null)
            bean.setTo(aionTxnRecipt.getTo().toString());

        bean.setTxHash(aionTxnRecipt.getTxHash().toString());
        bean.setTxIndex(aionTxnRecipt.getTxIndex());

        if(aionTxnRecipt.getTxLogs() != null) {
            bean.setTxLogs(aionTxnRecipt.getTxLogs()
                    .stream()
                    .map(tl -> {
                        TxReceiptBean.TxLogBean txLogBean = new TxReceiptBean.TxLogBean();

                        txLogBean.setAddress(tl.getAddress().toString());
                        txLogBean.setData(tl.getData().toString());
                        txLogBean.setTopic(txLogBean.getTopic());

                        return txLogBean;
                    })
                    .collect(Collectors.toList()));
        }

        return bean;
    }
}
