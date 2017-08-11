package me.zkevin.transaction.support;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;

/**
 * 会话事务Hook
 * Created by zkevin on 16/12/6.
 */
public abstract class SessionHook{
    public void preHook(){
        doPreHook();
    }
    public void postHook() throws RpcTransactionRollbackException {
        doPostHook();
        clearSession();
    }
    public void afterHook(){
        doAfterHook();
        clearSession();
    }
    public void exceptionHook(Throwable e,String  rollbackFor){
        doExceptionHook(e, rollbackFor);
        clearSession();
    }

    /**
     * servlet规范并没有规定每个http request对应独立的Thread，所以需要在事务结束清除ThreadLocal中的值
     * 清除Session<br/>
     * 【重要】在用户线程调用<br/>
     */
    public static void clearSession(){
        try{
            DubboSupport.clearSession();
            TransactionId.clear();
        }catch (Exception e){}
    }
    public abstract void doExceptionHook(Throwable e,String  rollbackFor);
    public abstract void doPreHook();
    public abstract void doPostHook() throws RpcTransactionRollbackException;
    public abstract void doAfterHook();
}
