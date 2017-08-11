package me.zkevin.transaction.support.notice.netty;

import me.zkevin.transaction.support.TransactionProtocol;
import org.apache.log4j.Logger;

import java.util.concurrent.*;

/**
 * Created by zkevin on 17/1/4.
 */
public abstract class AbstractNoticeServer {
    protected static final Logger logger=Logger.getLogger(AbstractNoticeServer.class);
    private ExecutorService executor;
    private final TransactionProtocol service;
    private  ScheduledExecutorService releaseConnectionExecutor;
    public AbstractNoticeServer(TransactionProtocol transactionService,int pollSize){
        executor= Executors.newFixedThreadPool(pollSize);
        this.service=transactionService;
        //每5s执行一次，释放掉一分钟内没有提交的资源
        releaseConnectionExecutor=Executors.newSingleThreadScheduledExecutor();
        releaseConnectionExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                //logger.debug("[NoticeServer]释放占用超过60s的资源");
                service.release(60);
            }
        },5,5,TimeUnit.SECONDS);
    }
    public void submitRequest(NoticeRequest request){
        executor.submit(new NoticeWorker(request,service));
    }
}
