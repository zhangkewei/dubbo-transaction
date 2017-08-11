package me.zkevin.transaction.context;

import java.util.List;

/**
 * 事务bean容器
 */
public  interface TransactionContext {
    public  Transaction persistent(Object transactionStatus);
    public  void clear();
    public  void clear(String transactionKey);
    public  List<Transaction> getTransactions();
    public  List<Transaction> getTransactions(String transactionId);
    public  void release(long seconds);
}
