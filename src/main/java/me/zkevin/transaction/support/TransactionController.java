package me.zkevin.transaction.support;

import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;
import com.alibaba.fastjson.JSONObject;
import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
import me.zkevin.transaction.support.notice.netty.NettyNoticeServer;
import me.zkevin.transaction.support.notice.netty.command.*;
import org.apache.log4j.Logger;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.*;

/**
 * 事务链控制器
 */
public class TransactionController{
    private static TransactionProtocol protocol;
    private static  final Logger logger=Logger.getLogger(TransactionController.class);
    /**
     *提交事务链<br/>
     * 【重要】在用户线程调用<br/>
     * @return
     */
    protected static boolean commit() throws RpcTransactionRollbackException {
        CommitCommand command=new CommitCommand();
        return commit(fillIp(command));
    }

    /**
     * 回滚事务链<br/>
     * 【重要】在用户线程调用<br/>
     * @return
     */
    protected static boolean rollback(){
        RollbackCommand command=new RollbackCommand();
        return rollback(fillIp(command));
    }

    protected static boolean commit(Command command) throws RpcTransactionRollbackException {
        if (command instanceof CommitCommand) {
            if(prepare(command)){
                List<CommandResult> results = execute(command);
                for (CommandResult rs : results) {
                    logger.debug("[commit]" + rs.getCommand().toJSONString() + ":" + rs.isSuccess() + "&" + rs.getMsg());
                    retry(rs);
                }
                return true;
            } else {//提交失败，回滚事务
                rollback(command.convert2(Command.COMMAND_ROLLBACK));
                throw new RpcTransactionRollbackException("提交失败，回滚事务释放数据库链接");
            }
        }
        return false;
    }
    protected static boolean rollback(Command command) {
        if(command instanceof  RollbackCommand) {
            List<CommandResult> results = execute(command);
            for (CommandResult rs : results) {
                logger.debug("[rollback]" + rs.getCommand().toJSONString() + ":" + rs.isSuccess() + "&" + rs.getMsg());
                retry(rs);
            }
            return true;
        }
        return false;
    }
    protected static boolean prepare(Command command) {
        Command prepare=command.convert2(Command.COMMAND_PREPARE);
        List<CommandResult> results = execute(prepare);
        for (CommandResult rs : results) {
            logger.debug("[prepare]" + rs.getCommand().toJSONString() + ":" + rs.isSuccess() + "&" + rs.getMsg());
            retry(rs);
            if(!rs.isSuccess())return false;
        }
        return true;
    }
    private static CommandResult retry(CommandResult result){
        if(null!=result&&!result.isSuccess()){
            for(int i=1;!result.isSuccess()&&result.isRetry()&&i<=TransactionConfig.getInstance().getRetries();i++){
                result = result.getCommand().execute().setCommand(result.getCommand());
                logger.debug("[command][retry][" + i + "]" + result.getCommand().toJSONString() + ":" + result.isSuccess() + "&" + result.getMsg());
            }
        }
        return  result;
    }
    private static List<CommandResult> execute(Command command){
        List<CommandResult> results=new ArrayList<CommandResult>();
        List<InvokerExchangeFilter.ExchangeObject> chain=DubboSupport.listInvokeChain(command.getTransactionId());
        logger.debug("consumer invoke chain:" + com.alibaba.fastjson.JSONObject.toJSONString(chain));
        if(null!=chain&&chain.size()>0){
            for (InvokerExchangeFilter.ExchangeObject eo : chain) {
                Command nextCommand = null;
                try {
                    if (command instanceof CommitCommand) {
                        nextCommand = new CommitCommand(eo.getExchangeId());
                    } else if (command instanceof PrepareCommand) {
                        nextCommand = new PrepareCommand(eo.getExchangeId());
                    } else if (command instanceof RollbackCommand) {
                        nextCommand = new RollbackCommand(eo.getExchangeId());
                    }
                    nextCommand.setIsInvoker(eo.isInvoker());
                    //回滚
                    if(null!= eo.getException()&&(eo.getException().getMessage().contains("UnexpectedRollbackException")||eo.getException().getMessage().contains("RpcTransactionRollbackException")||eo.getException() instanceof UnexpectedRollbackException||eo.getException() instanceof RpcTransactionRollbackException)){
                        if(nextCommand instanceof PrepareCommand){
                            logger.debug("[" + eo.getExchangeId() + "] need rollback,prepare false!");
                            results.add(CommandResult.error(eo.getException()).setCommand(nextCommand));
                            continue;
                        }
                    }
                    if (eo.isInvoker()) {
                        CommandResult result = CommandResult.success().setCommand(nextCommand);
                        if (null != protocol) result.copy(protocol.execute(nextCommand));
                        results.add(result);
                    } else {
                        String noticeInfo = eo.getExtend().containsKey(NettyNoticeServer.GLOBAL_NETTY_KEY) ? String.valueOf(eo.getExtend().get(NettyNoticeServer.GLOBAL_NETTY_KEY)) : "";
                        String[] ip = noticeInfo.split(":");
                        if (null != noticeInfo && !noticeInfo.isEmpty() && ip.length == 2) {
                            CommandResult result =nextCommand.beforeExecute(ip[0], ip[1]).execute().setCommand(nextCommand);;
                            results.add(result);
                        } else {
                            //netty notice server不存在时说明当前节点没有数据库事务，无需提交
                            results.add(CommandResult.success().setCommand(nextCommand));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(null != nextCommand ? JSONObject.toJSONString(nextCommand) : JSONObject.toJSONString(eo), e);
                    results.add(CommandResult.error(e).setCommand(nextCommand));
                }
            }
        }else{
            results.add(CommandResult.success().setCommand(command));
        }
        //两阶段提交协议中，提交、回滚阶段才算是最终阶段，才会释放内存对象
        if(command instanceof CommitCommand ||command instanceof RollbackCommand){
            logger.debug("[clear]" + command.getTransactionId());
            DubboSupport.removeInvokeChain(command.getTransactionId());
        }
        return  results;
    }

    /**
     * 从LocalThread获取线程相关信息<br/>
     * 【重要】在用户线程调用<br/>
     * @param command
     * @return
     */
    private static Command fillIp(Command command){
        InvokerExchangeFilter.ExchangeObject eo=DubboSupport.getCurrentInvoke();
        command.setTransactionId(eo.getExchangeId());
        String noticeInfo=eo.getExtend().containsKey(NettyNoticeServer.GLOBAL_NETTY_KEY)?String.valueOf(eo.getExtend().get(NettyNoticeServer.GLOBAL_NETTY_KEY)):"";
        String[] ip=noticeInfo.split(":");
        if(ip.length==2)command.beforeExecute(ip[0],ip[1]);
        return command;
    }


    protected static void start(TransactionProtocol inProtocol) {
        synchronized(TransactionController.class){
            if (null == protocol)protocol = inProtocol;
        }
    }
}
