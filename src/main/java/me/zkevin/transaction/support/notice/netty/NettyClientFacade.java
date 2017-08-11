package me.zkevin.transaction.support.notice.netty;

import me.zkevin.transaction.support.TransactionConfig;
import me.zkevin.transaction.support.notice.netty.command.Command;
import me.zkevin.transaction.support.notice.netty.command.CommandResult;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.log4j.Logger;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * netty配置表
 */
public class NettyClientFacade {
    private static final Logger logger=Logger.getLogger(NettyClientFacade.class);
    private final  int CHANNEL_POOL_SIZE;
    private final  int BOOTSTRAP_POOL_SIZE;
    private static final Map<String,CommandResult> NETTY_RESULT=new ConcurrentHashMap<String,CommandResult>();
    private Map<String,List<Bootstrap>> NETTY_BOOTSTRAP=new ConcurrentHashMap<String,List<Bootstrap>>();
    private Map<String,Semaphore> NETTY_BOOTSTRAP_COUNT=new ConcurrentHashMap<String,Semaphore>();
    private Map<String,Semaphore> NETTY_CHANNEL_COUNT=new ConcurrentHashMap<String,Semaphore>();
    private Map<String,ArrayBlockingQueue<Channel>> NETTY_CHANNEL=new ConcurrentHashMap<String,ArrayBlockingQueue<Channel>>();
    private NettyClientFacade(){
        BOOTSTRAP_POOL_SIZE= TransactionConfig.getInstance().bootstrapSize2Int();
        CHANNEL_POOL_SIZE=TransactionConfig.getInstance().channelSize2Int();
    }
    public static NettyClientFacade getInstance(){
        NettyClientFacade facade=NettyFacadeHelper.NETTY;
        return facade;
    }

    private static  class NettyFacadeHelper{
        private static final NettyClientFacade NETTY=new NettyClientFacade();
    }
    private Bootstrap getBootstrap(String ip,String port) throws InterruptedException {
        String channelKey=getUniqKey(ip, port);
        List<Bootstrap> ab=putIfAbsent(NETTY_BOOTSTRAP,channelKey, new CopyOnWriteArrayList<Bootstrap>());
        ab=null==ab?NETTY_BOOTSTRAP.get(channelKey):ab;
        Semaphore sp=putIfAbsent(NETTY_BOOTSTRAP_COUNT, channelKey, new Semaphore(BOOTSTRAP_POOL_SIZE));
        sp=null==sp?NETTY_BOOTSTRAP_COUNT.get(channelKey):sp;
        while(sp.availablePermits()>0&&sp.tryAcquire()){
            Bootstrap bootstrap=null;
            EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_BACKLOG, 128);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.group(eventLoopGroup);
            bootstrap.remoteAddress(ip, Integer.parseInt(port));
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new ObjectEncoder());
                    socketChannel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Command.class.getClassLoader())));
                    socketChannel.pipeline().addLast(new NettyClientHandler());
                }
            });
            putIfAbsent(NETTY_CHANNEL_COUNT,channelKey + "_" + bootstrap.hashCode(), new Semaphore(CHANNEL_POOL_SIZE));
            ab.add(bootstrap);
        }
        for(Bootstrap b:ab){
            Semaphore spOfChannel=NETTY_CHANNEL_COUNT.get(channelKey + "_" + b.hashCode());
            if(spOfChannel.availablePermits()>0&&spOfChannel.tryAcquire())return b;
        }
        return null;
    }

    /**
     * 获取channel
     * @param ip
     * @param port
     * @return
     * @throws InterruptedException
     */
    private Channel getChannel(String ip,String port) throws InterruptedException {
        String channelKey=getUniqKey(ip, port);
        ArrayBlockingQueue<Channel> aq=putIfAbsent(NETTY_CHANNEL,channelKey, new ArrayBlockingQueue<Channel>(BOOTSTRAP_POOL_SIZE*CHANNEL_POOL_SIZE));
        aq=null==aq?NETTY_CHANNEL.get(channelKey):aq;
        Channel channel=aq.poll(100, TimeUnit.MILLISECONDS);
        //判断是否已经关闭的channel，如果是，不再放入连接池,重新申请连接
        if(null!=channel&&(!channel.isActive()||!channel.isOpen()||!channel.isWritable())){
            channel.disconnect();
            channel=null;
        }
        Bootstrap bootstrap=null==channel?getBootstrap(ip, port):null;
        if(null!=bootstrap){
            ChannelFuture f =bootstrap.connect(ip,Integer.parseInt(port)).sync();
            if (f.isSuccess())channel=f.channel();
        }

        return null!=channel?channel:aq.take();
    }

    /**
     * 发送请求
     * @param command
     * @return
     */
    public CommandResult sendCommand(final Command command){
        Channel channel=null;
        try {
            channel=getChannel(command.getIp(),command.getPort());
            ChannelFuture future=channel.writeAndFlush(command);
            final  CommandResult result=CommandResult.async(future).setCommand(command);
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(null!=channelFuture.cause()){
                        logger.error(command.getTransactionId() + " need retry:【error】" + channelFuture.cause().getMessage());
                        if(channelFuture.cause() instanceof ClosedChannelException){
                            NETTY_RESULT.put(command.getUniqKey(),CommandResult.result(CommandResult.CODE_RETRY,channelFuture.cause().getMessage()));
                        }else {
                            NETTY_RESULT.put(command.getUniqKey(), CommandResult.error(channelFuture.cause()));
                        }
                    }
                    bindResultImmediately(result);
                }
            });
            return result;
        } catch (Exception e1) {
            e1.printStackTrace();
            return CommandResult.error(e1).setCommand(command);
        }finally {
            releaseChannel(command.getIp(),command.getPort(),channel);
        }
    }

    /**
     * 释放channel
     * @param ip
     * @param port
     * @param channel
     */
    private void releaseChannel(String ip,String port,Channel channel) {
        String channelKey=getUniqKey(ip, port);
        if(null!=channel) {
            ArrayBlockingQueue<Channel> aq = NETTY_CHANNEL.containsKey(channelKey) ? NETTY_CHANNEL.get(channelKey) : null;
            if(null!=aq)aq.add(channel);
        }
    }

    public CommandResult bindResult(CommandResult result){
        if(null!=result.getFuture()&&!result.getFuture().isDone()){
            result.getFuture().awaitUninterruptibly();
        }
        //等待获取返回结果循环次数
        int loopCount=TransactionConfig.getInstance().getCallModel().equals(TransactionConfig.CALL_SYNC)?
                1*60*1000/TransactionConfig.getInstance().getCallTimeoutPerLoop():TransactionConfig.getInstance().getCallTimeoutLoops();
        //重试N次，每次线程睡眠N毫秒
        for (int i=0;i<loopCount && result.isHandling(); i++) {
            //logger.debug("wait for result:-----------"+i);
            try {
                Thread.sleep(TransactionConfig.getInstance().getCallTimeoutPerLoop());
            } catch (InterruptedException e) {
            }
            bindResultImmediately(result);
        }
        //如果还没有获得返回结果,快速失效,尝试重试
        if(result.isHandling())result.copy(CommandResult.result(CommandResult.CODE_RETRY,"事务提交请求未正确返回"));
        if(NETTY_RESULT.containsKey(result.getCommand().getUniqKey()))NETTY_RESULT.remove(result.getCommand().getUniqKey());
        return result;
    }
    private boolean bindResultImmediately(CommandResult result){
        if(!result.isHandling())return  true;
        if(NETTY_RESULT.containsKey(result.getCommand().getUniqKey())) {
            CommandResult mapResult=NETTY_RESULT.get(result.getCommand().getUniqKey());
            result.copy(mapResult);
            return true;
        }
        return false;
    }
    private class NettyClientHandler extends SimpleChannelInboundHandler<CommandResult> {
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, CommandResult result) throws Exception {
            logger.debug("notice server response:" + result.toJSONString());
            NETTY_RESULT.put(result.getCommand().getUniqKey(), result);
        }
    }
    private String getUniqKey(String ip,String port){
        return ip+port;
    }

    /**
     * 模拟jdk8 Map.putIfAbsent
     * @param pool
     * @param key
     * @param newValue
     * @param <T>
     * @return
     */
    private <T> T putIfAbsent(Map<String,T> pool,String key,T newValue){
        T oldValue=pool.get(key);
        if(null==oldValue){
            synchronized (pool) {
                oldValue=pool.get(key);
                if(null==oldValue)pool.put(key, newValue);
            }
        }
        return oldValue;
    }
}
