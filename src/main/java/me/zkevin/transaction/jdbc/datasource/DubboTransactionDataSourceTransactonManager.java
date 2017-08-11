package me.zkevin.transaction.jdbc.datasource;

import me.zkevin.transaction.context.DBTransaction;
import me.zkevin.transaction.context.Transaction;
import me.zkevin.transaction.context.TransactionContextFactory;
import me.zkevin.transaction.support.TransactionId;
import me.zkevin.transaction.support.TransactionStartup;
import me.zkevin.transaction.support.TransactionSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.sql.Connection;

/**
 *
 * Created by zkevin on 16/11/8.
 */
public class DubboTransactionDataSourceTransactonManager extends DataSourceTransactionManager{
    public void transactionStart(){
        logger.info("chit-transaction starting........");
        TransactionStartup.start();
        logger.info("chit-transaction started.........");
    }
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction,definition);
    }
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        // Remove the connection holder from the thread, if exposed.
        if (checkNewConnectionHolder(transaction)&&null!= TransactionSynchronizationManager.getResource(getDataSource())) {
            TransactionSynchronizationManager.unbindResource(getDataSource());
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        super.doRollback(status);
        super.doCleanupAfterCompletion(status.getTransaction());
    }
    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        String prefix="";
        if(logger.isDebugEnabled()) {
            prefix += "[" + TransactionId.get().getCurrentId() + "]";
            try {
                Connection connection = ((JdbcTransactionObjectSupport)status.getTransaction()).getConnectionHolder().getConnection();
                prefix += "[" + (null != connection ? connection : "") + "]";
            } catch (Exception e) {}
        }
        logger.debug(prefix+"DataSource doCommit");
        //将status持久化到jvm内存
        if(null!=TransactionContextFactory.getInstance()&& TransactionSupport.isProxy()){
            logger.debug(prefix+"DataSource doCommit before store");
            Transaction transaction=TransactionContextFactory.getInstance().persistent(status);
            if(transaction instanceof DBTransaction){
                logger.debug(prefix+"DataSource doCommit in store");
                DBTransaction dbT=(DBTransaction)transaction;
                dbT.setDataSource(getDataSource());
                dbT.setIsNewConnectionHolder(checkNewConnectionHolder(status.getTransaction()));
            }
        }else{//如果事务容器工厂类初始化失败，执行默认逻辑
            logger.debug(prefix+"DataSource doCommit don't store");
            super.doCommit(status);
            super.doCleanupAfterCompletion(status.getTransaction());
        }
    }
    private boolean checkNewConnectionHolder(Object transaction){
        Method method=ReflectionUtils.findMethod(transaction.getClass(), "isNewConnectionHolder");
        method.setAccessible(true);
        Object invokeResult=null!=method?ReflectionUtils.invokeMethod(method,transaction):null;
        return null!=invokeResult&&invokeResult instanceof Boolean&&(Boolean)invokeResult;
    }
}
