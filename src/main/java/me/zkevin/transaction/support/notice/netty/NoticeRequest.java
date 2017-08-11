package me.zkevin.transaction.support.notice.netty;

import me.zkevin.transaction.support.notice.netty.command.Command;
import io.netty.channel.Channel;

/**
 * 将事务操作请求封装到请求对象中，设置优先级，通过工作队列调用
 * Created by zkevin on 17/1/3.
 */
public class NoticeRequest {
    private Command command;
    private Channel channel;
    public NoticeRequest(Command command,Channel channel){
        this.command=command;
        this.channel=channel;
    }
    public Command getCommand() {
        return command;
    }

    public Channel getChannel() {
        return channel;
    }
}
