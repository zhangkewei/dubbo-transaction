package me.zkevin.transaction.support;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;

/**
 * 事务手动控制类，不支持嵌套事务
 */
public class TransactionSupport {
    public static boolean startWithTransaction(){
        TransactionId transactionId=TransactionId.get();
        return transactionId.enableProxy();
    }
    public static boolean endWithTransaction(){
        TransactionId transactionId=TransactionId.get();
        try {
            TransactionController.commit();
        } catch (RpcTransactionRollbackException e) {
            TransactionController.rollback();
        }
        return transactionId.revertProxy();
    }
    public static boolean rollbackWithTransaction(){
        TransactionId transactionId=TransactionId.get();
        TransactionController.rollback();
        return transactionId.revertProxy();
    }
    public static boolean startNoTransaction(){
        TransactionId transactionId=TransactionId.get();
        return transactionId.disableProxy();
    }
    public static boolean endNoTransaction(){
        TransactionId transactionId=TransactionId.get();
        try {
            TransactionController.commit();
        } catch (RpcTransactionRollbackException e) {
            e.printStackTrace();
        }
        return transactionId.revertProxy();
    }

    /**
     * 重置会话
     */
    public static void resetSession(){
        SessionHook.clearSession();
    }
    public static boolean isProxy(){
        TransactionId transactionId=TransactionId.get();
        return transactionId.isProxy();
    }
}
