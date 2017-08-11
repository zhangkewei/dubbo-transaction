package me.zkevin.transaction.support;

import com.alibaba.dubbo.rpc.proxy.AbstractProvidedListener;
import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;
import me.zkevin.transaction.context.TransactionContextFactory;
import me.zkevin.transaction.support.notice.netty.NettyNoticeServer;
import org.apache.log4j.Logger;

/**
 * transaction launcher
 * Created by zkevin on 16/12/14.
 */
public class TransactionStartup {
    private static final Logger logger=Logger.getLogger(TransactionStartup.class);
    /**
        //如果配置成自动提交事务，添加消费者调用结束回调
        String commitOn=TransactionConfig.getInstance().getCommitOn();
        if(null!=commitOn&&!commitOn.trim().isEmpty()&&commitOn.equals(TransactionConfig.COMMIT_AUTO)) {
            logger.info("consumedListeners added:transaction auto commit.");
            InvokerExchangeFilter.consumedListeners.add(new AbstractConsumedListener() {
                @Override
                public void listen(InvokerExchangeFilter.ExchangeObject eo) {
                    new DefaultTransctionHook().postHook();
                }
            });
        }
     **/
    public  static void start(){
        //0.添加监听
        DubboSupport.addProvidedListener(new AbstractProvidedListener() {
            @Override
            public void listen(InvokerExchangeFilter.ExchangeObject eo) {
                SessionHook.clearSession();
            }
        });
        //1.启动事务容器
        TransactionContextFactory factory=TransactionContextFactory.getInstance().start();
        logger.info("TransactionContextFactory started");
        //2.选择本地事务提交协议
        TransactionProtocol protocol=new TransactionProtocolImpl(factory);
        //3.TransactionController
        TransactionController.start(protocol);
        //4.启动事务提交server
        final NettyNoticeServer server=new NettyNoticeServer(protocol);
        server.start();
        logger.info("NettyNoticeServer started");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
               server.shutdown();
            }
        }));
    }
}
