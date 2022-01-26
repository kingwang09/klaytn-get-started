package com.example.klaytn.basic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klaytn.caver.Caver;
import com.klaytn.caver.methods.response.Bytes32;
import com.klaytn.caver.methods.response.TransactionReceipt;
import com.klaytn.caver.transaction.TxPropertyBuilder;
import com.klaytn.caver.transaction.response.PollingTransactionReceiptProcessor;
import com.klaytn.caver.transaction.response.TransactionReceiptProcessor;
import com.klaytn.caver.transaction.type.ValueTransfer;
import com.klaytn.caver.wallet.keyring.AbstractKeyring;
import com.klaytn.caver.wallet.keyring.KeyStore;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class KlaytnService {

    private final String BAOBAB_URL = "https://api.baobab.klaytn.net:8651";

    public KlaytnService() {
    }

    public void sendKlay(String keystorePath, String password) throws IOException, CipherException, TransactionException {
        Caver caver = new Caver(BAOBAB_URL);
        //Caver caver = new Caver("https://public-node-api.klaytnapi.com/v1/baobab");

        //keystore json 파일을 읽음.
        File file = new File(keystorePath);

        // keystore 복호화.
        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        KeyStore keyStore = objectMapper.readValue(file, KeyStore.class);
        AbstractKeyring keyring = caver.wallet.keyring.decrypt(keyStore, password);

        // caver wallet에 추가
        caver.wallet.add(keyring);

        BigInteger value = new BigInteger(caver.utils.convertToPeb(BigDecimal.ONE, "KLAY"));

        // 자산 이전 트랜잭션 생성
        ValueTransfer valueTransfer = caver.transaction.valueTransfer.create(
                TxPropertyBuilder.valueTransfer()
                        .setFrom(keyring.getAddress())
                        .setTo("0x8084fed6b1847448c24692470fc3b2ed87f9eb47")
                        .setValue(value)
                        .setGas(BigInteger.valueOf(25000))
        );

        // 트랜잭션 서명
        valueTransfer.sign(keyring);

        // Klaytn으로 트랜잭션 전송
        Bytes32 result = caver.rpc.klay.sendRawTransaction(valueTransfer.getRawTransaction()).send();
        if(result.hasError()) {
            throw new RuntimeException(result.getError().getMessage());
        }

        // 트랜잭션 영수증 확인
        TransactionReceiptProcessor transactionReceiptProcessor = new PollingTransactionReceiptProcessor(caver, 1000, 15);
        TransactionReceipt.TransactionReceiptData transactionReceipt = transactionReceiptProcessor.waitForTransactionReceipt(result.getResult());
        System.out.println("receipt hash : "+ transactionReceipt.getBlockHash());
    }
}
