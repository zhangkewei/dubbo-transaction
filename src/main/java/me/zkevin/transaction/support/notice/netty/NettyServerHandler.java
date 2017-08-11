package me.zkevin.transaction.support.notice.netty;

import io.netty.channel.ChannelHandler.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.zkevin.transaction.support.notice.netty.command.Command;
import org.apache.log4j.Logger;

/**
 * netty channel handler
 */
@Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Command> {
    private static final Logger logger=Logger.getLogger(NettyServerHandler.class);
    private final AbstractNoticeServer server;
    public NettyServerHandler(AbstractNoticeServer server){
        this.server=server;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext context, Command command) throws Exception {
        logger.debug("notice client request:" + command.toJSONString());
        server.submitRequest(new NoticeRequest(command, context.channel()));
    }
}