package me.zkevin.transaction.support.notice.netty.command;

import me.zkevin.transaction.support.notice.netty.NettyClientFacade;

/**
 * Created by zkevin on 16/11/23.
 */
public class PrepareCommand extends Command {
    public PrepareCommand(){
        setType(COMMAND_PREPARE);
    }
    public PrepareCommand(String transactionId) {
        super(COMMAND_PREPARE,transactionId);
    }
    @Override
    protected CommandResult doExecute() {
        return NettyClientFacade.getInstance().sendCommand(this);
    }

}
