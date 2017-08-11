package me.zkevin.transaction.context;


import me.zkevin.transaction.support.TransactionId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.SQLException;

/**
 * 分布式事务抽象类
 * Created by zkevin on 16/11/16.
 */
public abstract class Transaction {
    protected transient Log logger = LogFactory.getLog(getClass());
    private TransactionId transactionId;
    public abstract boolean commit() throws SQLException;
    public abstract boolean rollback() throws SQLException;
    public abstract boolean prepare();
    public TransactionId getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(TransactionId transactionId) {
        this.transactionId = transactionId;
    }
}
