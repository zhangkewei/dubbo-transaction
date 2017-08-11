package me.zkevin.transaction.support.notice.netty.command;

import me.zkevin.transaction.support.notice.netty.NettyClientFacade;
/**
 * Created by zkevin on 16/11/23.
 */
public class RollbackCommand extends Command {
    public RollbackCommand(){
        setType(COMMAND_ROLLBACK);
    }
    public RollbackCommand(String transactionId) {
        super(COMMAND_ROLLBACK,transactionId);
    }
    @Override
    protected CommandResult doExecute() {
        return NettyClientFacade.getInstance().sendCommand(this);
    }
}
