package me.zkevin.transaction.support;

import me.zkevin.transaction.context.Transaction;
import me.zkevin.transaction.context.TransactionContextFactory;
import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
import me.zkevin.transaction.support.notice.netty.command.*;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
public   class TransactionProtocolImpl implements TransactionProtocol{
    private static Logger logger= Logger.getLogger(TransactionProtocolImpl.class);
    private TransactionContextFactory factory;
    public TransactionProtocolImpl(TransactionContextFactory factory){
        this.factory=factory;
    }
    public CommandResult commit(Command command) throws RpcTransactionRollbackException {
        if(!command.isInvoker())TransactionController.commit(command);
        List<Transaction> transactions=factory.getTransactions(command.getTransactionId());
        logger.debug("[dbCommit]["+command.getTransactionId()+"]"+transactions.size());
        if(null!=transactions&&!transactions.isEmpty()){
            for(Transaction t:transactions){
                try {
                    logger.info("["+t.getTransactionId().getCurrentId()+"]commit transaction");
                    t.commit();
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }
        factory.clear(command.getTransactionId());
        return CommandResult.success();
    }
    public  CommandResult prepare(Command command){
        if(command.isInvoker()||TransactionController.prepare(command)) {
            List<Transaction> transactions = factory.getTransactions(command.getTransactionId());
            if (null != transactions && !transactions.isEmpty()) {
                for (Transaction t : transactions) {
                    if (!t.prepare()) return CommandResult.error(null);
                }
            }
        }else{
            return CommandResult.error(null);
        }
        return CommandResult.success();
    }
    public CommandResult rollback(Command command){
        if(!command.isInvoker())TransactionController.rollback(command);
        List<Transaction> transactions = factory.getTransactions(command.getTransactionId());
        if (null != transactions && !transactions.isEmpty()) {
            for (Transaction t : transactions) {
                try {
                    t.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        factory.clear(command.getTransactionId());
        return CommandResult.success();
    }

    public CommandResult execute(Command command) throws RpcTransactionRollbackException {
        if(command instanceof CommitCommand){
            return commit(command);
        }
        if(command instanceof RollbackCommand){
            return rollback(command);
        }
        if(command instanceof PrepareCommand){
            return prepare(command);
        }
        return null;
    }

    public void release(long seconds) {
        factory.release(seconds);
    }

    public void setFactory(TransactionContextFactory factory) {
        this.factory = factory;
    }
}
