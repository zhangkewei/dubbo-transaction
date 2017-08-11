package me.zkevin.transaction.context;

import com.alibaba.dubbo.rpc.proxy.invoker.InvokerChainContextFactory;
import me.zkevin.transaction.support.TransactionId;
import org.apache.log4j.Logger;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by zkevin on 16/11/8.
 */
public    class DBTransactionContext  implements TransactionContext {
    protected static Logger logger= Logger.getLogger(DBTransactionContext.class);
    private ReadWriteLock STATUS_LOCK=new ReentrantReadWriteLock();
    private Map<String,List<Transaction>> _TRANSACTION_HASH=new ConcurrentHashMap<String,List<Transaction>>();
    //记录事务开始时间，用于释放占用的资源
    private Map<String,Long> _TRANSACTION_HOLD_TIMES=new ConcurrentHashMap<String,Long>();
    public  List<Transaction> getTransactions(){
        return getTransactions(TransactionId.getTransactionKey(null));
    }

    public List<Transaction> getTransactions(String transactionId) {
        List<Transaction> subTransaction=_TRANSACTION_HASH.containsKey(transactionId)?_TRANSACTION_HASH.get(transactionId):new ArrayList<Transaction>();
        return Collections.unmodifiableList(subTransaction);
    }

    public void release(long seconds) {
        for(Map.Entry<String,Long> entry:_TRANSACTION_HOLD_TIMES.entrySet()){
            if((entry.getValue()+seconds*1000)<System.currentTimeMillis()){
                List<Transaction> transactions=_TRANSACTION_HASH.get(entry.getKey());
                if(null!=transactions&&!transactions.isEmpty()){
                    for(Transaction transaction:transactions){
                        try {
                            logger.info("release DBTransaction and invoke chain:"+transaction.getTransactionId().getTransactionKey());
                            transaction.rollback();
                            InvokerChainContextFactory.getInstance().remove(transaction.getTransactionId().getTransactionKey());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
                clear(entry.getKey());
            }
        }
    }

    public  Transaction persistent(Object status){
        TransactionId tID=TransactionId.get();
        String transactionKey=tID.getTransactionKey();
        //将事务状态放入集合
        List<Transaction> transactions=_TRANSACTION_HASH.containsKey(transactionKey)?_TRANSACTION_HASH.get(transactionKey):null;
        if(null==transactions) {
            STATUS_LOCK.writeLock().lock();
            transactions = null == transactions && _TRANSACTION_HASH.containsKey(transactionKey) ? _TRANSACTION_HASH.get(transactionKey) : transactions;
            if (null == transactions) {
                transactions = new CopyOnWriteArrayList<Transaction>();
                _TRANSACTION_HASH.put(transactionKey,transactions);
                _TRANSACTION_HOLD_TIMES.put(transactionKey,System.currentTimeMillis());
            }
            STATUS_LOCK.writeLock().unlock();
        }
        Transaction t=DBTransaction.createDbTransaction((DefaultTransactionStatus) status);
        transactions.add(t);
        return t;
    }
    public void clear() {
        TransactionId tID=TransactionId.get();
        String transactionKey=tID.getTransactionKey();
        clear(transactionKey);
    }
    public void clear(String transactionKey) {
        _TRANSACTION_HASH.remove(transactionKey);
    }
}
