package me.zkevin.transaction.context;

import me.zkevin.transaction.support.TransactionConfig;
import me.zkevin.transaction.support.TransactionId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zkevin on 16/11/8.
 */
public class TransactionContextFactory implements TransactionContext  {
    protected transient Log logger = LogFactory.getLog(getClass());
    private static  volatile  TransactionContextFactory factory=new TransactionContextFactory();
    //常量
    private int tcCount=1;
    //jdk保证内存可见性
    private AtomicBoolean state;
    private TransactionContextFactory(){
        state=new AtomicBoolean(false);
    }
    public static TransactionContextFactory getInstance(){
        return factory;
    }
    //jdk保证内存可见性
    private List<TransactionContext> contexts=new java.util.concurrent.CopyOnWriteArrayList<TransactionContext>();
    public  TransactionContextFactory  start(){
        if(contexts.isEmpty() && state.compareAndSet(false,true)){
            Class tcClass = TransactionConfig.getInstance().contextClass2Class();
            tcCount = TransactionConfig.getInstance().contextCount2Int();
            Assert.isTrue(null != tcClass && TransactionContext.class.isAssignableFrom(tcClass), "tcClass is null or not assignable from TransactionContext");
            for (int i = 0; i < tcCount; i++) {
                TransactionContext tc = (TransactionContext) BeanUtils.instantiateClass(tcClass);
                contexts.add(tc);
            }
        }
        return this;
    }
    public Transaction persistent(Object transactionStatus) {
        return getTransactionContext().persistent(transactionStatus);
    }

    public void clear() {
        getTransactionContext().clear();
    }

    public void clear(String transactionKey) {
        getTransactionContext(transactionKey).clear(transactionKey);
    }

    public List<Transaction> getTransactions() {
        return getTransactionContext().getTransactions();
    }

    public List<Transaction> getTransactions(String transactionId) {
        return  getTransactionContext(transactionId).getTransactions(transactionId);
    }

    public void release(long seconds) {
        if(null!=contexts&&!contexts.isEmpty()){
            for(TransactionContext tc:contexts){
                tc.release(seconds);
            }
        }
    }

    /**
     * hash取模得出当前
     * @return
     */
    private  int getHashKey(String transactionId){
        if(null==transactionId||transactionId.trim().isEmpty()) {
            transactionId= TransactionId.getTransactionKey(null);
        }
        int hashCode=transactionId.hashCode();
        int hashKey=hashCode%tcCount;
        return hashKey>0&&hashKey<tcCount?hashKey:0;
    }
    private TransactionContext getTransactionContext() {
        return contexts.get(getHashKey(null));
    }
    private TransactionContext getTransactionContext(String transactionId) {
        return contexts.get(getHashKey(transactionId));
    }
}
