package me.zkevin.transaction.support;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
/**
 * 事务在控制器层的Hook默认实现
 * Created by zkevin on 16/12/6.
 */
public class DefaultSessionHook extends SessionHook {
    public void doPreHook() {

    }

    public void doPostHook() throws RpcTransactionRollbackException {
        TransactionController.commit();
    }

    public void doAfterHook() {
    }

    public void doExceptionHook(Throwable trigger,String  rollbackFor){
        try {
            Class rollback=Class.forName(rollbackFor);
            if(rollback.isAssignableFrom(trigger.getClass())){
                TransactionController.rollback();
            }else {
                TransactionController.commit();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            TransactionController.rollback();
        } catch (RpcTransactionRollbackException e) {
            e.printStackTrace();
            //rollback transaction alread
        }
    }

}
