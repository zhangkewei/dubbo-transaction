package me.zkevin.transaction.support.notice.netty;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
import me.zkevin.transaction.support.SessionHook;
import me.zkevin.transaction.support.TransactionProtocol;
import me.zkevin.transaction.support.notice.netty.command.CommandResult;
import me.zkevin.transaction.support.notice.netty.command.CommitCommand;
import me.zkevin.transaction.support.notice.netty.command.PrepareCommand;
import me.zkevin.transaction.support.notice.netty.command.RollbackCommand;
import org.apache.log4j.Logger;


/**
 * Created by zkevin on 17/1/4.
 */
public class NoticeWorker implements Runnable {
    private NoticeRequest request;
    private TransactionProtocol service;
    private static final Logger logger=Logger.getLogger(NoticeWorker.class);
    public NoticeWorker(NoticeRequest request,TransactionProtocol service){
        this.service=service;
        this.request=request;
    }
    public void run() {
        //清除当前线程绑定数据
        SessionHook.clearSession();
        //开始处理正常业务逻辑
        CommandResult result=null;
        try {
            if (request.getCommand() instanceof PrepareCommand) {
                result = service.prepare(request.getCommand());
            } else if (request.getCommand() instanceof CommitCommand) {
                result = service.commit(request.getCommand());
            } else if (request.getCommand() instanceof RollbackCommand) {
                result = service.rollback(request.getCommand());
            }
        } catch (RpcTransactionRollbackException e) {
            e.printStackTrace();
            result=CommandResult.error(e);
        }finally {
            result.setCommand(request.getCommand());
            logger.debug("notice client response:" + result.toJSONString());
            request.getChannel().writeAndFlush(result);
        }
    }
}
