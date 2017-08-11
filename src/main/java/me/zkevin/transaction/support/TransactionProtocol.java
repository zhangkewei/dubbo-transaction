package me.zkevin.transaction.support;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
import me.zkevin.transaction.support.notice.netty.command.Command;
import me.zkevin.transaction.support.notice.netty.command.CommandResult;

/**
 * 事务提交协议
 */
public interface TransactionProtocol{
    /**
     * 级联提交关联事务，然后提交本地事务
     * @param command
     * @return
     * @throws RpcTransactionRollbackException
     */
    public CommandResult commit(Command command) throws RpcTransactionRollbackException;

    /**
     * 级联查询事务是否可提交，然后查询本地事务是否可提交
     * @param command
     * @return
     */
    public CommandResult prepare(Command command);

    /**
     * 级联回滚关联事务，然后回滚本地事务
     * @param command
     * @return
     */
    public CommandResult rollback(Command command);
    public CommandResult execute(Command command) throws RpcTransactionRollbackException;

    /**
     * 释放长时间占用的资源
     * @param seconds
     */
    public void release(long seconds);

}
