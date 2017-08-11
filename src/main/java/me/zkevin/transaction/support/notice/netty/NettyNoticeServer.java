package me.zkevin.transaction.support.notice.netty;

import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;
import me.zkevin.transaction.support.TransactionConfig;
import me.zkevin.transaction.support.TransactionProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import me.zkevin.transaction.support.notice.netty.command.Command;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;

/**
 * netty配置表
 * Created by zkevin on 16/11/23.
 */
public class NettyNoticeServer extends AbstractNoticeServer{
    public static final String GLOBAL_NETTY_KEY="globalNettyKey";
    private volatile ServerBootstrap bootstrap;
    private volatile Channel channel;
    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private InetSocketAddress address;

    public NettyNoticeServer(TransactionProtocol transactionService){
        super(transactionService, TransactionConfig.getInstance().executeWorkerSize2Int());
        this.address=TransactionConfig.getInstance().commitAddr2SocketAddress();
        boss = new NioEventLoopGroup();
        worker = new NioEventLoopGroup();
    }
    public void start(){
        //将参数追加到dubbo InvokerExchangeFilter
        InvokerExchangeFilter.GLOBAL_EXCHANGE_PARAMS.put(GLOBAL_NETTY_KEY, TransactionConfig.getInstance().getCommitAddr());
        createServer();
    }
    public void shutdown() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
        if (channel != null) {
            channel.closeFuture().addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("netty server closed");
                }
            });
        }
    }
    private void createServer(){
        if(null==bootstrap){
            synchronized (this){
                if(null==bootstrap) {
                    bootstrap = new ServerBootstrap();
                    bootstrap.group(boss, worker);
                    bootstrap.channel(NioServerSocketChannel.class);
                    bootstrap.option(ChannelOption.SO_BACKLOG, 128);
                    //通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
                    bootstrap.option(ChannelOption.TCP_NODELAY, true);
                    //channel handler
                    final NettyServerHandler handler=new NettyServerHandler(this);
                    //保持长连接状态
                    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
                    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new ObjectEncoder());
                            p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Command.class.getClassLoader())));
                            p.addLast(handler);
                        }
                    });
                    bind();
                }
            }
        }
    }
    private void bind(){
        ChannelFuture f = null;
        try {
            f = bootstrap.bind(address).sync();
            channel=f.channel();
            if(f.isSuccess()){
                logger.info("netty server started");
            }else{
                Assert.isTrue(!f.isSuccess(),"netty server can't start");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
