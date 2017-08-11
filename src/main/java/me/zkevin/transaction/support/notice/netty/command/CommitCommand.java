package me.zkevin.transaction.support.notice.netty.command;

import me.zkevin.transaction.support.notice.netty.NettyClientFacade;

/**
 * Created by zkevin on 16/11/23.
 */
public class CommitCommand extends Command{
    public CommitCommand(){
        setType(COMMAND_COMMIT);
    }
    public CommitCommand(String transactionId) {
        super(COMMAND_COMMIT,transactionId);
    }
    @Override
    protected CommandResult doExecute() {
        return NettyClientFacade.getInstance().sendCommand(this);
    }
}
